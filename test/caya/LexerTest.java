package caya;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class LexerTest {
  @ParameterizedTest
  @MethodSource("code")
  void test_lexer(String code, String expected) {
    assertEquals(expected, String.join(" ", Parser.tokenize(code)));
  }

  private static Stream<Arguments> code() {
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
        arguments(".((])[,", ". ( ( ] ) [ ,")
      );
  }
}
