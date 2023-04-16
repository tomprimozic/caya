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
      arguments("", "Seq[[]]"),
      arguments("x", "Ident[x]"),
      arguments("x; 1", "Seq[[Ident[x], Int[1]]]"),
      arguments("false", "Bool[false]"),
      arguments("1.x", "Field[Int[1], x]"),
      arguments("(1)", "Int[1]"),
      arguments("f()", "Call[Ident[f], []]"),
      arguments("f(1)", "Call[Ident[f], [Int[1]]]"),
      arguments("[]", "Array[[]]"),
      arguments("[1]", "Array[[Int[1]]]"),
      arguments("[1,]", "Array[[Int[1]]]"),
      arguments("[9, 7,]", "Array[[Int[9], Int[7]]]"),
      arguments("[1, 2, 3]", "Array[[Int[1], Int[2], Int[3]]]"),
      arguments("a.push(1, 8)", "Call[Field[Ident[a], push], [Int[1], Int[8]]]"),
      arguments("_", "Ident[_]"),
      arguments("x._", "Field[Ident[x], _]"),
      arguments("x[0]", "Item[Ident[x], [Int[0]]]"),
      arguments("x = 1", "Assign[Ident[x], Int[1]]"),
      arguments("4; z = true; 'm'", "Seq[[Int[4], Assign[Ident[z], Bool[true]], Str[m]]]"),
      arguments("f(x) = y", "Assign[Call[Ident[f], [Ident[x]]], Ident[y]]"),
      arguments("x[0] = 1", "Assign[Item[Ident[x], [Int[0]]], Int[1]]"),
      arguments("f(x) = x + 1", "Assign[Call[Ident[f], [Ident[x]]], Binary[Ident[+], Ident[x], Int[1]]]"),
      arguments("h(x) = if x then 3 else -7", "Assign[Call[Ident[h], [Ident[x]]], If[Ident[x], Int[3], Unary[Ident[-], Int[7]]]]"),
      arguments("-1 * 2", "Unary[Ident[-], Binary[Ident[*], Int[1], Int[2]]]"),
      arguments("1 < x <= 5", "Cmp[[Int[1], Ident[<], Ident[x], Ident[<=], Int[5]]]")
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "(",
    "{}",
    "[,]",
    "==",
    "*"
  })
  void test_error(String code) {
    assertThrows(Parser.ParserError.class, () -> ParserHelper.parse(code));
  }
}
