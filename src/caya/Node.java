package caya;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Pattern;

import caya.Parser.Location;

sealed public interface Node {
  final static Pattern remove_loc = Pattern.compile("loc=Pos\\[pos=\\d+\\](\\-Pos\\[pos=\\d+\\])?(, |(?=]))");
  final static Pattern remove_field_names = Pattern.compile("(?<=\\[|, )[_a-z]+=");
  public static String show(Node n) {
    var s = remove_loc.matcher(n.toString()).replaceAll("");
    s = remove_field_names.matcher(s).replaceAll("");
    return s;
  }

  public Location loc();

  record Err(Location loc, String error) implements Node {}
  record Ident(Location loc, String name) implements Node {}
  record Atom(Location loc, String name) implements Node {}
  record Int(Location loc, BigInteger value) implements Node {}
  record Bool(Location loc, boolean value) implements Node {}
  record Str(Location loc, String value) implements Node {}
  record None(Location loc) implements Node {}
  record Attr(Location loc, Node expr, String attr) implements Node {}
  record Arg(Location loc, Node name, Node value) implements Node {}
  record Record(Location loc, List<Node> fields) implements Node {}
  record Spread(Location loc, Node expr) implements Node {}
  record Tuple(Location loc, List<Node> items) implements Node {}
  record Array(Location loc, List<Node> items) implements Node {}
  record Item(Location loc, Node expr, List<Node> items) implements Node {}
  record Call(Location loc, Node fn, List<Node> args) implements Node {}
  record Seq(Location loc, List<Node> exprs) implements Node {}
  record Assign(Location loc, Node pattern, Node value) implements Node {}
  record Var(Location loc, Node pattern) implements Node {}
  record VarAssign(Location loc, Node pattern, Node value) implements Node {}
  record Unary(Location loc, Ident op, Node expr) implements Node {}
  record Binary(Location loc, Ident op, Node left, Node right) implements Node {}
  record If(Location loc, Node cond, Node then) implements Node {}
  record IfElse(Location loc, Node cond, Node then, Node else_) implements Node {}
  record And(Location loc, List<Node> exprs) implements Node {}
  record Or(Location loc, List<Node> exprs) implements Node {}
  record Not(Location loc, Node expr) implements Node {}
  record Cmp(Location loc, List<Node> parts) implements Node {}
  record Arrow(Location loc, List<Node> params, Node body) implements Node {}
  record Func(Location loc, Node declaration, Seq body) implements Node {}
  record Return(Location loc, Node value) implements Node {}
  record While(Location loc, Node cond, Seq body) implements Node {}
  record Class(Location loc, Node declaration, Seq body) implements Node {}
  record This(Location loc) implements Node {}
}
