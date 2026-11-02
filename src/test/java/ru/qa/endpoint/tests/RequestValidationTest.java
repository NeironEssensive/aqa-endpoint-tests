package ru.qa.endpoint.tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.qa.endpoint.client.EndpointClient;
import ru.qa.endpoint.data.Tokens;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.qa.endpoint.checks.ResponseChecks.assertClientError;
import static ru.qa.endpoint.checks.ResponseChecks.assertRejected;

@Epic("Сервис /endpoint")
@Feature("Проверка параметров запроса")
@DisplayName("Проверка параметров запроса")
class RequestValidationTest extends BaseTest {

    private static final String FORM = "application/x-www-form-urlencoded";

    static Stream<Arguments> invalidActions() {
        return Stream.of(
                arguments("неизвестное действие", "DELETE"),
                arguments("действие строчными буквами", "login"),
                arguments("пустое значение", ""),
                arguments("два действия через запятую", "LOGIN,ACTION"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidActions")
    @DisplayName("Недопустимое значение action отклоняется:")
    @Story("Параметр action")
    @Severity(SeverityLevel.NORMAL)
    void invalidActionIsRejected(String description, String action) {
        String token = Tokens.random();

        assertRejected(endpoint.send(token, action), 400);
        externalService.verifyNoRequests();
    }

    @Test
    @Story("Параметр action")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Запрос без параметра action отклоняется")
    void requestWithoutActionIsRejected() {
        String token = Tokens.random();

        assertRejected(endpoint.send(token, null), 400);
        externalService.verifyNoRequests();
    }

    @Test
    @Story("Параметр action")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Запрос без параметров отклоняется")
    void requestWithoutParametersIsRejected() {
        assertRejected(endpoint.send(null, null), 400);
        externalService.verifyNoRequests();
    }

    static Stream<Arguments> malformedRequests() {
        String token = Tokens.random();
        String form = "token=" + token + "&action=LOGIN";
        String json = "{\"token\":\"" + token + "\",\"action\":\"LOGIN\"}";
        return Stream.of(
                arguments("метод GET вместо POST", "GET", EndpointClient.PATH, FORM, null),
                arguments("метод PUT вместо POST", "PUT", EndpointClient.PATH, FORM, form),
                arguments("тело в формате JSON вместо формы", "POST", EndpointClient.PATH, "application/json", json),
                arguments("несуществующий адрес", "POST", "/unknown", FORM, form));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedRequests")
    @DisplayName("Некорректный HTTP-запрос не вызывает внутреннюю ошибку сервиса:")
    @Story("Некорректный HTTP-запрос")
    @Severity(SeverityLevel.MINOR)
    @Description("Ошибка в запросе со стороны клиента должна возвращаться с кодом 4xx и понятной причиной. "
            + "Код 500 означает, что сервис не обработал такую ситуацию и сообщает о собственном сбое.")
    void malformedRequestReturnsClientError(String description, String method, String path,
                                            String contentType, String body) {
        assertClientError(endpoint.sendRaw(method, path, contentType, body));
        externalService.verifyNoRequests();
    }
}
