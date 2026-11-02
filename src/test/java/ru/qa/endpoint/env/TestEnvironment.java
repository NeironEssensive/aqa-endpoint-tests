package ru.qa.endpoint.env;

import org.junit.jupiter.api.extension.ExtensionContext;
import ru.qa.endpoint.config.TestConfig;
import ru.qa.endpoint.mock.ExternalServiceMock;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class TestEnvironment implements ExtensionContext.Store.CloseableResource {

    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);
    private static final Path APP_LOG = Path.of("target", "app.log");

    private static volatile TestEnvironment current;

    private final ExternalServiceMock externalService;
    private final Process app;

    private TestEnvironment(ExternalServiceMock externalService, Process app) {
        this.externalService = externalService;
        this.app = app;
    }

    public static ExternalServiceMock externalService() {
        if (current == null) {
            throw new IllegalStateException("Тестовое окружение не запущено");
        }
        return current.externalService;
    }

    static TestEnvironment start() {
        ExternalServiceMock externalService = new ExternalServiceMock(TestConfig.MOCK_PORT);
        externalService.start();
        externalService.reset();

        Process app = null;
        try {
            if (TestConfig.APP_START) {
                app = launchApp();
            }
            waitForApp(app);
            writeAllureMetadata();
        } catch (RuntimeException e) {
            if (app != null) {
                app.destroyForcibly();
            }
            externalService.stop();
            throw e;
        }

        current = new TestEnvironment(externalService, app);
        return current;
    }

    @Override
    public void close() throws InterruptedException {
        if (app != null) {
            app.destroy();
            if (!app.waitFor(10, TimeUnit.SECONDS)) {
                app.destroyForcibly();
            }
        }
        externalService.stop();
        current = null;
    }

    private static Process launchApp() {
        URI uri = TestConfig.appUri();
        if (isPortOpen(uri.getHost(), uri.getPort())) {
            throw new IllegalStateException("Порт " + uri.getPort() + " уже занят. Остановите запущенное приложение "
                    + "или запустите тесты с -Dapp.start=false");
        }
        if (!Files.isRegularFile(TestConfig.APP_JAR)) {
            throw new IllegalStateException("Не найден jar приложения: " + TestConfig.APP_JAR.toAbsolutePath()
                    + ". Положите его в папку app или укажите путь через -Dapp.jar");
        }
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<String> command = List.of(
                java,
                "-Dsecret=" + TestConfig.API_KEY,
                "-Dmock=" + TestConfig.mockUrl(),
                "-Dserver.port=" + uri.getPort(),
                "-jar", TestConfig.APP_JAR.toAbsolutePath().toString());
        try {
            Files.createDirectories(APP_LOG.getParent());
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(APP_LOG.toFile())
                    .start();
            Runtime.getRuntime().addShutdownHook(new Thread(process::destroyForcibly));
            return process;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось запустить приложение", e);
        }
    }

    private static void waitForApp(Process app) {
        URI uri = TestConfig.appUri();
        Instant deadline = Instant.now().plus(STARTUP_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            if (app != null && !app.isAlive()) {
                throw new IllegalStateException("Приложение завершилось при запуске, подробности в "
                        + APP_LOG.toAbsolutePath());
            }
            if (isPortOpen(uri.getHost(), uri.getPort())) {
                return;
            }
            sleep(Duration.ofMillis(300));
        }
        throw new IllegalStateException("Приложение не ответило на " + TestConfig.APP_URL + " за "
                + STARTUP_TIMEOUT.toSeconds() + " секунд");
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void writeAllureMetadata() {
        try {
            Files.createDirectories(TestConfig.ALLURE_RESULTS);
            String environment = String.join(System.lineSeparator(),
                    "Application=" + TestConfig.APP_URL,
                    "External.service.mock=" + TestConfig.mockUrl(),
                    "Java=" + System.getProperty("java.version"));
            Files.writeString(TestConfig.ALLURE_RESULTS.resolve("environment.properties"), environment);
            try (InputStream categories = TestEnvironment.class.getResourceAsStream("/allure/categories.json")) {
                if (categories != null) {
                    Files.copy(categories, TestConfig.ALLURE_RESULTS.resolve("categories.json"),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось записать данные для отчёта Allure", e);
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
