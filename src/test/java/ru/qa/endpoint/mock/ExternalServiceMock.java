package ru.qa.endpoint.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

public class ExternalServiceMock {

    public static final String AUTH = "/auth";
    public static final String DO_ACTION = "/doAction";

    private final WireMockServer server;

    public ExternalServiceMock(int port) {
        server = new WireMockServer(options().port(port).http2PlainDisabled(true));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop();
    }

    public void reset() {
        server.resetAll();
        stub(AUTH, 200, 0);
        stub(DO_ACTION, 200, 0);
    }

    public void clearRequestLog() {
        server.resetRequests();
    }

    @Step("Внешний сервис отвечает на {path} кодом {status}")
    public void respondWith(String path, int status) {
        stub(path, status, 0);
    }

    @Step("Внешний сервис отвечает на {path} с задержкой {delayMillis} мс")
    public void respondWithDelay(String path, int delayMillis) {
        stub(path, 200, delayMillis);
    }

    @Step("Внешний сервис обрывает соединение при запросе на {path}")
    public void dropConnection(String path) {
        server.stubFor(post(urlPathEqualTo(path))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    @Step("Проверить число запросов во внешний сервис на {path} с токеном {token}: ожидается {expected}")
    public void verifyRequestCount(String path, String token, int expected) {
        int actual = requests(path, token).size();
        assertThat(actual)
                .withFailMessage("Ожидалось запросов на %s с токеном %s: %d, получено: %d", path, token, expected, actual)
                .isEqualTo(expected);
    }

    @Step("Проверить, что приложение не обращалось к внешнему сервису")
    public void verifyNoRequests() {
        List<String> received = server.getAllServeEvents().stream()
                .map(event -> event.getRequest().getMethod() + " " + event.getRequest().getUrl()
                        + " " + event.getRequest().getBodyAsString())
                .collect(Collectors.toList());
        assertThat(received)
                .withFailMessage("Приложение не должно было обращаться к внешнему сервису, но отправило запросы: %s",
                        received)
                .isEmpty();
    }

    @Step("Проверить формат запроса на {path}: метод, заголовки и тело")
    public void verifyRequestFormat(String path, String token) {
        verifyRequestCount(path, token, 1);
        LoggedRequest request = requests(path, token).get(0);
        Allure.addAttachment("Запрос, полученный внешним сервисом", "text/plain", describe(request));

        assertThat(request.getMethod().getName())
                .withFailMessage("Ожидался метод POST, получен %s", request.getMethod())
                .isEqualTo("POST");
        assertThat(request.getHeader("Content-Type"))
                .withFailMessage("Ожидался заголовок Content-Type: application/x-www-form-urlencoded, получен «%s»",
                        request.getHeader("Content-Type"))
                .startsWith("application/x-www-form-urlencoded");
        assertThat(request.getHeader("Accept"))
                .withFailMessage("Ожидался заголовок Accept: application/json, получен «%s»", request.getHeader("Accept"))
                .isEqualTo("application/json");
        assertThat(request.getBodyAsString())
                .withFailMessage("Ожидалось тело запроса «token=%s», получено «%s»", token, request.getBodyAsString())
                .isEqualTo("token=" + token);
    }

    private void stub(String path, int status, int delayMillis) {
        server.stubFor(post(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")
                        .withFixedDelay(delayMillis)));
    }

    private List<LoggedRequest> requests(String path, String token) {
        return server.findAll(postRequestedFor(urlPathEqualTo(path)).withFormParam("token", equalTo(token)));
    }

    private static String describe(LoggedRequest request) {
        String headers = request.getHeaders().all().stream()
                .map(header -> header.key() + ": " + String.join(", ", header.values()))
                .collect(Collectors.joining("\n"));
        return request.getMethod() + " " + request.getUrl() + "\n" + headers + "\n\n" + request.getBodyAsString();
    }
}
