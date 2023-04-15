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

  public static final class Scope {
    private final HashMap<String, Value> bindings = new HashMap<>();
    private final Scope parent;
    public Scope() { this.parent = null; }
    public Scope(Scope parent) { this.parent = parent; }
    public Value get(String binding) {
      if(bindings.containsKey(binding)) {
        return bindings.get(binding);
      } else if(parent != null) {
        return parent.get(binding);
      } else {
        throw new InterpreterError("binding not found: " + binding);
      }
    }

    private boolean set_existing(String binding, Value value) {
      if(this.bindings.containsKey(binding)) {
        this.bindings.put(binding, value);
        return true;
      } else if(this.parent != null) {
        return this.parent.set_existing(binding, value);
      } else {
        return false;
      }
    }

    public void set(String binding, Value value) {
      if(this.bindings.containsKey(binding) || this.parent == null || !this.parent.set_existing(binding, value)) {
        this.bindings.put(binding, value);
      }
    }

    public Value eval(Node n) {
      Value result = switch(n) {
        case Node.Int(var __, var value) -> new Int(value);
        case Node.Str(var __, var value) -> new Str(value);
        case Node.None(var __) -> NONE;
        case Node.Bool(var __, var value) -> value ? TRUE : FALSE;
        case Node.Ident(var __, var name) -> get(name);
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
        case Node.Assign(var __, Node.Ident(var ___, var name), var expr) -> { set(name, eval(expr)); yield NONE; }
        case Node.Unary(var __, Node.Ident(var ___, var op), var expr) when op == "-" -> new Int(to_int(eval(expr)).value.negate());
        case Node.Binary(var __, Node.Ident(var ___, var op), var left, var right) when op == "+" -> new Int(to_int(eval(left)).value.add(to_int(eval(right)).value));
        case Node.Binary(var __, Node.Ident(var ___, var op), var left, var right) when op == "-" -> new Int(to_int(eval(left)).value.subtract(to_int(eval(right)).value));
        case Node.Binary(var __, Node.Ident(var ___, var op), var left, var right) when op == "*" -> new Int(to_int(eval(left)).value.multiply(to_int(eval(right)).value));
        case Node.If(var __, var cond, var then, var else_) -> to_bool(eval(cond)).value ? eval(then) : eval(else_);
        case Node.Assign(var __, Node.Call(var ___, Node.Ident(var ____, String fn), var params), var body) -> {
          var param_names = new String[params.size()];
          for(int i = 0; i < params.size(); i++) {
            if(params.get(i) instanceof Node.Ident(var _____, var name)) {
              param_names[i] = name;
            } else {
              throw new InterpreterError("parameters must be identifiers");
            }
          }
          this.set(fn, new Runtime.Function(param_names, this, body));
          yield NONE;
        }
        default -> throw new InterpreterError("not implemented");
      };
      return result;
    }
  }

  public static Value eval(Node n) { return new Scope().eval(n); }
}
