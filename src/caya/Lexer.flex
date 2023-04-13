package caya;

import java.util.ArrayList;

import caya.ParserHelper.Pos;

%%

%public
%class Lexer
%implements Parser.Lexer
%integer
%char

%{
  public String value = null;
  private long start_pos;
  public final ArrayList<ParsingError> errors = new ArrayList<>();

  public int token(int token) {
    this.start_pos = yychar;
    this.value = null;
    return token;
  }

  public int token(int token, String value) {
    this.start_pos = yychar;
    this.value = value;
    return token;
  }

  public String getLVal() {
    return this.value;
  }

  public void yyerror (Parser.Location location, String msg) {
    throw new Parser.ParserError(location, msg);
  }

  public void reportSyntaxError(Parser.Context ctx) {
    var lookahead = ctx.getToken();
    var expected = new Parser.SymbolKind[100];
    var count = ctx.getExpectedTokens(expected, expected.length);
    expected = java.util.Arrays.copyOf(expected, count);
    errors.add(new ParsingError(ctx.getLocation(), lookahead, expected));
  }

  record ParsingError(Parser.Location loc, Parser.SymbolKind lookahead, Parser.SymbolKind[] expected) {}

  public Pos getStartPos() { return new Pos(start_pos); }
  public Pos getEndPos() { return new Pos(yychar + (zzMarkedPos - zzStartRead)); }
%}

identifier = [_a-zA-Z][_a-zA-Z0-9]*

integer = [0-9](_?[0-9]+)*

%%

<YYINITIAL> {
  [ \t\n]                       { /* whitespace */ }

  /* symbols */
  "."                         { return token(DOT); }

  /* keywords */
  "none"                      { return token(NONE); }
  "true"                      { return token(TRUE); }
  "false"                     { return token(FALSE); }

  /* the rest */
  {integer}                   { return token(INTEGER, yytext()); }
  {identifier}                { return token(IDENT, yytext()); }
}

[^]                           { return token(ERROR, yytext()); }

<<EOF>>                       { return token(EOF); }