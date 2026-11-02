package ru.qa.endpoint.client;

import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import ru.qa.endpoint.config.TestConfig;

import static io.restassured.config.EncoderConfig.encoderConfig;

public class EndpointClient {

    public static final String PATH = "/endpoint";
    private static final String API_KEY_HEADER = "X-Api-Key";

    private static final RestAssuredConfig CONFIG = RestAssured.config()
            .encoderConfig(encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false));

    private static final AllureRestAssured ALLURE = new AllureRestAssured()
            .setRequestAttachmentName("Запрос к сервису")
            .setResponseAttachmentName("Ответ сервиса");

    @Step("Отправить LOGIN с токеном {token}")
    public ApiResponse login(String token) {
        return post(request(true).header(API_KEY_HEADER, TestConfig.API_KEY), token, UserAction.LOGIN.name());
    }

    @Step("Отправить ACTION с токеном {token}")
    public ApiResponse action(String token) {
        return post(request(true).header(API_KEY_HEADER, TestConfig.API_KEY), token, UserAction.ACTION.name());
    }

    @Step("Отправить LOGOUT с токеном {token}")
    public ApiResponse logout(String token) {
        return post(request(true).header(API_KEY_HEADER, TestConfig.API_KEY), token, UserAction.LOGOUT.name());
    }

    @Step("Отправить запрос с параметрами token={token}, action={action}")
    public ApiResponse send(String token, String action) {
        return post(request(true).header(API_KEY_HEADER, TestConfig.API_KEY), token, action);
    }

    @Step("Отправить {action} с токеном {token} и API-ключом «{apiKey}»")
    public ApiResponse sendWithApiKey(String token, UserAction action, String apiKey) {
        return post(request(true).header(API_KEY_HEADER, apiKey), token, action.name());
    }

    @Step("Отправить {action} с токеном {token} без API-ключа")
    public ApiResponse sendWithoutApiKey(String token, UserAction action) {
        return post(request(true), token, action.name());
    }

    @Step("Отправить {method}-запрос на {path} с Content-Type «{contentType}»")
    public ApiResponse sendRaw(String method, String path, String contentType, String body) {
        RequestSpecification spec = request(true)
                .header(API_KEY_HEADER, TestConfig.API_KEY)
                .contentType(contentType);
        if (body != null) {
            spec.body(body);
        }
        return ApiResponse.from(spec.request(method, path));
    }

    public ApiResponse sendWithoutReport(String token, UserAction action) {
        return post(request(false).header(API_KEY_HEADER, TestConfig.API_KEY), token, action.name());
    }

    private RequestSpecification request(boolean report) {
        RequestSpecification spec = RestAssured.given()
                .config(CONFIG)
                .baseUri(TestConfig.APP_URL)
                .contentType(ContentType.URLENC)
                .accept(ContentType.JSON);
        return report ? spec.filter(ALLURE) : spec;
    }

    private ApiResponse post(RequestSpecification spec, String token, String action) {
        if (token != null) {
            spec.formParam("token", token);
        }
        if (action != null) {
            spec.formParam("action", action);
        }
        return ApiResponse.from(spec.post(PATH));
    }
}
