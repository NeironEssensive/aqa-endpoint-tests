package ru.qa.endpoint.checks;

import io.qameta.allure.Step;
import ru.qa.endpoint.client.ApiResponse;

import static org.assertj.core.api.Assertions.assertThat;

public final class ResponseChecks {

    private ResponseChecks() {
    }

    @Step("Проверить, что запрос выполнен успешно")
    public static void assertSuccess(ApiResponse response) {
        assertThat(response.statusCode())
                .withFailMessage("Ожидался код ответа 200, получен %d. Ответ сервиса: %s",
                        response.statusCode(), response.body())
                .isEqualTo(200);
        assertJson(response);
        assertThat(response.result())
                .withFailMessage("Ожидалось значение result = OK, получено «%s»", response.result())
                .isEqualTo("OK");
        assertThat(response.message())
                .withFailMessage("В успешном ответе не должно быть поля message, получено «%s»", response.message())
                .isNull();
    }

    @Step("Проверить, что запрос отклонён с кодом {expectedStatus}")
    public static void assertRejected(ApiResponse response, int expectedStatus) {
        assertThat(response.statusCode())
                .withFailMessage("Ожидался код ответа %d, получен %d. Ответ сервиса: %s",
                        expectedStatus, response.statusCode(), response.body())
                .isEqualTo(expectedStatus);
        assertErrorBody(response);
    }

    @Step("Проверить, что запрос не выполнен")
    public static void assertFailed(ApiResponse response) {
        assertThat(response.statusCode())
                .withFailMessage("Ожидался код ошибки (400 и выше), получен %d. Ответ сервиса: %s",
                        response.statusCode(), response.body())
                .isGreaterThanOrEqualTo(400);
        assertErrorBody(response);
    }

    @Step("Проверить, что сервис ответил ошибкой клиента (код 4xx), а не внутренней ошибкой")
    public static void assertClientError(ApiResponse response) {
        assertThat(response.statusCode())
                .withFailMessage("Ожидался код ошибки клиента (от 400 до 499), получен %d. Ответ сервиса: %s",
                        response.statusCode(), response.body())
                .isBetween(400, 499);
        assertErrorBody(response);
    }

    private static void assertErrorBody(ApiResponse response) {
        assertJson(response);
        assertThat(response.result())
                .withFailMessage("Ожидалось значение result = ERROR, получено «%s»", response.result())
                .isEqualTo("ERROR");
        assertThat(response.message())
                .withFailMessage("В ответе с ошибкой должна быть указана причина в поле message")
                .isNotBlank();
    }

    private static void assertJson(ApiResponse response) {
        assertThat(response.contentType())
                .withFailMessage("Ожидался ответ в формате JSON, получен «%s»", response.contentType())
                .startsWith("application/json");
    }
}
