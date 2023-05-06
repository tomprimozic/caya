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
  private StringBuffer buffer = new StringBuffer();
  public final ArrayList<ParsingError> errors = new ArrayList<>();

  public int token(int token) {
    if(token != STRING) { this.start_pos = yychar; }
    this.value = null;
    return token;
  }

  public int token(int token, String value) {
    if(token != STRING) { this.start_pos = yychar; }
    this.value = value;
    return token;
  }

  public String getLVal() {
    return this.value;
  }

  public void yyerror (Parser.Location location, String msg) {
    throw new Parser.ParserError(location, msg);
  }

  private void error(String msg) {
    throw new Parser.ParserError(new Parser.Location(getStartPos()), msg);
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

%state SINGLE_STRING
%state DOUBLE_STRING

%%

<YYINITIAL> {
  [ \t\n]                       { /* whitespace */ }

  /* symbols */
  ","                         { return token(COMMA); }
  "."                         { return token(DOT); }
  "..."                       { return token(DOTDOTDOT); }
  ";"                         { return token(SEMICOLON); }
  "="                         { return token(ASSIGN); }
  "<"                         { return token(LT); }
  ">"                         { return token(GT); }
  "=="                        { return token(EQ); }
  "!="                        { return token(NE); }
  "<="                        { return token(LE); }
  ">="                        { return token(GE); }
  "->"                        { return token(ARROW); }
  "-"                         { return token(MINUS); }
  "+"                         { return token(PLUS); }
  "*"                         { return token(STAR); }
  "/"                         { return token(SLASH); }
  "%"                         { return token(PERCENT); }
  "^"                         { return token(CARET); }
  "|"                         { return token(PIPE); }
  "&"                         { return token(AMPERSAND); }
  "_"                         { return token(UNDERSCORE); }
  "?"                         { return token(WHAT); }
  "!"                         { return token(BANG); }
  "@"                         { return token(AT); }
  "#"                         { return token(HASH); }
  "("                         { return token(LPAREN); }
  ")"                         { return token(RPAREN); }
  "["                         { return token(LBRACKET); }
  "]"                         { return token(RBRACKET); }
  "{"                         { return token(LBRACE); }
  "}"                         { return token(RBRACE); }

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
  "this"                      { return token(THIS); }
  "super"                     { return token(SUPER); }
  "forall"                    { return token(FORALL); }
  "exists"                    { return token(EXISTS); }
  "do"                        { return token(DO); }
  "try"                       { return token(TRY); }
  "catch"                     { return token(CATCH); }
  "finally"                   { return token(FINALLY); }
  "throw"                     { return token(THROW); }

  /* the rest */
  \'                          { buffer.setLength(0); start_pos = yychar; yybegin(SINGLE_STRING); }
  \"                          { buffer.setLength(0); start_pos = yychar; yybegin(DOUBLE_STRING); }
  {integer}                   { return token(INTEGER, yytext()); }
  `{identifier}               { return token(ATOM, yytext().substring(1)); }
  {identifier}                { return token(IDENT, yytext()); }
}

<SINGLE_STRING> {
  "'"                         { yybegin(YYINITIAL); return token(STRING, buffer.toString()); }
  [^\n\r\'\\]+                { buffer.append(yytext()); }
}

<DOUBLE_STRING> {
  "\""                        { yybegin(YYINITIAL); return token(STRING, buffer.toString()); }
  [^\n\r\"\\]+                { buffer.append(yytext()); }
}

<SINGLE_STRING,DOUBLE_STRING> {
  /* escapes */
  \\t                         { buffer.append('\t'); }
  \\n                         { buffer.append('\n'); }
  \\r                         { buffer.append('\r'); }
  \\\'                        { buffer.append('\''); }
  \\\"                        { buffer.append('"'); }
  \\\\                        { buffer.append('\\'); }
  \\ [^tnr\'\"\\]             { error("invalid escape: " + yytext()); }

  /* normalize newlines */
  \n | \r | \r\n              { buffer.append( '\n' ); }

  <<EOF>>                     { yybegin(YYINITIAL); return token(ERROR, "EOF"); }
}

[^]                           { return token(ERROR, yytext()); }

<<EOF>>                       { return token(EOF); }