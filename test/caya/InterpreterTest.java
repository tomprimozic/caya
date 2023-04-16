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
    assertEquals(expected, Interpreter.eval(Parser.parse(code)).toString());
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
      arguments("if true then 1 else 'y'", "1"),
      arguments("if false then 1 else 'y'", "y"),
      arguments("l = []; if true then l.append(1) else l.append(2); l", "[1]"),
      arguments("l = []; if true then l.append(1) else l.append(2); l", "[1]"),
      arguments("f(x) = x + 1", "none"),
      arguments("f(x) = x + 1; f(3)", "4"),
      arguments("f(x) = x + y; y = 4; f(3)", "7"),
      arguments("f(x) = x + y; y = 4; a = []; a.append(f(3)); y = -9; a.append(f(3)); a", "[7, -6]"),
      arguments("m() = (x = 0; get() = x; set(y) = (x = y; -1); [get, set]); a = m(); b = m(); [a[0](), b[0](), a[1](4), b[1](9), a[0](), b[0]()]", "[0, 0, -1, -1, 4, 9]"),
      arguments("1 < 5", "true"),
      arguments("1 > 5", "false"),
      arguments("1 <= 2 > -1 < 7 == 7 != 4", "true"),
      arguments("l = []; l.append((l.append(0); 1) <= (l.append(1); 2) > (l.append(2); 6) < (l.append(3); 17)); l", "[0, 1, 2, false]"),
      arguments("l = []; while l.size < 4 { l.append(0) }; l", "[0, 0, 0, 0]"),
      arguments("while false {}", "none")
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "y",
    "1 + true",
    "if 1 then 'x' else 'y'",
    "[][true]",
    "f(x) = x + y; f(3)",
    "m() = (x = 0; 1); m(); x",
    "while 1 {}"
  })
  void test_error(String code) {
    assertThrows(Interpreter.InterpreterError.class, () -> Interpreter.eval(Parser.parse(code)));
  }
}
