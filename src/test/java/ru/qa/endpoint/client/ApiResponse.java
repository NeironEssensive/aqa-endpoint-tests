package ru.qa.endpoint.client;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

public record ApiResponse(int statusCode, String contentType, String result, String message, String body) {

    static ApiResponse from(Response response) {
        String body = response.asString();
        JsonPath json = parse(body);
        return new ApiResponse(
                response.statusCode(),
                response.contentType(),
                json == null ? null : json.getString("result"),
                json == null ? null : json.getString("message"),
                body);
    }

    private static JsonPath parse(String body) {
        try {
            JsonPath json = JsonPath.from(body);
            json.get();
            return json;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
