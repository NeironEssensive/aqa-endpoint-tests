package ru.qa.endpoint.tests;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qa.endpoint.client.ApiResponse;
import ru.qa.endpoint.client.UserAction;
import ru.qa.endpoint.data.Tokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.qa.endpoint.checks.ResponseChecks.assertRejected;
import static ru.qa.endpoint.checks.ResponseChecks.assertSuccess;
import static ru.qa.endpoint.mock.ExternalServiceMock.AUTH;
import static ru.qa.endpoint.mock.ExternalServiceMock.DO_ACTION;

@Epic("Сервис /endpoint")
@Feature("Сценарии работы пользователя")
@DisplayName("Сценарии работы пользователя")
class UserScenarioTest extends BaseTest {

    private static final int PARALLEL_REQUESTS = 10;

    @Test
    @Story("Полный цикл")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Полный цикл: вход, два действия, выход, после выхода действие недоступно")
    void fullUserSession() {
        String token = Tokens.random();

        assertSuccess(endpoint.login(token));
        assertSuccess(endpoint.action(token));
        assertSuccess(endpoint.action(token));
        assertSuccess(endpoint.logout(token));
        assertRejected(endpoint.action(token), 403);

        externalService.verifyRequestCount(AUTH, token, 1);
        externalService.verifyRequestCount(DO_ACTION, token, 2);
    }

    @Test
    @Story("Одновременные запросы")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Одновременные запросы на вход с одним токеном: успешен только один")
    void concurrentLoginsWithSameToken() {
        String token = Tokens.random();
        externalService.respondWithDelay(AUTH, 300);

        List<ApiResponse> responses = loginConcurrently(Collections.nCopies(PARALLEL_REQUESTS, token));

        verifyStatusCounts(responses, new TreeMap<>(Map.of(200, 1L, 409, (long) PARALLEL_REQUESTS - 1)));
        externalService.verifyRequestCount(AUTH, token, 1);
    }

    @Test
    @Story("Одновременные запросы")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Одновременный вход разных пользователей: все входы успешны")
    void concurrentLoginsOfDifferentUsers() {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < PARALLEL_REQUESTS; i++) {
            tokens.add(Tokens.random());
        }
        externalService.respondWithDelay(AUTH, 300);

        List<ApiResponse> responses = loginConcurrently(tokens);

        verifyStatusCounts(responses, new TreeMap<>(Map.of(200, (long) PARALLEL_REQUESTS)));
    }

    List<ApiResponse> loginConcurrently(List<String> tokens) {
        return Allure.step("Отправить одновременно " + tokens.size() + " запросов LOGIN", () -> sendLogins(tokens));
    }

    private List<ApiResponse> sendLogins(List<String> tokens) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tokens.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<ApiResponse>> futures = new ArrayList<>();
            for (String token : tokens) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return endpoint.sendWithoutReport(token, UserAction.LOGIN);
                }));
            }
            start.countDown();
            List<ApiResponse> responses = new ArrayList<>();
            for (Future<ApiResponse> future : futures) {
                responses.add(future.get(30, TimeUnit.SECONDS));
            }
            Allure.addAttachment("Полученные ответы", "text/plain", responses.stream()
                    .map(response -> response.statusCode() + " " + response.body())
                    .collect(Collectors.joining("\n")));
            return responses;
        } finally {
            pool.shutdownNow();
        }
    }

    @Step("Проверить количество ответов с каждым кодом (код=количество): ожидается {expected}")
    void verifyStatusCounts(List<ApiResponse> responses, Map<Integer, Long> expected) {
        Map<Integer, Long> actual = responses.stream()
                .collect(Collectors.groupingBy(ApiResponse::statusCode, TreeMap::new, Collectors.counting()));
        assertThat(actual)
                .withFailMessage("Ожидалось ответов по кодам %s, получено %s", expected, actual)
                .isEqualTo(expected);
    }
}
