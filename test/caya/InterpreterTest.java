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
      arguments("![].append(1)", "none"),
      arguments("[].append(1)", "[1]"),
      arguments("[].append(1, 2)", "[1, 2]"),
      arguments("'xyzz'.size", "4"),
      arguments("\"'\".join([4,6,7,])", "4'6'7"),
      arguments("'  '.join(![4,6,7,])", "4  6  7"),
      arguments("", "none"),
      arguments("7; 3", "3"),
      arguments("x = 1; x", "1"),
      arguments("x = 1; x = 4; x", "4"),
      arguments("l = ![]; l.append(1); l.append(5); l", "![1, 5]"),
      arguments("l = ![]; l.append(7, 3); l", "![7, 3]"),
      arguments("l = ![]; (l.append(0); l).append((l.append(1); 4), (l.append(2); 5), (l.append(3); 6)); l", "![0, 1, 2, 3, 4, 5, 6]"),
      arguments("1 + -1", "0"),
      arguments("if true then 1 else 'y'", "1"),
      arguments("if false then 1 else 'y'", "y"),
      arguments("l = ![]; if true then l.append(1) else l.append(2); l", "![1]"),
      arguments("l = ![]; if false then l.append(1) else l.append(2); l", "![2]"),
      arguments("f(x) = x + 1", "none"),
      arguments("f(x) = x + 1; f(3)", "4"),
      arguments("func f(x) { x + 1 } f(3)", "4"),
      arguments("f(x) = (return x + 1; 7); f(4)", "5"),
      arguments("func f(x) { return x + 1; 7; } f(4)", "5"),
      arguments("f(x) = x + y; y = 4; f(3)", "7"),
      arguments("f = (x) -> x + y; y = 4; f(3)", "7"),
      arguments("f = x -> x + y; y = 4; f(3)", "7"),
      arguments("f = () -> 1 + y; y = 4; f()", "5"),
      arguments("f(x) = x + y; y = 4; a = ![]; a.append(f(3)); y = -9; a.append(f(3)); a", "![7, -6]"),
      arguments("f(x, y) = x + y + 1; f(5, 2)", "8"),
      arguments("f = (x, y) -> x + y + 1; f(5, 2)", "8"),
      arguments("func f(x, y) { x + y + 1 } f(5, 2)", "8"),
      arguments("m() = (x = 0; get() = x; set(y) = (x = y; -1); [get, set]); a = m(); b = m(); [a[0](), b[0](), a[1](4), b[1](9), a[0](), b[0]()]", "[0, 0, -1, -1, 4, 9]"),
      arguments("1 < 5", "true"),
      arguments("1 > 5", "false"),
      arguments("1 <= 2 > -1 < 7 == 7 != 4", "true"),
      arguments("l = ![]; l.append((l.append(0); 1) <= (l.append(1); 2) > (l.append(2); 6) < (l.append(3); 17)); l", "![0, 1, 2, false]"),
      arguments("l = ![]; while l.size < 4 { l.append(0); } l", "![0, 0, 0, 0]"),
      arguments("while false {}", "none"),
      arguments("[6, 2, 8].last", "8"),
      arguments("x = 1; f(x) = 0; f(3); x", "1"),
      arguments("x = 1; f() = (x = 4; 0); f(); x", "4"),
      arguments("x = 1; f() = (var x = 4; 0); f(); x", "1"),
      arguments("class A { var x = 0; fn get() { return this.x }; fn set(y) { this.x = y } }; a = A(); b = A(); [a.get(), b.get(), a.set(6), a.get(), b.get(), b.set(2), a.get(), b.get()]", "[0, 0, none, 6, 0, none, 6, 2]"),
      arguments("class X { var a = 0 } x = X(); x.a = 2; x.a", "2"),
      arguments("class X { var a = 0; fn this.b { this.a } fn this.b = x { this.a = x; };; } x = X(); x.b = 7; [x.a, x.b]", "[7, 7]"),
      arguments("`g", "`g"),
      arguments("`g.name", "g"),
      arguments("x = 0; [(x = 1; false) and (x = 2; true) and (x = 3; true), x]", "[false, 1]"),
      arguments("x = 0; [(x = 1; true) and (x = 2; false) and (x = 3; true), x]", "[false, 2]"),
      arguments("x = 0; [(x = 1; true) and (x = 2; true) and (x = 3; false), x]", "[false, 3]"),
      arguments("x = 0; [(x = 1; true) and (x = 2; true) and (x = 3; true), x]", "[true, 3]"),
      arguments("x = 0; [(x = 1; true) or (x = 2; true) or (x = 3; true), x]", "[true, 1]"),
      arguments("x = 0; [(x = 1; false) or (x = 2; true) or (x = 3; true), x]", "[true, 2]"),
      arguments("x = 0; [(x = 1; false) or (x = 2; false) or (x = 3; true), x]", "[true, 3]"),
      arguments("x = 0; [(x = 1; false) or (x = 2; false) or (x = 3; false), x]", "[false, 3]"),
      arguments("x = 1, 2; x", "[1, 2]"),
      arguments("fn f() { return 1, 2 } f()", "[1, 2]"),
      arguments("[math.sign(6), math.sign(-5), math.sign(0)]", "[1, -1, 0]"),
      arguments("x = 1; [(var x = 2; x), x]", "[2, 1]"),
      arguments("x = 1; [(var x = x + 1; x), x]", "[2, 1]"),
      arguments("fn fib(x) { if x < 2 then 1 else fib(x - 1) + fib(x - 2) } [fib(0), fib(1), fib(2), fib(3), fib(4), fib(5), fib(6)]", "[1, 1, 2, 3, 5, 8, 13]"),
      arguments("{}", "{}"),
      arguments("{x=1}", "{x=1}"),
      arguments("{x=1}.x", "1"),
      arguments("{x=1, y=5}.y", "5"),
      arguments("var a = 3; {x=1, a}", "{x=1, a=3}"),
      arguments("var a = {x = 1, b = 2}; {x = 4, ...a, y=6}", "{x=1, y=6, b=2}"),
      arguments("var a = {x = 1, b = 2}; {x = 4, ...a, b=6}", "{x=1, b=6}"),
      arguments("(1, 2, 3).pop()", "[2, 3]"),
      arguments("(1, 2, 3).shift()", "[1, 2]"),
      arguments("(1, 2, 3).push(5)", "[5, 1, 2, 3]"),
      arguments("(1, 2, 3).append(5)", "[1, 2, 3, 5]"),
      arguments("(1, 2, 3)[1]", "2"),
      arguments("(1, 2, 3).update(1, 6)", "[1, 6, 3]"),
      arguments("f(x, y=x+1, z=0) = x + y + z; f(5)", "11"),
      arguments("f(x, y=x+1, z=0) = x + y + z; f(z=2, 5)", "13"),
      arguments("f(x, y=x+1, z=0) = x + y + z; f(z=2, x=5)", "13"),
      arguments("f(x, y=x+1, z=0) = x + y + z; f(5, 2)", "7"),
      arguments("f(x, y=x+1, z=0) = x + y + z; f(5, 2, 1)", "8"),
      arguments("f(x, y=x+1, z=0) = x + y + z; f(5, z=2)", "13"),
      arguments("a = ![]; i = 0; while true { i = i + 1; if i < 5 { continue } if i > 7 { break } a.append(i) } a", "![5, 6, 7]"),
      arguments("a = ![]; for i in [3, 4, 5, 6, 7, 8] { i = i + 1; if i < 5 { continue } if i > 7 { break } a.append(i) } a", "![5, 6, 7]"),
      arguments("f(x) = (try { if x < 0 { throw -1 } x + 4 } catch i { x + i }); [f(3), f(-2)]", "[7, -3]"),
      arguments("m = [1=2, true=false]; [m[true], m[1]]", "[false, 2]"),
      arguments("fn take(i,xs) { it = xs.iter(); r = {iter = () -> r, next = () -> ( if i > 0 { i = i - 1; it.next() } else {iter.stop} ) };  r} ' '.join(take(3, [1,2,3,4,5]))", "1 2 3"),
      arguments("var l = []; for i in [1, 2, 3] { l = l.push(i); } l", "[3, 2, 1]"),
      arguments("for i in [] {}", "none"),
      arguments("a = 0; for i in [] { a = a + i; } a", "0"),
      arguments("list(1,6)", "![1, 6]"),
      arguments("x = ![5, 2, 9]; [list.last(x), list.size(x)]", "[9, 3]"),
      arguments("x = ![5, 2, 0]; list.pop(x); list.append(x, 3, 8); x", "![2, 0, 3, 8]"),
      arguments("[].push(1,5,2,3)", "[3, 2, 5, 1]"),
      arguments("vector.push(vector.empty, 1,5,2,3)", "[3, 2, 5, 1]"),
      arguments("int", "type int"),
      arguments("int(5)", "5"),
      arguments("int('-5')", "-5"),
      arguments("i = ['a' = 1, 5 = 2, false = 4]; [i.size, i.get('a'), index.size(i), index.get(i, false)]", "[3, 1, 3, 4]"),
      arguments("typeof(false)", "type bool"),
      arguments("typeof(typeof(false))", "type type")
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
    "while 1 {}",
    "class X { var a = 0 }; x = X(); x.b = 2",
    "class X { var a = 0 }; x = X(); x.b",
    "class X { var a = 0; var a = 1 }",
    "{}.a",
    "{x=1, x=2}",
    "a = 1; {...a}",
    "a={}; {a=1, ...a, a=1}",
    "f(x, y=x+1, z=0) = x + y + z; f(z=2)",
    "f(x, y=x+1, z=0) = x + y + z; f(z=2, y=5)",
    "list.size([1,2,3])",
  })
  void test_error(String code) {
    assertThrows(Interpreter.InterpreterError.class, () -> Interpreter.eval(Parser.parse(code)));
  }
}
