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
@Feature("Действие пользователя (ACTION)")
@DisplayName("Действие пользователя (ACTION)")
class ActionTest extends BaseTest {

    @Test
    @Story("Действие после входа")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Действие после входа выполняется и передаётся во внешний сервис")
    void actionAfterLoginSucceeds() {
        String token = loggedInUser();

        assertSuccess(endpoint.action(token));
        externalService.verifyRequestCount(DO_ACTION, token, 1);
        externalService.verifyRequestCount(AUTH, token, 0);
    }

    @Test
    @Story("Действие после входа")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Запрос во внешний сервис /doAction соответствует документации")
    void doActionRequestMatchesSpecification() {
        String token = loggedInUser();

        endpoint.action(token);
        externalService.verifyRequestFormat(DO_ACTION, token);
    }

    @Test
    @Story("Действие после входа")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Несколько действий подряд в одной сессии выполняются")
    void severalActionsInOneSessionSucceed() {
        String token = loggedInUser();

        assertSuccess(endpoint.action(token));
        assertSuccess(endpoint.action(token));
        assertSuccess(endpoint.action(token));
        externalService.verifyRequestCount(DO_ACTION, token, 3);
    }

    @Test
    @Story("Действие без входа")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Действие без предварительного входа отклоняется")
    void actionWithoutLoginIsRejected() {
        String token = Tokens.random();

        assertRejected(endpoint.action(token), 403);
        externalService.verifyNoRequests();
    }

    @Test
    @Story("Действие без входа")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Действие с токеном, который не входил, отклоняется, даже если вошёл другой пользователь")
    void actionWithAnotherTokenIsRejected() {
        loggedInUser();
        String anotherToken = Tokens.random();

        assertRejected(endpoint.action(anotherToken), 403);
        externalService.verifyNoRequests();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = {201, 202, 204})
    @DisplayName("Действие выполняется, если внешний сервис ответил успешным кодом")
    @Story("Ответы внешнего сервиса")
    @Severity(SeverityLevel.NORMAL)
    void actionSucceedsOnAnySuccessfulStatus(int status) {
        String token = loggedInUser();
        externalService.respondWith(DO_ACTION, status);

        assertSuccess(endpoint.action(token));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(ints = {400, 401, 403, 404, 500, 502, 503})
    @DisplayName("Действие не выполняется, если внешний сервис ответил ошибкой")
    @Story("Ответы внешнего сервиса")
    @Severity(SeverityLevel.CRITICAL)
    void actionFailsWhenExternalServiceReturnsError(int status) {
        String token = loggedInUser();
        externalService.respondWith(DO_ACTION, status);

        assertFailed(endpoint.action(token));
        externalService.verifyRequestCount(DO_ACTION, token, 1);
    }

    @Test
    @Story("Ответы внешнего сервиса")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Действие не выполняется, если внешний сервис недоступен")
    void actionFailsWhenExternalServiceIsUnavailable() {
        String token = loggedInUser();
        externalService.dropConnection(DO_ACTION);

        assertFailed(endpoint.action(token));
    }

    @Test
    @Story("Ответы внешнего сервиса")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Ошибка внешнего сервиса не завершает сессию: следующее действие выполняется")
    void sessionSurvivesExternalServiceError() {
        String token = loggedInUser();
        externalService.respondWith(DO_ACTION, 500);
        assertFailed(endpoint.action(token));

        externalService.respondWith(DO_ACTION, 200);
        assertSuccess(endpoint.action(token));
    }
}
