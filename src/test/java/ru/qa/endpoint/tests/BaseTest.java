package ru.qa.endpoint.tests;

import io.qameta.allure.Step;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.qa.endpoint.checks.ResponseChecks;
import ru.qa.endpoint.client.EndpointClient;
import ru.qa.endpoint.data.Tokens;
import ru.qa.endpoint.env.TestEnvironment;
import ru.qa.endpoint.env.TestEnvironmentExtension;
import ru.qa.endpoint.mock.ExternalServiceMock;

@ExtendWith(TestEnvironmentExtension.class)
abstract class BaseTest {

    protected final EndpointClient endpoint = new EndpointClient();
    protected ExternalServiceMock externalService;

    @BeforeEach
    void prepareExternalService() {
        externalService = TestEnvironment.externalService();
        externalService.reset();
    }

    protected String loggedInUser() {
        String token = Tokens.random();
        logIn(token);
        return token;
    }

    @Step("Подготовка: пользователь с токеном {token} выполнил вход")
    protected void logIn(String token) {
        ResponseChecks.assertSuccess(endpoint.login(token));
        externalService.clearRequestLog();
    }

    @Step("Проверить, что токен {token} не активен: действие ACTION ему недоступно")
    protected void verifyTokenIsNotActive(String token) {
        ResponseChecks.assertRejected(endpoint.action(token), 403);
        externalService.verifyRequestCount(ExternalServiceMock.DO_ACTION, token, 0);
    }
}
