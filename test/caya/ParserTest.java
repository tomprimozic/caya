package caya;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ParserTest {

  private final static Object ERROR = new Object() { @Override public String toString() { return "$ERROR$"; } };

  @ParameterizedTest(name = "\"{0}\"")
  @MethodSource("syntax")
  void test_syntax(String code, Object expected) {
    Object result = ERROR;
    try {
      result = Node.show(Parser.parse(code));
    } catch (Parser.ParserError e) {}
    assertEquals(expected, result, "\"" + code + "\"");
  }

  private static Stream<Arguments> syntax() {
    return Stream.of(
      arguments("", "Seq[[]]"),
      arguments(";", "Seq[[]]"),
      arguments(";;", "Seq[[]]"),
      arguments("x", "Ident[x]"),
      arguments(";x", "Ident[x]"),
      arguments(";;x;", "Ident[x]"),
      arguments("x;", "Ident[x]"),
      arguments("x;;", "Ident[x]"),
      arguments("none", "None[]"),
      arguments("x; 1", "Seq[[Ident[x], Int[1]]]"),
      arguments("x; 1;", "Seq[[Ident[x], Int[1]]]"),
      arguments("x; 1;;", "Seq[[Ident[x], Int[1]]]"),
      arguments("x;; 1;", "Seq[[Ident[x], Int[1]]]"),
      arguments("false", "Bool[false]"),
      arguments("1.x", "Attr[Int[1], x]"),
      arguments("(1)", "Int[1]"),
      arguments("f()", "Call[Ident[f], []]"),
      arguments("f(1)", "Call[Ident[f], [Int[1]]]"),
      arguments("[]", "Vector[[]]"),
      arguments("[1]", "Vector[[Int[1]]]"),
      arguments("[1,]", "Vector[[Int[1]]]"),
      arguments("[9, 7,]", "Vector[[Int[9], Int[7]]]"),
      arguments("[1, 2, 3]", "Vector[[Int[1], Int[2], Int[3]]]"),
      arguments("a.push(1, 8)", "Call[Attr[Ident[a], push], [Int[1], Int[8]]]"),
      arguments("_", "Ident[_]"),
      arguments("x._", "Attr[Ident[x], _]"),
      arguments("x[0]", "Item[Ident[x], [Int[0]]]"),
      arguments("x = 1", "Assign[Ident[x], Int[1]]"),
      arguments("4; z = true; 'm'", "Seq[[Int[4], Assign[Ident[z], Bool[true]], Str[m]]]"),
      arguments("f(x) = y", "Assign[Call[Ident[f], [Ident[x]]], Ident[y]]"),
      arguments("x[0] = 1", "Assign[Item[Ident[x], [Int[0]]], Int[1]]"),
      arguments("1 + 2", "Binary[Ident[+], Int[1], Int[2]]"),
      arguments("1 + 2 + 3", "Binary[Ident[+], Binary[Ident[+], Int[1], Int[2]], Int[3]]"),
      arguments("f(x) = x + 1", "Assign[Call[Ident[f], [Ident[x]]], Binary[Ident[+], Ident[x], Int[1]]]"),
      arguments("h(x) = if x then 3 else -7", "Assign[Call[Ident[h], [Ident[x]]], IfElse[Ident[x], Int[3], Unary[Ident[-], Int[7]]]]"),
      arguments("-1 * 2", "Unary[Ident[-], Binary[Ident[*], Int[1], Int[2]]]"),
      arguments("1 < x <= 5", "Cmp[[Int[1], Ident[<], Ident[x], Ident[<=], Int[5]]]"),
      arguments("if x {}", "If[Ident[x], Seq[[]]]"),
      arguments("if x { return 1  }", "If[Ident[x], Seq[[Return[Int[1]]]]]"),
      arguments("if x { return 1; }", "If[Ident[x], Seq[[Return[Int[1]]]]]"),
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
      arguments("{x=1, y=2, }", "Record[[Arg[Ident[x], Int[1]], Arg[Ident[y], Int[2]]]]"),
      arguments("{x=3, ...a, y=a}", "Record[[Arg[Ident[x], Int[3]], Spread[Ident[a]], Arg[Ident[y], Ident[a]]]]"),
      arguments("(1, 3, 4).pop()", "Call[Attr[Tuple[[Int[1], Int[3], Int[4]]], pop], []]"),
      arguments("(1; 2, 3; 4)", "Seq[[Int[1], Tuple[[Int[2], Int[3]]], Int[4]]]"),
      arguments("try { throw 1 } catch e { print e }", "Try[Seq[[Throw[Int[1]]]], Ident[e], Seq[[Print[Ident[e]]]]]"),
      arguments("!{true = 1, 'x' = z}", "Dict[[Arg[Bool[true], Int[1]], Arg[Str[x], Ident[z]]]]"),
      arguments("[true = 1, 'x' = z]", "Vector[[Arg[Bool[true], Int[1]], Arg[Str[x], Ident[z]]]]"),
      arguments("![1, false, '.']", "List[[Int[1], Bool[false], Str[.]]]"),
      arguments("a, b = 1, 2", "Assign[Tuple[[Ident[a], Ident[b]]], Tuple[[Int[1], Int[2]]]]"),
      arguments("var a, b = 1, 2", "VarAssign[Tuple[[Ident[a], Ident[b]]], Tuple[[Int[1], Int[2]]]]"),
      arguments("(a, b) = (1, 2)", "Assign[Tuple[[Ident[a], Ident[b]]], Tuple[[Int[1], Int[2]]]]"),
      arguments("(a, b = 1, 2)", "Seq[[Assign[Tuple[[Ident[a], Ident[b]]], Tuple[[Int[1], Int[2]]]]]]"),
      arguments("f(a, b = 1, 2)", "Call[Ident[f], [Ident[a], Arg[Ident[b], Int[1]], Int[2]]]"),
      arguments("f(a = x)", "Call[Ident[f], [Arg[Ident[a], Ident[x]]]]"),
      arguments("f((a = x))", "Call[Ident[f], [Seq[[Assign[Ident[a], Ident[x]]]]]]"),
      arguments("f((a; b))", "Call[Ident[f], [Seq[[Ident[a], Ident[b]]]]]"),
      arguments("(x; a, b = 1, 2; y)", "Seq[[Ident[x], Assign[Tuple[[Ident[a], Ident[b]]], Tuple[[Int[1], Int[2]]]], Ident[y]]]"),
      arguments("(   a, b = 1, 2; y)", "Seq[[Assign[Tuple[[Ident[a], Ident[b]]], Tuple[[Int[1], Int[2]]]], Ident[y]]]"),
      arguments("func f() { }; f()", "Seq[[Func[Call[Ident[f], []], Seq[[]]], Call[Ident[f], []]]]"),
      arguments("func f() { }  f()", "Seq[[Func[Call[Ident[f], []], Seq[[]]], Call[Ident[f], []]]]"),
      arguments("(1;)", "Seq[[Int[1]]]"),
      arguments("1 + (1;)", "Binary[Ident[+], Int[1], Seq[[Int[1]]]]"),
      arguments("return;", "Return[null]"),
      arguments("return 1;", "Return[Int[1]]"),
      arguments("print 1, 2", "Print[Tuple[[Int[1], Int[2]]]]"),
      arguments("(", "Err[statements1]"),
      arguments(")", "Err[statements1]"),
      arguments("()", "Err[statements1]"),
      arguments("(,)", "Err[statements1]"),
      arguments("(1, )", "Err[statements1]"),
      arguments("{,}", "Record[[Err[arg]]]"),
      arguments("[,]", "Vector[[Err[arg]]]"),
      arguments("==", "Err[statements1]"),
      arguments("12__34", ERROR),
      arguments("1_", ERROR),
      arguments("0b00112", ERROR),
      arguments("0o5679", ERROR),
      arguments("0x1fg", ERROR),
      arguments("*", "Err[statements1]"),
      arguments("(;)", "Err[statements1]"),
      arguments("1 + (;)", "Err[statements1]"),
      arguments("(1,2;)", "Err[statements1]"),
      arguments("(1,return;)", "Err[statements1]"),
      arguments("1 + (x, y;)", "Err[statements1]"),
      arguments("return 1,", "Err[statements1]"),
      arguments("return 1,; f", "Seq[[Err[statements1], Ident[f]]]"),
      arguments("a and b or c", ERROR),
      arguments("(,) -> 1", "Seq[[Err[statements1], Int[1]]]"),
      arguments("f(x) and x -> x", ERROR),
      arguments(" (1, 2; 3, 4)", "Err[statements1]"),
      arguments("f(1, 2; 3, 4)", "Call[Ident[f], [Err[arg], Int[4]]]"),
      arguments("f(1; 2, 3; 4)", "Call[Ident[f], [Err[arg]]]"),
      arguments("f(1; 2)", "Call[Ident[f], [Err[arg]]]")
    );
  }

  @ParameterizedTest(name = "\"{0}\"")
  @MethodSource("indentation")
  void test_indentation(String code, Object expected) {
    Object result = ERROR;
    try {
      result = Node.show(Parser.parse(code));
    } catch (Parser.ParserError e) {}
    assertEquals(expected, result, "\"" + code + "\"");
  }

  private static Stream<Arguments> indentation() {
    return Stream.of(
      arguments("1, 2,    \n"
              + "3, 4     \n",
              "Seq[[Tuple[[Int[1], Int[2]]], Tuple[[Int[3], Int[4]]]]]"),
      arguments("1, 2,    \n"
              + "  3, 4   \n",
              "Err[statements1]"),
      arguments("  1, 2,    \n"
              + "3, 4       \n",
              "Seq[[Err[statements1], Tuple[[Int[3], Int[4]]]]]"),
      arguments("  1    \n"
              + "2;     \n"
              + "3;     \n",
              "Seq[[Err[statements1], Int[2], Int[3]]]"),
      arguments("1, (2,     \n"
              + "  3), 4    \n",
              "Tuple[[Int[1], Tuple[[Int[2], Int[3]]], Int[4]]]"),
      arguments("class A    \n"
              + "  test     \n"
              + "  if X     \n"
              + "    x      \n"
              + "    a      \n"
              + "  b        \n"
              + "c          \n",
              "Seq[[Class[Ident[A], Seq[[Ident[test], If[Ident[X], Seq[[Ident[x], Ident[a]]]], Ident[b]]]], Ident[c]]]"),
      arguments("class A              \n"
              + "             test    \n"
              + "             if X    \n"
              + "              x      \n"
              + "              a      \n"
              + "             b       \n"
              + "c                    \n",
              "Seq[[Class[Ident[A], Seq[[Ident[test], If[Ident[X], Seq[[Ident[x], Ident[a]]]], Ident[b]]]], Ident[c]]]"),
      arguments("if a                                     \n"
              + "             if b                        \n"
              + "                        1 + (if c {      \n"
              + "                                 x       \n"
              + "                                 y       \n"
              + "                        }; b) + 3        \n"
              + "                        q                \n"
              + "z                                        \n",
              "Seq[[If[Ident[a], Seq[[If[Ident[b], Seq[[Binary[Ident[+], Binary[Ident[+], Int[1], Seq[[If[Ident[c], Seq[[Ident[x], Ident[y]]]], Ident[b]]]], Int[3]], Ident[q]]]]]]], Ident[z]]]"),
      arguments("if a   \n"
              + "  x[   \n"
              + " 0     \n"
              + "]      \n"
              + "  y    \n"
              + "z      \n",
              "Seq[[If[Ident[a], Seq[[Item[Ident[x], [Int[0]]], Ident[y]]]], Ident[z]]]")
    );
  }
}
