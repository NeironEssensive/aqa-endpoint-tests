package ru.qa.endpoint.env;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestEnvironmentExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        context.getRoot()
                .getStore(ExtensionContext.Namespace.GLOBAL)
                .getOrComputeIfAbsent(TestEnvironment.class, key -> TestEnvironment.start(), TestEnvironment.class);
    }
}
