package caya;

import java.util.HashMap;

import caya.Runtime.Value;

public final class Interpreter {
  public static final class InterpreterError extends RuntimeException {
    public InterpreterError(String msg) { super(msg); }
  }

  public final HashMap<String, Value> variables = new HashMap<>();

  public Value eval(Node n) {
    Value result = switch(n) {
      case Node.Int(var __, var value) -> new Builtins.Int(value);
      case Node.Str(var __, var value) -> new Builtins.Str(value);
      case Node.None(var __) -> Builtins.NONE;
      case Node.Bool(var __, var value) -> value ? Builtins.TRUE : Builtins.FALSE;
      case Node.Ident(var __, var name) -> {
        if(!variables.containsKey(name)) { throw new InterpreterError("variable not found: " + name); }
        yield variables.get(name);
      }
      case Node.Array(var __, var items) -> new Builtins.List(items.stream().map(this::eval).toArray(size -> new Value[size]));
      case Node.Field(var __, var expr, var field) -> eval(expr).get_attr(field);
      case Node.Call(var __, var fn, var args) -> eval(fn).call(args.stream().map(this::eval).toArray(size -> new Value[size]));
      case Node.Seq(var __, var exprs) -> {
        if(exprs.isEmpty()) yield Builtins.NONE;
        var it = exprs.iterator();
        while(true) {
          var expr = it.next();
          var value = eval(expr);
          if(!it.hasNext()) {
            yield value;
          }
        }
      }
      case Node.Assign(var __, Node.Ident(var ___, var name), var expr) -> variables.put(name, eval(expr));
      default -> throw new InterpreterError("not implemented");
    };
    return result;
  }
}
