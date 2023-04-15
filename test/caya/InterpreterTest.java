package caya;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class InterpreterTest {
  @ParameterizedTest
  @MethodSource("exprs")
  void test_interpreter(String code, String expected) {
    assertEquals(expected, new Interpreter().eval(Parser.parse(code)).toString());
  }

  private static Stream<Arguments> exprs() {
    return Stream.of(
      arguments("false", "false"),
      arguments("1", "1"),
      arguments("[1, 2, 3]", "[1, 2, 3]"),
      arguments("[1, 2, 3].size", "3"),
      arguments("[].append(1)", "none"),
      arguments("'xyzz'.size", "4"),
      arguments("\"'\".join([4,6,7,])", "4'6'7"),
      arguments("", "none"),
      arguments("7; 3", "3")
    );
  }
}
