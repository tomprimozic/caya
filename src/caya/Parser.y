%code imports {
  import java.util.*;
}

%language "Java"
%define api.parser.class {Parser}
%define api.package caya
%define api.parser.extends {ParserHelper}
%define api.parser.final
%define api.value.type {Object}
%locations
%define api.position.type {Pos}
%define parse.error custom
%define parse.trace

%code {
  Node result = null;

  public static String token_name(int token) { return yytranslate_(token).getName(); }

  public static final class ParserError extends RuntimeException {
    public ParserError(Parser.Location location, String msg) {
      super(location.toString() + ": " + msg);
    }
  }
}

%token <String> IDENT INTEGER ERROR
%token NONE TRUE FALSE
%token DOT "."

%type <Node> expr term

%%

start:
  expr                      { this.result = $1; }

expr:
  term

term:
    INTEGER                         { $$ = integer(@$, $1); }
  | TRUE                            { $$ = bool(@$, true); }
  | FALSE                           { $$ = bool(@$, false); }
  | NONE                            { $$ = none(@$); }
  | ERROR                           { $$ = error(@$, "invalid token: " + $1); }
  | term "." IDENT                  { $$ = field(@$, $1, $3); }
