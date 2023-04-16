package caya;

import java.math.BigInteger;
import java.util.List;
import java.util.regex.Pattern;

import caya.Parser.Location;

sealed public interface Node {
  final static Pattern remove_loc = Pattern.compile("loc=Pos\\[pos=\\d+\\](\\-Pos\\[pos=\\d+\\])?, ");
  final static Pattern remove_field_names = Pattern.compile("(?<=\\[|, )[_a-z]+=");
  public static String show(Node n) {
    var s = remove_loc.matcher(n.toString()).replaceAll("");
    s = remove_field_names.matcher(s).replaceAll("");
    return s;
  }

  public Location loc();

  record Err(Location loc, String error) implements Node {}
  record Ident(Location loc, String name) implements Node {}
  record Int(Location loc, BigInteger value) implements Node {}
  record Bool(Location loc, boolean value) implements Node {}
  record Str(Location loc, String value) implements Node {}
  record None(Location loc) implements Node {}
  record Field(Location loc, Node expr, String field) implements Node {}
  record Array(Location loc, List<Node> items) implements Node {}
  record Item(Location loc, Node expr, List<Node> items) implements Node {}
  record Call(Location loc, Node fn, List<Node> args) implements Node {}
  record Seq(Location loc, List<Node> exprs) implements Node {}
  record Assign(Location loc, Node pattern, Node value) implements Node {}
  record Unary(Location loc, Ident op, Node expr) implements Node {}
  record Binary(Location loc, Ident op, Node left, Node right) implements Node {}
  record If(Location loc, Node cond, Node then, Node else_) implements Node {}
  record Cmp(Location loc, List<Node> parts) implements Node {}
}
