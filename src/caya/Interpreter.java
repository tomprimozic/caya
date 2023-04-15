package caya;

import java.util.HashMap;

import caya.Runtime.Value;
import caya.Builtins.*;
import static caya.Builtins.*;

public final class Interpreter {
  public static final class InterpreterError extends RuntimeException {
    public InterpreterError(String msg) { super(msg); }
  }

  public static Int to_int(Value value) {
    if(value instanceof Int i) {
      return i;
    } else {
      throw new InterpreterError("expected int, got " + value);
    }
  }

  public static Bool to_bool(Value value) {
    if(value instanceof Bool b) {
      return b;
    } else {
      throw new InterpreterError("expected bool, got " + value);
    }
  }

  public final HashMap<String, Value> variables = new HashMap<>();

  public Value eval(Node n) {
    Value result = switch(n) {
      case Node.Int(var __, var value) -> new Int(value);
      case Node.Str(var __, var value) -> new Str(value);
      case Node.None(var __) -> NONE;
      case Node.Bool(var __, var value) -> value ? TRUE : FALSE;
      case Node.Ident(var __, var name) -> {
        if(!variables.containsKey(name)) { throw new InterpreterError("variable not found: " + name); }
        yield variables.get(name);
      }
      case Node.Array(var __, var items) -> new List(items.stream().map(this::eval).toArray(size -> new Value[size]));
      case Node.Field(var __, var expr, var field) -> eval(expr).get_attr(field);
      case Node.Call(var __, var fn, var args) -> eval(fn).call(args.stream().map(this::eval).toArray(size -> new Value[size]));
      case Node.Seq(var __, var exprs) -> {
        if(exprs.isEmpty()) yield NONE;
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
      case Node.Unary(var __, Node.Ident(var ___, var op), var expr) when op == "-" -> new Int(to_int(eval(expr)).value.negate());
      case Node.Binary(var __, Node.Ident(var ___, var op), var left, var right) when op == "+" -> new Builtins.Int(to_int(eval(left)).value.add(to_int(eval(right)).value));
      case Node.Binary(var __, Node.Ident(var ___, var op), var left, var right) when op == "-" -> new Builtins.Int(to_int(eval(left)).value.subtract(to_int(eval(right)).value));
      case Node.Binary(var __, Node.Ident(var ___, var op), var left, var right) when op == "*" -> new Builtins.Int(to_int(eval(left)).value.multiply(to_int(eval(right)).value));
      case Node.If(var __, var cond, var then, var else_) -> to_bool(eval(cond)).value ? eval(then) : eval(else_);
      default -> throw new InterpreterError("not implemented");
    };
    return result;
  }
}
