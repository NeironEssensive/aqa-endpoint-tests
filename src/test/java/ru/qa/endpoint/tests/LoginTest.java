package ru.qa.endpoint.tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.qa.endpoint.data.Tokens;

import static ru.qa.endpoint.checks.ResponseChecks.assertFailed;
import static ru.qa.endpoint.checks.ResponseChecks.assertRejected;
import static ru.qa.endpoint.checks.ResponseChecks.assertSuccess;
import static ru.qa.endpoint.mock.ExternalServiceMock.AUTH;
import static ru.qa.endpoint.mock.ExternalServiceMock.DO_ACTION;

@Epic("Сервис /endpoint")
@Feature("Вход пользователя (LOGIN)")
@DisplayName("Вход пользователя (LOGIN)")
class LoginTest extends BaseTest {

    @Test
    @Story("Успешный вход")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Успешный вход: сервис отвечает OK и проверяет токен во внешнем сервисе")
    void successfulLogin() {
        String token = Tokens.random();

        assertSuccess(endpoint.login(token));
        externalService.verifyRequestCount(AUTH, token, 1);
        externalService.verifyRequestCount(DO_ACTION, token, 0);
    }

    @Test
    @Story("Успешный вход")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Запрос во внешний сервис /auth соответствует документации")
    void authRequestMatchesSpecification() {
        String token = Tokens.random();

        endpoint.login(token);
        externalService.verifyRequestFormat(AUTH, token);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = {201, 202, 204})
    @DisplayName("Вход выполняется, если внешний сервис ответил успешным кодом")
    @Story("Ответы внешнего сервиса")
    @Severity(SeverityLevel.NORMAL)
    void loginSucceedsOnAnySuccessfulStatus(int status) {
        String token = Tokens.random();
        externalService.respondWith(AUTH, status);

        assertSuccess(endpoint.login(token));
        assertSuccess(endpoint.action(token));
    }

    @Test
    @Story("Повторный вход")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Повторный вход с тем же токеном отклоняется без обращения к внешнему сервису")
    void repeatedLoginIsRejected() {
        String token = loggedInUser();

        assertRejected(endpoint.login(token), 409);
        externalService.verifyNoRequests();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = {400, 401, 403, 404, 500, 502, 503})
    @DisplayName("Вход не выполняется, если внешний сервис ответил ошибкой")
    @Story("Ответы внешнего сервиса")
    @Severity(SeverityLevel.CRITICAL)
    void loginFailsWhenExternalServiceReturnsError(int status) {
        String token = Tokens.random();
        externalService.respondWith(AUTH, status);

        assertFailed(endpoint.login(token));
        verifyTokenIsNotActive(token);
    }

    @Test
    @Story("Ответы внешнего сервиса")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Вход не выполняется, если внешний сервис недоступен")
    void loginFailsWhenExternalServiceIsUnavailable() {
        String token = Tokens.random();
        externalService.dropConnection(AUTH);

        assertFailed(endpoint.login(token));
        verifyTokenIsNotActive(token);
    }

    @Test
    @Story("Ответы внешнего сервиса")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Вход не выполняется, если внешний сервис не ответил вовремя")
    void loginFailsWhenExternalServiceTimesOut() {
        String token = Tokens.random();
        externalService.respondWithDelay(AUTH, 15_000);

        assertFailed(endpoint.login(token));
        verifyTokenIsNotActive(token);
    }
}
