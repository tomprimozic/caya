package caya;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class LexerTest {
  @ParameterizedTest
  @MethodSource("ok")
  void test_ok(String code, String expected) {
    assertEquals(expected, String.join(" ", Parser.tokenize(code)));
  }

  private static Stream<Arguments> ok() {
    return Stream.of(
      arguments("false", "FALSE"),
      arguments("1.x", "INTEGER(1) . IDENT(x)"),
      arguments("1.2", "INTEGER(1) . INTEGER(2)"),
      arguments("1.e2", "INTEGER(1) . IDENT(e2)"),
      arguments("non none nones xnone", "IDENT(non) NONE IDENT(nones) IDENT(xnone)"),
      arguments("1x", "INTEGER(1) IDENT(x)"),
      arguments("1e4", "INTEGER(1) IDENT(e4)"),
      arguments("x1", "IDENT(x1)"),
      arguments("0x1", "INTEGER(0) IDENT(x1)"),
      arguments(".((...=]);;..[,==", ". ( ( . . . = ] ) ; ; . . [ , = ="),
      arguments("'a' '\\t' '\\'' \"f\"", "STRING(a) STRING(\t) STRING(') STRING(f)")
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "'\\x'",
    "'",
  })
  void test_error(String code) {
    assertThrows(Parser.ParserError.class, () -> ParserHelper.parse(code));
  }
}
