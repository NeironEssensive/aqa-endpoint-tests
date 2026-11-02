package ru.qa.endpoint.config;

import java.net.URI;
import java.nio.file.Path;

public final class TestConfig {

    public static final String APP_URL = System.getProperty("app.url", "http://localhost:8080");
    public static final Path APP_JAR = Path.of(System.getProperty("app.jar", "app/internal-0.0.1-SNAPSHOT.jar"));
    public static final boolean APP_START = Boolean.parseBoolean(System.getProperty("app.start", "true"));
    public static final String API_KEY = System.getProperty("api.key", "qazWSXedc");
    public static final int MOCK_PORT = Integer.getInteger("mock.port", 8888);
    public static final Path ALLURE_RESULTS = Path.of(System.getProperty("allure.results.directory", "target/allure-results"));

    private TestConfig() {
    }

    public static URI appUri() {
        return URI.create(APP_URL);
    }

    public static String mockUrl() {
        return "http://localhost:" + MOCK_PORT + "/";
    }
}
