package caya;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;

import caya.Parser.Location;
import caya.Node.*;

public class ParserHelper {
  record Pos(long pos) {}

  public final ArrayList<Err> errors = new ArrayList<>();

  public static Node parse(String code) {
    var parser = new Parser(new Lexer(new java.io.StringReader(code)));
    try {
      parser.parse();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if(!parser.errors.isEmpty()) {
      throw new RuntimeException("parsing errors: " + parser.errors);
    }
    if(parser.result == null) {
      throw new RuntimeException("failed to parse");
    }
    return parser.result;
  }

  private BigInteger parse_integer(String s) {
      return new BigInteger(s.replaceAll("_", ""));
  }

  Node error(Location loc, String error) { Err e = new Err(loc, error); errors.add(e); return e; }
  Node integer(Location loc, String value) { return new Int(loc, parse_integer(value)); }
  Node bool(Location loc, boolean value) { return new Bool(loc, value); }
  Node none(Location loc) { return new None(loc); }
  Node field(Location loc, Node obj, String field) { return new Field(loc, obj, field); }
}
