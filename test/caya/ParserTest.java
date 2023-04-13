package caya;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ParserTest {
  @ParameterizedTest
  @MethodSource("ok")
  void test_ok(String code, String expected) {
    assertEquals(expected, Node.show(Parser.parse(code)));
  }

  private static Stream<Arguments> ok() {
    return Stream.of(
        arguments("false", "Bool[false]"),
        arguments("1.x", "Field[Int[1], x]")
      );
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "(",
      "a",
  })
  void test_error(String code) {
    assertThrows(RuntimeException.class, () -> ParserHelper.parse(code));
  }
}
