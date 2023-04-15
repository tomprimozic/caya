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
  ","                         { return token(COMMA); }
  "."                         { return token(DOT); }
  "("                         { return token(LPAREN); }
  ")"                         { return token(RPAREN); }
  "["                         { return token(LBRACKET); }
  "]"                         { return token(RBRACKET); }

  /* keywords */
  "none"                      { return token(NONE); }
  "true"                      { return token(TRUE); }
  "false"                     { return token(FALSE); }
  "let"                       { return token(LET); }
  "var"                       { return token(VAR); }
  "if"                        { return token(IF); }
  "then"                      { return token(THEN); }
  "else"                      { return token(ELSE); }
  "for"                       { return token(FOR); }
  "in"                        { return token(IN); }
  "while"                     { return token(WHILE); }
  "break"                     { return token(BREAK); }
  "continue"                  { return token(CONTINUE); }
  "fn"                        { return token(FUNC); }
  "fun"                       { return token(FUNC); }
  "func"                      { return token(FUNC); }
  "function"                  { return token(FUNC); }
  "return"                    { return token(RETURN); }
  "print"                     { return token(PRINT); }
  "module"                    { return token(MODULE); }
  "import"                    { return token(IMPORT); }
  "not"                       { return token(NOT); }
  "and"                       { return token(AND); }
  "or"                        { return token(OR); }
  "match"                     { return token(MATCH); }
  "type"                      { return token(TYPE); }
  "record"                    { return token(RECORD); }
  "struct"                    { return token(STRUCT); }
  "class"                     { return token(CLASS); }
  "forall"                    { return token(FORALL); }
  "exists"                    { return token(EXISTS); }
  "do"                        { return token(DO); }
  "try"                       { return token(TRY); }
  "catch"                     { return token(CATCH); }
  "throw"                     { return token(THROW); }

  /* the rest */
  {integer}                   { return token(INTEGER, yytext()); }
  {identifier}                { return token(IDENT, yytext()); }
}

[^]                           { return token(ERROR, yytext()); }

<<EOF>>                       { return token(EOF); }