package caya;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
  public static void main(String[] args) throws IOException {
    switch(args.length) {
      case 1 -> {
        var code = Files.readString(Path.of(args[0]), java.nio.charset.StandardCharsets.UTF_8);
        var node = ParserHelper.parse(code);
        Interpreter.eval(node);
      }
      case 0 -> { repl(); }
      default -> { throw new RuntimeException("usage: caya [<path>]"); }
    }
  }

  public static void repl() throws IOException {
    var scope = new Interpreter.Scope(Interpreter.root, null, false, false);
    var input = new java.util.Scanner(System.in);

    while(true) {
      System.out.print("> "); System.out.flush();
      if(!input.hasNextLine()) { return; }

      var nodes = repl_input(input);
      if(nodes == null) { continue; }

      for(var node : nodes) {
        try {
          var result = scope.eval(node);
          if(result != Builtins.NONE) { System.out.println(result); }
        } catch (Interpreter.Control.Exception e) {
          System.out.println("exception: " + e.value);
        } catch (Interpreter.InterpreterError e) {
          System.out.println("interpreter error: " + e);
        }
      }
    }
  }

  private static boolean read_another_line(IndentationLexer lexer) {
    if(lexer.states.size() > 1) {
      // unfinished [({, so we expect another line of input
      return true;
    }
    if(lexer.last_token == Parser.Lexer.OUTDENT && lexer.last_newline.equals("\n")) {
      // we are in an indented block, take another line of input to allow the user to continue
      return true;
    }
    if(!lexer.errors.isEmpty()) {
      var last_error = lexer.errors.get(lexer.errors.size() - 1);
      var expected_tokens = java.util.Arrays.asList(last_error.expected());
      // we expect an indented block, i.e. after `if`, `class`, etc.
      return expected_tokens.contains(Parser.SymbolKind.S_INDENT) && !expected_tokens.contains(Parser.SymbolKind.S_OUTDENT);
    }
    return false;
  }

  public static java.util.List<Node> repl_input(java.util.Scanner input) throws IOException {
    String code = "";
    while(true) {
      if(!input.hasNextLine()) { break; }
      code += input.nextLine() + "\n";
      var lexer = new IndentationLexer(new Lexer(new java.io.StringReader(code)));
      var parser = new Parser(lexer);
      parser.parse();

      if(read_another_line(lexer)) {
        System.out.print("| "); System.out.flush();
      } else if(parser.result == null) {
        System.out.println("parsing error: failed to parse");
        return null;
      } else if(!parser.errors.isEmpty()) {
        System.out.println("parsing error: " + parser.errors);
        return null;
      } else {
        return parser.result;
      }
    }
    return null;
  }
}