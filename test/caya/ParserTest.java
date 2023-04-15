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
        arguments("x", "Ident[x]"),
        arguments("false", "Bool[false]"),
        arguments("1.x", "Field[Int[1], x]"),
        arguments("(1)", "Int[1]"),
        arguments("f()", "Call[Ident[f], []]"),
        arguments("f(1)", "Call[Ident[f], [Int[1]]]"),
        arguments("[1, 2, 3]", "Array[[Int[1], Int[2], Int[3]]]"),
        arguments("a.push(1, 8)", "Call[Field[Ident[a], push], [Int[1], Int[8]]]"),
        arguments("_", "Ident[_]"),
        arguments("x._", "Field[Ident[x], _]"),
        arguments("x[0]", "Item[Ident[x], [Int[0]]]")
      );
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "(",
      "{}",
  })
  void test_error(String code) {
    assertThrows(RuntimeException.class, () -> ParserHelper.parse(code));
  }
}
