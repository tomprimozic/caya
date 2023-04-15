package caya;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class InterpreterTest {
  @ParameterizedTest
  @MethodSource("ok")
  void test_ok(String code, String expected) {
    assertEquals(expected, new Interpreter().eval(Parser.parse(code)).toString());
  }

  private static Stream<Arguments> ok() {
    return Stream.of(
      arguments("false", "false"),
      arguments("1", "1"),
      arguments("-1", "-1"),
      arguments("[1, 2, 3]", "[1, 2, 3]"),
      arguments("[1, 2, 3].size", "3"),
      arguments("[].append(1)", "none"),
      arguments("'xyzz'.size", "4"),
      arguments("\"'\".join([4,6,7,])", "4'6'7"),
      arguments("", "none"),
      arguments("7; 3", "3"),
      arguments("x = 1; x", "1"),
      arguments("x = 1; x = 4; x", "4"),
      arguments("l = []; l.append(1); l.append(5); l", "[1, 5]"),
      arguments("l = []; l.append(7, 3); l", "[7, 3]"),
      arguments("l = []; (l.append(0); l).append((l.append(1); 4), (l.append(2); 5), (l.append(3); 6)); l", "[0, 1, 2, 3, 4, 5, 6]"),
      arguments("1 + -1", "0"),
      arguments("if true then 1 else 0", "1"),
      arguments("if false then 1 else 0", "0"),
      arguments("l = []; if true then l.append(1) else l.append(2); l", "[1]")
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "y",
  })
  void test_error(String code) {
    assertThrows(Interpreter.InterpreterError.class, () -> new Interpreter().eval(Parser.parse(code)));
  }
}
