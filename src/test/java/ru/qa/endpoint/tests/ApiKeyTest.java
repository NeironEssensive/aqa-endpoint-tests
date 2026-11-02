package ru.qa.endpoint.tests;

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
import ru.qa.endpoint.client.UserAction;
import ru.qa.endpoint.config.TestConfig;
import ru.qa.endpoint.data.Tokens;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.qa.endpoint.checks.ResponseChecks.assertRejected;
import static ru.qa.endpoint.checks.ResponseChecks.assertSuccess;

@Epic("Сервис /endpoint")
@Feature("Доступ по API-ключу")
@DisplayName("Доступ по API-ключу")
class ApiKeyTest extends BaseTest {

    @Test
    @Story("Запрос без API-ключа")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("LOGIN без API-ключа отклоняется, вход не выполняется")
    void loginWithoutApiKeyIsRejected() {
        String token = Tokens.random();

        assertRejected(endpoint.sendWithoutApiKey(token, UserAction.LOGIN), 401);
        externalService.verifyNoRequests();
        verifyTokenIsNotActive(token);
    }

    @Test
    @Story("Запрос без API-ключа")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("ACTION без API-ключа отклоняется и не передаётся во внешний сервис")
    void actionWithoutApiKeyIsRejected() {
        String token = loggedInUser();

        assertRejected(endpoint.sendWithoutApiKey(token, UserAction.ACTION), 401);
        externalService.verifyNoRequests();
    }

    @Test
    @Story("Запрос без API-ключа")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("LOGOUT без API-ключа отклоняется, сессия пользователя остаётся активной")
    void logoutWithoutApiKeyIsRejected() {
        String token = loggedInUser();

        assertRejected(endpoint.sendWithoutApiKey(token, UserAction.LOGOUT), 401);
        assertSuccess(endpoint.action(token));
    }

    static Stream<Arguments> invalidApiKeys() {
        String key = TestConfig.API_KEY;
        return Stream.of(
                arguments("посторонний ключ", "wrongKey123"),
                arguments("правильный ключ в верхнем регистре", key.toUpperCase()),
                arguments("ключ без последнего символа", key.substring(0, key.length() - 1)),
                arguments("ключ с лишним символом в конце", key + "1"),
                arguments("пустой ключ", ""));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidApiKeys")
    @DisplayName("Запрос с неверным API-ключом отклоняется:")
    @Story("Неверный API-ключ")
    @Severity(SeverityLevel.CRITICAL)
    void requestWithInvalidApiKeyIsRejected(String description, String apiKey) {
        String token = Tokens.random();

        assertRejected(endpoint.sendWithApiKey(token, UserAction.LOGIN, apiKey), 401);
        externalService.verifyNoRequests();
    }
}
