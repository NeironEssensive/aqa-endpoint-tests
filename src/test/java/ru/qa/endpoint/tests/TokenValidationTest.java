package ru.qa.endpoint.tests;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import ru.qa.endpoint.client.UserAction;
import ru.qa.endpoint.data.Tokens;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.qa.endpoint.checks.ResponseChecks.assertRejected;
import static ru.qa.endpoint.checks.ResponseChecks.assertSuccess;
import static ru.qa.endpoint.mock.ExternalServiceMock.AUTH;

@Epic("Сервис /endpoint")
@Feature("Проверка токена")
@DisplayName("Проверка токена")
class TokenValidationTest extends BaseTest {

    static Stream<Arguments> allowedTokens() {
        return Stream.of(
                arguments("только цифры", Tokens.random(Tokens.DIGITS)),
                arguments("только буквы A-F", Tokens.random(Tokens.HEX_LETTERS)),
                arguments("только буквы G-Z", Tokens.random(Tokens.NON_HEX_LETTERS)),
                arguments("буквы A-Z и цифры вперемешку", Tokens.withNonHexLetter()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allowedTokens")
    @DisplayName("Токен из допустимых символов принимается:")
    @Story("Допустимый токен")
    @Severity(SeverityLevel.CRITICAL)
    @Description("По документации токен - строка из 32 символов, где допустимы буквы A-Z и цифры 0-9. "
            + "Сервис должен принимать любой такой токен.")
    void tokenWithAllowedCharactersIsAccepted(String description, String token) {
        assertSuccess(endpoint.login(token));
        externalService.verifyRequestCount(AUTH, token, 1);
    }

    static Stream<Arguments> invalidTokens() {
        return Stream.of(
                arguments("31 символ вместо 32", Tokens.random(Tokens.DIGITS, 31)),
                arguments("33 символа вместо 32", Tokens.random(Tokens.DIGITS, 33)),
                arguments("строчные латинские буквы", Tokens.random("abcdef")),
                arguments("кириллические буквы, похожие на латинские", "АВСЕ" + Tokens.random(Tokens.DIGITS, 28)),
                arguments("спецсимволы", Tokens.random(Tokens.DIGITS, 30) + "-_"),
                arguments("пробел внутри токена", Tokens.random(Tokens.DIGITS, 16) + " " + Tokens.random(Tokens.DIGITS, 15)),
                arguments("пустая строка", ""));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTokens")
    @DisplayName("Токен неверного формата отклоняется:")
    @Story("Недопустимый токен")
    @Severity(SeverityLevel.NORMAL)
    void tokenWithInvalidFormatIsRejected(String description, String token) {
        assertRejected(endpoint.login(token), 400);
        externalService.verifyNoRequests();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(UserAction.class)
    @DisplayName("Запрос без токена отклоняется:")
    @Story("Недопустимый токен")
    @Severity(SeverityLevel.NORMAL)
    void requestWithoutTokenIsRejected(UserAction action) {
        assertRejected(endpoint.send(null, action.name()), 400);
        externalService.verifyNoRequests();
    }
}
