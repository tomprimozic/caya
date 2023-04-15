package caya;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;

import caya.Parser.Location;
import caya.Parser.ParserError;
import caya.Node.*;

public class ParserHelper {
  record Pos(long pos) {}

  public final ArrayList<Err> errors = new ArrayList<>();

  public static Node parse(String code) {
    var parser = new Parser(new Lexer(new java.io.StringReader(code)));
    try {
      parser.parse();
    } catch (IOException e) {
      throw new ParserError(e);
    }
    if(!parser.errors.isEmpty()) {
      throw new ParserError("parsing errors: " + parser.errors);
    }
    if(parser.result == null) {
      throw new ParserError("failed to parse");
    }
    // bison returns a null location if code == "", so we handle it ourselves
    if(parser.result.isEmpty()) {
      var loc = new Location(new Pos(0), new Pos(code.length()));
      return new Node.Seq(loc, List.of());
    } else if(parser.result.size() > 1) {
      var first = parser.result.get(0);
      var last = parser.result.get(parser.result.size() - 1);
      var loc = new Location(first.loc().begin, last.loc().end);
      return new Node.Seq(loc, parser.result);
    } else {
      return parser.result.get(0);
    }
  }

  public static List<String> tokenize(String code) {
    var lexer = new Lexer(new java.io.StringReader(code));
    var tokens = new ArrayList<String>();
    while(true) {
      try {
        var token = lexer.yylex();
        if(token == Lexer.EOF) {
          break;
        }
        var token_name = Parser.token_name(token);
        var value = lexer.value;
        if(value == null) {
          tokens.add(token_name);
        } else {
          tokens.add(token_name + "(" + value + ")");
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return tokens;
  }

  private BigInteger parse_integer(String s) {
    return new BigInteger(s.replaceAll("_", ""));
  }

  List<Node> list() { return new ArrayList<Node>(); }
  List<Node> list(Node node) { return new ArrayList<Node>(List.of(node)); }
  List<Node> list(List<Node> ns, Node n) { ns.add(n); return ns; }

  Node error(Location loc, String error) { Err e = new Err(loc, error); errors.add(e); return e; }

  Node ident(Location loc, String name) { return new Ident(loc, name); }
  Node integer(Location loc, String value) { return new Int(loc, parse_integer(value)); }
  Node bool(Location loc, boolean value) { return new Bool(loc, value); }
  Node str(Location loc, String value) { return new Str(loc, value); }
  Node none(Location loc) { return new None(loc); }
  Node field(Location loc, Node obj, String field) { return new Field(loc, obj, field); }
  Node paren(Location loc, Node expr) { return expr; }    // TODO: should be a different Node, to update location
  Node array(Location loc, List<Node> items) { return new Array(loc, items); }
  Node call(Location loc, Node fn, List<Node> args) { return new Call(loc, fn, args); }
  Node item(Location loc, Node expr, List<Node> items) { return new Item(loc, expr, items); }
  Seq seq(Location loc, List<Node> exprs) { return new Seq(loc, exprs); }
  Node assign(Location loc, Node pattern, Node value) { return new Assign(loc, pattern, value); }
}
