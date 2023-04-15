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
    public ParserError(Parser.Location location, String msg) { super(location.toString() + ": " + msg); }
    public ParserError(String msg) { super(msg); }
    public ParserError(Exception cause) { super(cause); }
  }
}

%token <String> IDENT INTEGER ERROR
%token NONE TRUE FALSE LET VAR IF THEN ELSE FOR IN WHILE RETURN BREAK CONTINUE
%token NOT OR AND FUNC PRINT MODULE IMPORT MATCH TYPE RECORD STRUCT CLASS FORALL EXISTS DO TRY CATCH THROW
%token DOT "." COMMA ","
%token LPAREN "(" RPAREN ")" LBRACKET "[" RBRACKET "]"


%type <Node> expr term
%type <List<Node>> exprs0 exprs1

%%

start:
  expr                              { this.result = $1; }

expr:
  term

term:
    IDENT                           { $$ = ident(@$, $1); }
  | INTEGER                         { $$ = integer(@$, $1); }
  | TRUE                            { $$ = bool(@$, true); }
  | FALSE                           { $$ = bool(@$, false); }
  | NONE                            { $$ = none(@$); }
  | ERROR                           { $$ = error(@$, "invalid token: " + $1); }
  | term "." IDENT                  { $$ = field(@$, $1, $3); }
  | "(" expr ")"                    { $$ = paren(@$, $2); }
  | "[" exprs0 "]"                  { $$ = array(@$, $2); }
  | term "(" exprs0 ")"             { $$ = call(@$, $1, $3); }
  | term "[" exprs0 "]"             { $$ = item(@$, $1, $3); }

exprs0:
    %empty                          { $$ = list(); }
  | exprs1
  | exprs1 ","

exprs1:
    expr                            { $$ = list($1); }
  | exprs1 "," expr                 { $$ = list($1, $3); }