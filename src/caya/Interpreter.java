package caya;

import java.util.HashMap;

import caya.Runtime.Value;
import caya.Builtins.*;
import static caya.Builtins.*;

public final class Interpreter {
  public static final class InterpreterError extends RuntimeException {
    public InterpreterError(String msg) { super(msg); }
  }
  public static final class NotImplemented extends RuntimeException {}

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

    public Value lookup(String binding) {
      if(bindings.containsKey(binding)) {
        return bindings.get(binding);
      } else if(parent != null) {
        return parent.lookup(binding);
      } else {
        throw new InterpreterError("binding not found: " + binding);
      }
    }

    private boolean update(String binding, Value value) {
      if(this.bindings.containsKey(binding)) {
        this.bindings.put(binding, value);
        return true;
      } else if(this.parent != null) {
        return this.parent.update(binding, value);
      } else {
        return false;
      }
    }

    public void assign(String binding, Value value) {
      if(this.bindings.containsKey(binding) || this.parent == null || !this.parent.update(binding, value)) {
        this.bindings.put(binding, value);
      }
    }

    public void declare(String binding, Value value) {
      if(this.bindings.containsKey(binding)) { throw new InterpreterError("duplicate var " + binding); }
      this.bindings.put(binding, value);
    }

    public Value eval(Node n) {
      Value result = switch(n) {
        case Node.Int(var __, var value) -> new Int(value);
        case Node.Str(var __, var value) -> new Str(value);
        case Node.None(var __) -> NONE;
        case Node.Bool(var __, var value) -> value ? TRUE : FALSE;
        case Node.Ident(var __, var name) -> lookup(name);
        case Node.Array(var __, var items) -> new List(items.stream().map(this::eval).toArray(size -> new Value[size]));
        case Node.Attr(var __, var expr, var attr) -> eval(expr).get_attr(attr);
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
        case Node.Assign(var __, Node.Ident(var ___, var name), var expr) -> { assign(name, eval(expr)); yield NONE; }
        case Node.VarAssign(var __, Node.Ident(var ___, var name), var expr) -> { declare(name, eval(expr)); yield NONE; }
        case Node.Unary(var __, Node.Ident(var ___, var op), var expr) when op == "-" -> new Int(to_int(eval(expr)).value.negate());
        case Node.Binary(var __, Node.Ident(var ___, var op), var left, var right) when op == "+" -> new Int(to_int(eval(left)).value.add(to_int(eval(right)).value));
        case Node.Binary(var __, Node.Ident(var ___, var op), var left, var right) when op == "-" -> new Int(to_int(eval(left)).value.subtract(to_int(eval(right)).value));
        case Node.Binary(var __, Node.Ident(var ___, var op), var left, var right) when op == "*" -> new Int(to_int(eval(left)).value.multiply(to_int(eval(right)).value));
        case Node.If(var __, var cond, var then) -> to_bool(eval(cond)).value ? eval(then) : NONE;
        case Node.IfElse(var __, var cond, var then, var else_) -> to_bool(eval(cond)).value ? eval(then) : eval(else_);
        case Node.Assign(var __, Node.Call(var ___, Node.Ident(var ____, String fn), var params), var body) -> {
          var param_names = new String[params.size()];
          for(int i = 0; i < params.size(); i++) {
            if(params.get(i) instanceof Node.Ident(var _____, var name)) {
              param_names[i] = name;
            } else {
              throw new InterpreterError("parameters must be identifiers");
            }
          }
          assign(fn, new Runtime.Function(param_names, this, body));
          yield NONE;
        }
        case Node.Item(var __, var value, var items) when items.size() == 1 -> eval(value).get_item(eval(items.get(0)));
        case Node.Cmp(var __, var items) -> {
          assert items.size() >= 3 && items.size() % 2 == 1;
          var left = to_int(eval(items.get(0)));
          for(int i = 1; i < items.size(); i += 2) {
            var op = switch(items.get(i)) {
              case Node.Ident(var ___, var name) -> name;
              default -> { assert false; yield null; }
            };
            var right = to_int(eval(items.get(i + 1)));
            var cmp = left.value.compareTo(right.value);
            var cmp_result = switch(op) {
              case "<" -> cmp < 0;
              case ">" -> cmp > 0;
              case "<=" -> cmp <= 0;
              case ">=" -> cmp >= 0;
              case "!=" -> cmp != 0;
              case "==" -> cmp == 0;
              default -> throw new InterpreterError("unexpected comparison operator: " + op);
            };
            if(!cmp_result) {
              yield FALSE;
            }
            left = right;
          }
          yield TRUE;
        }
        case Node.While(var __, var cond, var body) -> {
          while(to_bool(eval(cond)).value) { eval(body); }
          yield NONE;
        }
        default -> throw new NotImplemented();
      };
      return result;
    }
  }

  public static Value eval(Node n) { return new Scope().eval(n); }
}
