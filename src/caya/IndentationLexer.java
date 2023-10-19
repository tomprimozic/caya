package caya;

import java.io.IOException;
import java.util.Queue;
import java.util.ArrayList;
import java.util.ArrayDeque;

import caya.Parser.Context;
import caya.Parser.Location;
import caya.ParserHelper.Pos;

public class IndentationLexer implements Parser.Lexer {
  public record Token(int token) {}

  public sealed interface SignificantWhitespace {
    record Enabled(int previous_indent) implements SignificantWhitespace {}
    record Disabled() implements SignificantWhitespace {}
    static final Disabled Disabled = new Disabled();
  }

  private final Lexer lexer;
  public final Queue<Token> pending_tokens;    // queue - pending tokens are always trivial - OUTDENT, EOF (except the last one), so we don't need to store the `value` as well
  public final ArrayDeque<Integer> indents;    // stack - list of past indents we've seen - after a newline, the indentation level must match a previous indentation level
  public final ArrayDeque<SignificantWhitespace> states;     // stack - remembers indentation level (or -1 for "no whitespace" mode) for each `({[` we see
  private String value = null;
  public final ArrayList<Lexer.ParsingError> errors;
  public String last_newline = null;
  public int last_token = EOF;

  public IndentationLexer(Lexer lexer) {
    this.lexer = lexer;
    this.pending_tokens = new ArrayDeque<>();
    this.indents = new ArrayDeque<>();
    this.indents.push(0);
    this.states = new ArrayDeque<>();
    this.states.push(new SignificantWhitespace.Enabled(0));
    this.errors = lexer.errors;
  }

  // delegate
  @Override public void yyerror(Location loc, String msg) { lexer.yyerror(loc, msg); }
  @Override public void reportSyntaxError(Context ctx) { lexer.reportSyntaxError(ctx); }
  @Override public Pos getStartPos() { return lexer.getStartPos(); }
  @Override public Pos getEndPos() { return lexer.getEndPos(); }

  @Override public Object getLVal() { return this.value; }

  public void enter(boolean significant_whitespace) {
    // sets current mode to either consider or ignore newlines and indentation
    // mode is automatically reset on each ending delimiter `)}]`
    assert this.pending_tokens.isEmpty();
    this.states.push(significant_whitespace ? new SignificantWhitespace.Enabled(indents.peek()) : SignificantWhitespace.Disabled);
  }

  public static int calculate_indent(String whitespace) {
    assert !whitespace.contains("\t");                                // TODO: allow tabs
    return whitespace.length() - whitespace.lastIndexOf('\n') - 1;
  }

  public void dedent(int to_indent) {
    while(indents.peek() > to_indent) {
      indents.pop();
      pending_tokens.add(new Token(OUTDENT));
      if(indents.peek() < to_indent) {
        lexer.error("indents don't match");
      }
    }
  }

  @Override public int yylex() throws IOException {
    var t = next_token();
    if(t == NEWLINE) {
      last_newline = lexer.getLVal();
    } else if(t != EOF) {
      last_token = t;
    }
    return t;
  }

  private int next_token() throws IOException {
    if(!pending_tokens.isEmpty()) { return pending_tokens.remove().token; }
    int token = lexer.yylex();
    this.value = lexer.getLVal();
    switch(token) {
      case EOF:
        dedent(0);
        pending_tokens.add(new Token(EOF));
        return pending_tokens.remove().token;
      case NEWLINE:
        if(!states.isEmpty() && states.peek() == SignificantWhitespace.Disabled) { return next_token(); }
        int indent = calculate_indent(value);
        if(indent > this.indents.peek()) {
          indents.push(indent);
          return INDENT;
        } else {
          dedent(indent);
          return NEWLINE;
        }
      case RPAREN:
      case RBRACE:
      case RBRACKET:
        if(states.size() <= 1) { return token; }    // probably an error, e.g. an unmatched )
        switch(states.pop()) {
          case SignificantWhitespace.Disabled() -> { return token; }
          case SignificantWhitespace.Enabled(var previous_indent) -> {
            dedent(previous_indent);
            pending_tokens.add(new Token(token));
            return pending_tokens.remove().token;
          }
        }
      default:
        return token;
    }
  }
}
