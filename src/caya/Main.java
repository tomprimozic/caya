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
      case 0 -> {
        var scope = new Interpreter.Scope(Interpreter.root, null, false, false);

        @SuppressWarnings("resource") var input = new java.util.Scanner(System.in);

        while(true) {
          System.out.print("> ");
          System.out.flush();
          if(!input.hasNextLine()) { break; }
          var code = input.nextLine();
          try {
            var result = scope.eval(ParserHelper.parse(code));
            if(result != Builtins.NONE) { System.out.println(result); }
          } catch (Interpreter.Control.Exception e) {
            System.out.println("exception: " + e.value);
          } catch (Parser.ParserError e) {
            System.out.println("parsing error: " + e);
          } catch (Interpreter.InterpreterError e) {
            System.out.println("interpreter error: " + e);
          }
        }
      }
      default -> { throw new RuntimeException("usage: caya [<path>]"); }
    }
  }
}