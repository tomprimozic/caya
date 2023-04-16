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
  List<Node> result = null;

  public static String token_name(int token) { return yytranslate_(token).getName(); }

  public static final class ParserError extends RuntimeException {
    public ParserError(Parser.Location location, String msg) { super(location.toString() + ": " + msg); }
    public ParserError(String msg) { super(msg); }
    public ParserError(Exception cause) { super(cause); }
  }
}

%token <String> IDENT INTEGER STRING ERROR
%token NONE TRUE FALSE LET VAR IF THEN ELSE FOR IN WHILE RETURN BREAK CONTINUE
%token NOT OR AND FUNC PRINT MODULE IMPORT MATCH TYPE RECORD STRUCT CLASS THIS FORALL EXISTS DO TRY CATCH THROW
%token DOT "." COMMA "," SEMICOLON ";" ASSIGN "="
%token PLUS "+" MINUS "-" STAR "*"
%token EQ "==" NE "!=" GT ">" LT "<" GE ">=" LE "<="
%token LPAREN "(" RPAREN ")" LBRACKET "[" RBRACKET "]" LBRACE "{" RBRACE "}"

%type <Node> statement block_if expr arith term
%type <List<Node>> exprs0 exprs1 statements0 statements1 cmp
%type <Node.Seq> block
%type <String> cmp_op

%left "+" "-"
%precedence UNARY
%left "*"

%%

start:
    statements0 YYEOF               { this.result = $1; }

statements0:
    %empty                          { $$ = list(); }
  | statements1

statements1:
    statement                       { $$ = list($1); }
  | statements1 ";" statement       { $$ = list($1, $3); }

statement:
    expr
  | expr "=" expr                   { $$ = assign(@$, $1, $3); }
  | VAR expr                        { $$ = var(@$, $2); }
  | VAR expr "=" expr               { $$ = var(@$, $2, $4); }
  | RETURN                          { $$ = return_(@$); }
  | RETURN expr                     { $$ = return_(@$, $2); }
  | block_if
  | WHILE expr block                { $$ = while_(@$, $2, $3); }
  | FUNC expr block                 { $$ = func(@$, $2, $3); }
  | CLASS expr block                { $$ = class_(@$, $2, $3); }

block_if:
    IF expr block                   { $$ = if_block(@$, $2, $3); }
  | IF expr block ELSE block        { $$ = if_block(@$, $2, $3, $5); }
  | IF expr block ELSE block_if     { $$ = if_block(@$, $2, $3, $5); }

block:
    "{" statements0 "}"             { $$ = seq(@$, $2); }

expr:
    arith
  | IF expr THEN expr ELSE expr     { $$ = if_expr(@$, $2, $4, $6); }
  | cmp                             { $$ = cmp(@$, $1); }

cmp:
    arith cmp_op arith              { $$ = list($1, ident(@2, $2), $3); }
  | cmp cmp_op arith                { $$ = list($1, ident(@2, $2), $3); }

cmp_op:
    "=="                            { $$ = "=="; }
  | "!="                            { $$ = "!="; }
  | "<"                             { $$ = "<"; }
  | ">"                             { $$ = ">"; }
  | "<="                            { $$ = "<="; }
  | ">="                            { $$ = ">="; }

arith:
    term
  | "-" arith          %prec UNARY  { $$ = unary(@$, ident(@1, "-"), $2); }
  | arith "+" arith                 { $$ = binary(@$, $1, ident(@2, "+"), $3); }
  | arith "-" arith                 { $$ = binary(@$, $1, ident(@2, "-"), $3); }
  | arith "*" arith                 { $$ = binary(@$, $1, ident(@2, "*"), $3); }

term:
    IDENT                           { $$ = ident(@$, $1); }
  | INTEGER                         { $$ = integer(@$, $1); }
  | TRUE                            { $$ = bool(@$, true); }
  | FALSE                           { $$ = bool(@$, false); }
  | STRING                          { $$ = str(@$, $1); }
  | NONE                            { $$ = none(@$); }
  | ERROR                           { $$ = error(@$, "invalid token: " + $1); }
  | THIS                            { $$ = this_(@$); }
  | term "." IDENT                  { $$ = attr(@$, $1, $3); }
  | "(" expr ")"                    { $$ = paren(@$, $2); }
  | "(" statement ";" statements1 ")"    { $$ = seq(@$, list($2, $4)); }
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