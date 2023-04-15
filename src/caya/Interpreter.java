package caya;

import caya.Runtime.Value;

public class Interpreter {
  public static final class InterpreterError extends RuntimeException {
    public InterpreterError(String msg) { super(msg); }
  }

  public Value eval(Node n) {
    Value result = switch(n) {
      case Node.Int(var loc, var value) -> new Builtins.Int(value);
      case Node.Str(var loc, var value) -> new Builtins.Str(value);
      case Node.None(var loc) -> Builtins.NONE;
      case Node.Bool(var loc, var value) -> value ? Builtins.TRUE : Builtins.FALSE;
      case Node.Array(var loc, var items) -> new Builtins.List(items.stream().map(this::eval).toArray(size -> new Value[size]));
      case Node.Field(var loc, var expr, var field) -> eval(expr).get_attr(field);
      case Node.Call(var loc, var fn, var args) -> eval(fn).call(args.stream().map(this::eval).toArray(size -> new Value[size]));
      default -> throw new InterpreterError("not implemented");
    };
    return result;
  }
}
