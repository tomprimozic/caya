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
  @MethodSource("exprs")
  void test_interpreter(String code, String expected) {
    assertEquals(expected, new Interpreter().eval(Parser.parse(code)).toString());
  }

  private static Stream<Arguments> exprs() {
    return Stream.of(
        arguments("false", "false"),
        arguments("1", "1"));
  }
}
