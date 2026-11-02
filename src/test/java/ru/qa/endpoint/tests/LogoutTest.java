package ru.qa.endpoint.tests;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.qa.endpoint.data.Tokens;

import static ru.qa.endpoint.checks.ResponseChecks.assertRejected;
import static ru.qa.endpoint.checks.ResponseChecks.assertSuccess;
import static ru.qa.endpoint.mock.ExternalServiceMock.AUTH;
import static ru.qa.endpoint.mock.ExternalServiceMock.DO_ACTION;

@Epic("Сервис /endpoint")
@Feature("Выход пользователя (LOGOUT)")
@DisplayName("Выход пользователя (LOGOUT)")
class LogoutTest extends BaseTest {

    @Test
    @Story("Успешный выход")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Выход после входа выполняется без обращения к внешнему сервису")
    void logoutAfterLoginSucceeds() {
        String token = loggedInUser();

        assertSuccess(endpoint.logout(token));
        externalService.verifyNoRequests();
    }

    @Test
    @Story("Успешный выход")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("После выхода действие ACTION недоступно")
    void actionIsUnavailableAfterLogout() {
        String token = loggedInUser();

        assertSuccess(endpoint.logout(token));
        assertRejected(endpoint.action(token), 403);
        externalService.verifyRequestCount(DO_ACTION, token, 0);
    }

    @Test
    @Story("Успешный выход")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("После выхода можно снова войти с тем же токеном")
    void loginAgainAfterLogout() {
        String token = loggedInUser();

        assertSuccess(endpoint.logout(token));
        assertSuccess(endpoint.login(token));
        externalService.verifyRequestCount(AUTH, token, 1);
        assertSuccess(endpoint.action(token));
    }

    @Test
    @Story("Успешный выход")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Выход одного пользователя не завершает сессию другого")
    void logoutDoesNotAffectOtherUsers() {
        String first = loggedInUser();
        String second = loggedInUser();

        assertSuccess(endpoint.logout(first));
        assertSuccess(endpoint.action(second));
        assertRejected(endpoint.action(first), 403);
    }

    @Test
    @Story("Выход без активной сессии")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Выход без предварительного входа отклоняется")
    void logoutWithoutLoginIsRejected() {
        String token = Tokens.random();

        assertRejected(endpoint.logout(token), 403);
        externalService.verifyNoRequests();
    }

    @Test
    @Story("Выход без активной сессии")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Повторный выход отклоняется")
    void repeatedLogoutIsRejected() {
        String token = loggedInUser();

        assertSuccess(endpoint.logout(token));
        assertRejected(endpoint.logout(token), 403);
    }
}
