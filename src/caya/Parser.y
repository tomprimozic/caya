%code imports {
  import java.util.*;

  @SuppressWarnings("unchecked")
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

%token <String> IDENT ATOM INTEGER STRING ERROR
%token NONE TRUE FALSE LET VAR IF THEN ELSE FOR IN WHILE RETURN BREAK CONTINUE
%token NOT OR AND FUNC PRINT MODULE IMPORT MATCH TYPE RECORD STRUCT CLASS THIS SUPER FORALL EXISTS DO TRY CATCH THROW FINALLY
%token DOT "." DOTDOTDOT "..." COMMA "," SEMICOLON ";" ASSIGN "=" ARROW "->"
%token PIPE "|" AMPERSAND "&" UNDERSCORE "_" WHAT "?" BANG "!" AT "@" HASH "#"
%token PLUS "+" MINUS "-" STAR "*" SLASH "/" PERCENT "%" CARET "^"
%token EQ "==" NE "!=" GT ">" LT "<" GE ">=" LE "<="
%token LPAREN "(" RPAREN ")" LBRACKET "[" RBRACKET "]" LBRACE "{" RBRACE "}"

%type <Node> line_statement block_statement block_if assign tuple_expr expr arrow simple arith atom term arg
%type <List<Node>> and_exprs or_exprs cmp
%type <List<Node>> exprs0 exprs1 exprs2 args0 args1 statements0 statements1
%type <Node.Seq> block
%type <String> cmp_op

%left "+" "-"
%precedence UNARY
%left "*"

%%

start:
    statements0 YYEOF               { this.result = $1; }

sep1: ";" | sep1 ";"
sep0: %empty | sep1

statements0:
    sep0                            { $$ = list(); }
  | tuple_expr                      { $$ = list($1); }
  | line_statement                  { $$ = list($1); }
  | statements1 tuple_expr          { $$ = list($1, $2); }
  | statements1 line_statement      { $$ = list($1, $2); }
  | statements1

statements1:
    tuple_expr sep1                       { $$ = list($1); }
  | error sep1                            { $$ = list(error(@$, "statements1")); }
  | tuple_expr error sep1                 { $$ = list(error(@$, "tuple_expr")); }
  | line_statement sep1                   { $$ = list($1); }
  | line_statement error sep1             { $$ = list(error(@$, "line_statement")); }
  | block_statement sep0                  { $$ = list($1); }
  | statements1 tuple_expr sep1           { $$ = list($1, $2); }
  | statements1 line_statement sep1       { $$ = list($1, $2); }
  | statements1 block_statement sep0      { $$ = list($1, $2); }

line_statement:
    assign
  | VAR tuple_expr                  { $$ = var(@$, $2); }
  | VAR tuple_expr "=" tuple_expr   { $$ = var(@$, $2, $4); }
  | BREAK                           { $$ = break_(@$); }
  | CONTINUE                        { $$ = continue_(@$); }
  | PRINT                           { $$ = print(@$); }
  | PRINT expr                      { $$ = print(@$, $2); }
  | RETURN                          { $$ = return_(@$); }
  | RETURN tuple_expr               { $$ = return_(@$, $2); }
  | THROW expr                      { $$ = throw_(@$, $2); }

block_statement:
    block_if
  | WHILE expr block                { $$ = while_(@$, $2, $3); }
  | FOR expr IN expr block          { $$ = for_(@$, $2, $4, $5); }
  | FUNC arg block                  { $$ = func(@$, $2, $3); }
  | CLASS expr block                { $$ = class_(@$, $2, $3); }
  | TRY block CATCH term block      { $$ = try_(@$, $2, $4, $5); }

assign:
    tuple_expr "=" tuple_expr       { $$ = assign(@$, $1, $3); }

block_if:
    IF expr block                   { $$ = if_block(@$, $2, $3); }
  | IF expr block ELSE block        { $$ = if_block(@$, $2, $3, $5); }
  | IF expr block ELSE block_if     { $$ = if_block(@$, $2, $3, $5); }

block:
    "{" statements0 "}"             { $$ = seq(@$, $2); }

tuple_expr:
    expr
  | exprs2                          { $$ = tuple(@$, $1); }

expr:
    arrow
  | simple
  | and_exprs                       { $$ = and(@$, $1); }
  | or_exprs                        { $$ = or(@$, $1); }
  | IF expr THEN expr ELSE expr     { $$ = if_expr(@$, $2, $4, $6); }

arrow:
    "(" ")" "->" expr               { $$ = arrow(@$, list(), $4); }
  | term "->" expr                  { $$ = arrow(@$, list($1), $3); }
  | "(" expr "," ")" "->" expr      { $$ = arrow(@$, list($2), $6); }
  | "(" exprs2 ")" "->" expr        { $$ = arrow(@$, $2, $5); }

and_exprs:
    simple AND simple               { $$ = list($1, $3); }
  | and_exprs AND simple            { $$ = list($1, $3); }

or_exprs:
    simple OR simple                { $$ = list($1, $3); }
  | or_exprs OR simple              { $$ = list($1, $3); }

simple:
    arith
  | cmp                             { $$ = cmp(@$, $1); }
  | NOT simple                      { $$ = not(@$, $2); }

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
    atom
  | "-" arith          %prec UNARY  { $$ = unary(@$, ident(@1, "-"), $2); }
  | arith "+" arith                 { $$ = binary(@$, $1, ident(@2, "+"), $3); }
  | arith "-" arith                 { $$ = binary(@$, $1, ident(@2, "-"), $3); }
  | arith "*" arith                 { $$ = binary(@$, $1, ident(@2, "*"), $3); }

atom:
    term
  | "(" exprs2 ")"                            { $$ = tuple(@$, $2); }
  | "(" line_statement ")"                    { $$ = seq(@$, list($2)); }
  | "(" block_statement ")"                   { $$ = seq(@$, list($2)); }
  | "(" expr ";" statements0 ")"              { $$ = seq(@$, list($2, $4)); }
  | "(" line_statement ";" statements0 ")"    { $$ = seq(@$, list($2, $4)); }
  | "(" block_statement ";" statements0 ")"   { $$ = seq(@$, list($2, $4)); }
  | atom "." IDENT                            { $$ = attr(@$, $1, $3); }
  | atom "." "_"                              { $$ = attr(@$, $1, "_"); }
  | atom "(" args0 ")"                        { $$ = call(@$, $1, $3); }
  | atom "[" exprs0 "]"                       { $$ = item(@$, $1, $3); }

term:
    IDENT                           { $$ = ident(@$, $1); }
  | "_"                             { $$ = ident(@$, "_"); }
  | INTEGER                         { $$ = integer(@$, $1); }
  | TRUE                            { $$ = bool(@$, true); }
  | FALSE                           { $$ = bool(@$, false); }
  | STRING                          { $$ = str(@$, $1); }
  | ATOM                            { $$ = atom(@$, $1); }
  | NONE                            { $$ = none(@$); }
  | ERROR                           { $$ = error(@$, "invalid token: " + $1); }
  | THIS                            { $$ = this_(@$); }
  | "(" expr ")"                    { $$ = paren(@$, $2); }
  | "[" args0 "]"                   { $$ = vector(@$, $2); }
  | "{" args0 "}"                   { $$ = record(@$, $2); }
  | "!" "[" exprs0 "]"              { $$ = list(@$, $3); }
  | "!" "{" args0 "}"               { $$ = dict(@$, $3); }

exprs0:
    %empty                          { $$ = list(); }
  | exprs1
  | exprs1 ","

exprs1:
    expr                            { $$ = list($1); }
  | exprs1 "," expr                 { $$ = list($1, $3); }

exprs2:
    expr "," exprs1                 { $$ = list($1, $3); }
  | expr "," exprs1 ","             { $$ = list($1, $3); }

arg:
    expr
  | "..." expr                      { $$ = spread(@$, $2); }
  | expr "=" expr                   { $$ = arg(@$, $1, $3); }
  | expr error                      { $$ = error(@$, "arg"); }
  | error                           { $$ = error(@$, "arg"); }

args0:
    %empty                          { $$ = list(); }
  | args1
  | args1 ","

args1:
    arg                             { $$ = list($1); }
  | args1 "," arg                   { $$ = list($1, $3); }
