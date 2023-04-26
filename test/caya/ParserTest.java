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
      arguments("none", "None[]"),
      arguments("x; 1", "Seq[[Ident[x], Int[1]]]"),
      arguments("false", "Bool[false]"),
      arguments("1.x", "Attr[Int[1], x]"),
      arguments("(1)", "Int[1]"),
      arguments("f()", "Call[Ident[f], []]"),
      arguments("f(1)", "Call[Ident[f], [Int[1]]]"),
      arguments("[]", "Array[[]]"),
      arguments("[1]", "Array[[Int[1]]]"),
      arguments("[1,]", "Array[[Int[1]]]"),
      arguments("[9, 7,]", "Array[[Int[9], Int[7]]]"),
      arguments("[1, 2, 3]", "Array[[Int[1], Int[2], Int[3]]]"),
      arguments("a.push(1, 8)", "Call[Attr[Ident[a], push], [Int[1], Int[8]]]"),
      arguments("_", "Ident[_]"),
      arguments("x._", "Attr[Ident[x], _]"),
      arguments("x[0]", "Item[Ident[x], [Int[0]]]"),
      arguments("x = 1", "Assign[Ident[x], Int[1]]"),
      arguments("4; z = true; 'm'", "Seq[[Int[4], Assign[Ident[z], Bool[true]], Str[m]]]"),
      arguments("f(x) = y", "Assign[Call[Ident[f], [Ident[x]]], Ident[y]]"),
      arguments("x[0] = 1", "Assign[Item[Ident[x], [Int[0]]], Int[1]]"),
      arguments("f(x) = x + 1", "Assign[Call[Ident[f], [Ident[x]]], Binary[Ident[+], Ident[x], Int[1]]]"),
      arguments("h(x) = if x then 3 else -7", "Assign[Call[Ident[h], [Ident[x]]], IfElse[Ident[x], Int[3], Unary[Ident[-], Int[7]]]]"),
      arguments("-1 * 2", "Unary[Ident[-], Binary[Ident[*], Int[1], Int[2]]]"),
      arguments("1 < x <= 5", "Cmp[[Int[1], Ident[<], Ident[x], Ident[<=], Int[5]]]"),
      arguments("if x {}", "If[Ident[x], Seq[[]]]"),
      arguments("if x { return 1 }", "If[Ident[x], Seq[[Return[Int[1]]]]]"),
      arguments("if x { 1 } else { 2 }", "IfElse[Ident[x], Seq[[Int[1]]], Seq[[Int[2]]]]"),
      arguments("if x { 1 } else if y { 3 }", "IfElse[Ident[x], Seq[[Int[1]]], If[Ident[y], Seq[[Int[3]]]]]"),
      arguments("if x { 1 } else if y { 3 } else { 4 }", "IfElse[Ident[x], Seq[[Int[1]]], IfElse[Ident[y], Seq[[Int[3]]], Seq[[Int[4]]]]]"),
      arguments("while a < 1 { a = a + 1 }", "While[Cmp[[Ident[a], Ident[<], Int[1]]], Seq[[Assign[Ident[a], Binary[Ident[+], Ident[a], Int[1]]]]]]"),
      arguments("fn f(x, y) { return x + y }", "Func[Call[Ident[f], [Ident[x], Ident[y]]], Seq[[Return[Binary[Ident[+], Ident[x], Ident[y]]]]]]"),
      arguments("class A { var x = 1; fn get() { return this.x } }", "Class[Ident[A], Seq[[VarAssign[Ident[x], Int[1]], Func[Call[Ident[get], []], Seq[[Return[Attr[This[], x]]]]]]]]"),
      arguments("var y; var x = 16", "Seq[[Var[Ident[y]], VarAssign[Ident[x], Int[16]]]]"),
      arguments("x = (x = 1)", "Assign[Ident[x], Seq[[Assign[Ident[x], Int[1]]]]]"),
      arguments("class A { fn x = y { 1 } }", "Class[Ident[A], Seq[[Func[Arg[Ident[x], Ident[y]], Seq[[Int[1]]]]]]]"),
      arguments("return", "Return[null]"),
      arguments("return 1", "Return[Int[1]]"),
      arguments("return 1,2", "Return[Tuple[[Int[1], Int[2]]]]"),
      arguments("return 1,2,", "Return[Tuple[[Int[1], Int[2]]]]"),
      arguments("return 1,2;f", "Seq[[Return[Tuple[[Int[1], Int[2]]]], Ident[f]]]"),
      arguments("return 1,2,; f", "Seq[[Return[Tuple[[Int[1], Int[2]]]], Ident[f]]]"),
      arguments("return 1,2,3", "Return[Tuple[[Int[1], Int[2], Int[3]]]]"),
      arguments("a and b and `c", "And[[Ident[a], Ident[b], Atom[c]]]"),
      arguments("a or b or not c", "Or[[Ident[a], Ident[b], Not[Ident[c]]]]"),
      arguments("var y, x = 16, 3", "VarAssign[Tuple[[Ident[y], Ident[x]]], Tuple[[Int[16], Int[3]]]]"),
      arguments("x -> x", "Arrow[[Ident[x]], Ident[x]]"),
      arguments("x -> y -> z", "Arrow[[Ident[x]], Arrow[[Ident[y]], Ident[z]]]"),
      arguments("(x) -> y -> x + y", "Arrow[[Ident[x]], Arrow[[Ident[y]], Binary[Ident[+], Ident[x], Ident[y]]]]"),
      arguments("() -> 1", "Arrow[[], Int[1]]"),
      arguments("x -> 1", "Arrow[[Ident[x]], Int[1]]"),
      arguments("(x) -> 1", "Arrow[[Ident[x]], Int[1]]"),
      arguments("(x,) -> 1", "Arrow[[Ident[x]], Int[1]]"),
      arguments("(x, y) -> 1", "Arrow[[Ident[x], Ident[y]], Int[1]]"),
      arguments("(x, y, ) -> 1", "Arrow[[Ident[x], Ident[y]], Int[1]]"),
      arguments("x -> if b then a else m + 1", "Arrow[[Ident[x]], IfElse[Ident[b], Ident[a], Binary[Ident[+], Ident[m], Int[1]]]]"),
      arguments("x -> y, y -> x", "Tuple[[Arrow[[Ident[x]], Ident[y]], Arrow[[Ident[y]], Ident[x]]]]"),
      arguments("x -> y = y -> x", "Assign[Arrow[[Ident[x]], Ident[y]], Arrow[[Ident[y]], Ident[x]]]"),
      arguments("x -> x and f(x)", "Arrow[[Ident[x]], And[[Ident[x], Call[Ident[f], [Ident[x]]]]]]"),
      arguments("x -> (x and f(x))", "Arrow[[Ident[x]], And[[Ident[x], Call[Ident[f], [Ident[x]]]]]]"),
      arguments("{}", "Record[[]]"),
      arguments("{x}", "Record[[Ident[x]]]"),
      arguments("{x,}", "Record[[Ident[x]]]"),
      arguments("{x=1}", "Record[[Arg[Ident[x], Int[1]]]]"),
      arguments("{1=x}", "Record[[Arg[Int[1], Ident[x]]]]"),
      arguments("{x=1, y=2}", "Record[[Arg[Ident[x], Int[1]], Arg[Ident[y], Int[2]]]]"),
      arguments("{x=1, y=2, }", "Record[[Arg[Ident[x], Int[1]], Arg[Ident[y], Int[2]]]]")
    );
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "(",
    ")",
    "()",
    "(,)",
    "(1, )",
    "{,}",
    "[,]",
    "==",
    "12__34",
    "1_",
    "0b00112",
    "0o5679",
    "0x1fg",
    "*",
    "(;)",
    "a;b;",
    "1 + (;)",
    "(1;)",
    "1 + (1;)",
    "(1,2;)",
    "(1,return;)",
    "1 + (x, y;)",
    "return;",
    "return 1;",
    "return 1,",
    "return 1,; f",
    "a and b or c",
    "(,) -> 1",
    "f(x) and x -> x",
  })
  void test_error(String code) {
    assertThrows(Parser.ParserError.class, () -> ParserHelper.parse(code));
  }
}
