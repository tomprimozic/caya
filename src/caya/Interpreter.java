package caya;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.math.BigInteger;
import java.util.ArrayList;

import caya.Runtime.Value;
import caya.Runtime.Param;
import caya.Builtins.*;
import static caya.Builtins.*;

public final class Interpreter {
  public static class InterpreterError extends RuntimeException {
    public InterpreterError(String msg) { super(msg); }
  }
  public static final class NotImplemented extends InterpreterError {
    public NotImplemented(String reason) { super(reason); }
  }
  public static final class AttrError extends InterpreterError {
    public final Class<?> cls;
    public final String attr;
    public AttrError(Class<?> cls, String attr) { super("object of type `" + cls + "` has no attribute `" + attr + "`"); this.cls = cls; this.attr = attr; }
  }

  public static abstract class Control extends RuntimeException {
    public static final class Return extends Control {
      Value value;
      public Return(Value value) { this.value = value; }
    }

    public static final class Exception extends Control {
      Value value;
      public Exception(Value value) { this.value = value; }
    }

    public static final class Break extends Control {}
    public static final class Continue extends Control {}
  }

  public static BigInteger to_int(Value value) {
    if(value instanceof Int i) {
      return i.value;
    } else {
      throw new InterpreterError("expected int, got " + value);
    }
  }

  public static int to_int32(Value value) {
    return to_int(value).intValueExact();
  }

  public static long to_int64(Value value) {
    return to_int(value).longValueExact();
  }

  public static boolean to_bool(Value value) {
    if(value instanceof Bool b) {
      return b.value;
    } else {
      throw new InterpreterError("expected bool, got " + value);
    }
  }

  public static Value eval_function(Param[] params, Scope closure, Node body, Value[] args, Map<String, Value> named_args, Obj this_obj) {
    var scope = new Interpreter.Scope(closure, this_obj, false, true);
    if(named_args != null) {
      next_arg:
      for(var arg : named_args.entrySet()) {
        for(var param : params) {
          if(param.name().equals(arg.getKey())) {
            scope.declare(param.name(), arg.getValue());
            continue next_arg;
          }
        }
        throw new Interpreter.InterpreterError("invalid named argument `" + arg.getKey() + "`");
      }
    }
    var i = 0;
    for(var param : params) {
      if(named_args != null && named_args.containsKey(param.name())) {
        continue;
      } else if(i < args.length) {
        scope.declare(param.name(), args[i]);
        i += 1;
      } else if(param.default_value() != null) {
        scope.declare(param.name(), scope.eval(param.default_value()));
      } else {
        throw new Interpreter.InterpreterError("missing parameter ` " + param.name() + "`");
      }
    }
    try {
      return scope.eval(body);
    } catch (Control.Return e) {
      return e.value;
    }
  }

  public static final Scope root = new Scope(null, null, false, false);
  static {
    root.assign("math", new Builtins.Module("math", new HashMap<>(Map.of(
      "sign", new Builtins.Function("math_sign")
    ))));
    root.assign("iter", new Builtins.Module("iter", new HashMap<>(Map.of(
      "stop", Builtins.STOP
    ))));
    root.assign("http", new Builtins.Module("http", new HashMap<>(Map.of(
      "get", new Builtins.Function("http_get")
    ))));
    root.assign("jvm", new Builtins.Module("jvm", new HashMap<>(Map.of(
      "cls", new Builtins.Function("jvm_cls")
    ))));
    root.assign("load", new Builtins.Function("load"));
    root.assign("list", Builtins.List.TYPE);
    root.assign("vector", Vector.TYPE);
  }

  public static final class Scope {
    public final HashMap<String, Value> bindings = new HashMap<>();
    private final Scope parent;
    private final Obj this_obj;
    private final boolean in_loop;
    private final boolean in_fn;
    public Scope(Scope parent, Obj this_obj, boolean in_loop, boolean in_fn) { this.parent = parent; this.this_obj = this_obj; this.in_loop = in_loop; this.in_fn = in_fn; }

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

    private Param[] get_params(java.util.List<Node> params) {
      var parameters = new Param[params.size()];
      var names = new HashSet<String>();
      var i = 0;
      for(var param : params) {
        switch(param) {
          case Node.Arg(var __, Node.Ident(var ___, var name), var default_value) -> {
            if(names.contains(name)) {
              throw new InterpreterError("duplicated parameter name `" + name + "`");
            }
            names.add(name);
            parameters[i] = new Param(name, default_value);
          }
          case Node.Ident(var __, var name) -> {
            if(names.contains(name)) {
              throw new InterpreterError("duplicated parameter name `" + name + "`");
            }
            names.add(name);
            parameters[i] = new Param(name, null);
          }
          default -> throw new InterpreterError("invalid parameter: " + Node.show(param));
        }
        i += 1;   // if we change this to a for(;;) loop, Java thinks return is unreachable
      }
      return parameters;
    }

    public Map<Value, Value> eval_map(Iterable<Node> items) {
      var entries = new HashMap<Value, Value>();
      for(var item : items) {
        switch(item) {
          case Node.Arg(var __, var key, var value) -> entries.put(eval(key), eval(value));
          default -> throw new InterpreterError("expected a `key = value` entry, not " + Node.show(item));
        }
      }
      return entries;
    }

    public Value eval(Node n) {
      Value result = switch(n) {
        case Node.Int(var __, var value) -> new Int(value);
        case Node.Str(var __, var value) -> new Str(value);
        case Node.None(var __) -> NONE;
        case Node.Bool(var __, var value) -> value ? TRUE : FALSE;
        case Node.Atom(var __, var name) -> new Atom(name);
        case Node.Ident(var __, var name) -> lookup(name);
        case Node.List(var __, var items) -> new List(items.stream().map(this::eval).toArray(size -> new Value[size]));
        case Node.Vector(var __, var items) -> {
          if(items.size() == 0) { yield Vector.empty; }   // TODO: how to specify an empty index?
          yield switch(items.get(0)) {
            case Node.Arg(var ___, var key, var value) -> // index
              new Index(eval_map(items));
            default -> // vector
              Vector.make(items.stream().map(this::eval));
          };
        }
        case Node.Tuple(var __, var items) -> Vector.make(items.stream().map(this::eval));
        case Node.Dict(var __, var fields) -> new Dict(eval_map(fields));
        case Node.Record(var __, var fields) -> {
          var seen_fields = new HashSet<String>();
          var record = new scala.collection.mutable.HashMap<String, Value>();
          for(var field : fields) {
            switch(field) {
              case Node.Arg(var ___, Node.Ident(var ____, var name), var expr) -> {
                if(seen_fields.contains(name)) {
                  throw new InterpreterError("duplicated record field `" + name + "`");
                }
                seen_fields.add(name);
                record.put(name, eval(expr));
              }
              case Node.Ident(var ___, var name) -> {
                if(seen_fields.contains(name)) {
                  throw new InterpreterError("duplicated record field `" + name + "`");
                }
                // treat as variable name
                seen_fields.add(name);
                record.put(name, eval(field));
              }
              case Node.Spread(var ___, var expr) -> {
                var value = eval(expr);
                if(value instanceof Record r) {
                  record.addAll(r.fields);
                } else {
                  throw new InterpreterError("expected a record, not `" + value.getClass() + "`");
                }
              }
              default -> throw new NotImplemented(Node.show(field));
            }
          }
          yield new Record(record);
        }
        case Node.Attr(var __, var expr, var attr) -> eval(expr).get_attr(attr);
        case Node.Call(var __, var fn, var args) -> {
          var named_args = new HashMap<String, Value>();
          var other_args = new ArrayList<Value>();
          var fn_value = eval(fn);
          for(var arg : args) {
            switch(arg) {
              case Node.Arg(var ___, Node.Ident(var ____, var name), var expr) -> {
                if(named_args.containsKey(name)) {
                  throw new InterpreterError("duplicated named argument `" + name + "`");
                }
                named_args.put(name, eval(expr));
              }
              default -> { other_args.add(eval(arg)); }
            }
          }
          yield fn_value.call(other_args.toArray(size -> new Value[size]), named_args.isEmpty() ? null : named_args);
        }
        case Node.Seq(var __, var exprs) -> eval_seq(exprs, this.in_loop);
        case Node.Assign(var __, Node.Ident(var ___, var name), var expr) -> { assign(name, eval(expr)); yield NONE; }
        case Node.Assign(var __, Node.Attr(var ___, var obj, var attr), var expr) -> { eval(obj).set_attr(attr, eval(expr)); yield NONE; }
        case Node.VarAssign(var __, Node.Ident(var ___, var name), var expr) -> { declare(name, eval(expr)); yield NONE; }
        case Node.Unary(var __, Node.Ident(var ___, var op), var expr) when op == "-" -> new Int(to_int(eval(expr)).negate());
        case Node.Binary(var __, Node.Ident(var ___, var op), var left, var right) when op == "+" -> new Int(to_int(eval(left)).add(to_int(eval(right))));
        case Node.Binary(var __, Node.Ident(var ___, var op), var left, var right) when op == "-" -> new Int(to_int(eval(left)).subtract(to_int(eval(right))));
        case Node.Binary(var __, Node.Ident(var ___, var op), var left, var right) when op == "*" -> new Int(to_int(eval(left)).multiply(to_int(eval(right))));
        case Node.Not(var __, var expr) -> to_bool(eval(expr)) ? FALSE : TRUE;
        case Node.And(var __, var exprs) -> {
          assert exprs.size() >= 2;
          for(var expr : exprs) {
            if(!to_bool(eval(expr))) {
              yield FALSE;
            }
          }
          yield TRUE;
        }
        case Node.Or(var __, var exprs) -> {
          assert exprs.size() >= 2;
          for(var expr : exprs) {
            if(to_bool(eval(expr))) {
              yield TRUE;
            }
          }
          yield FALSE;
        }
        case Node.If(var __, var cond, var then) -> to_bool(eval(cond)) ? eval(then) : NONE;
        case Node.IfElse(var __, var cond, var then, var else_) -> to_bool(eval(cond)) ? eval(then) : eval(else_);
        case Node.Assign(var __, Node.Call(var ___, Node.Ident(var ____, String fn), var params), var body) -> {
          assign(fn, new Runtime.Function(get_params(params), this, body));
          yield NONE;
        }
        case Node.Arrow(var __, var params, var body) -> new Runtime.Function(get_params(params), this, body);
        case Node.Func(var __, Node.Call(var ___, Node.Ident(var ____, var fn_name), var params), var body) -> {
          assign(fn_name, new Runtime.Function(get_params(params), this, body));
          yield NONE;
        }
        case Node.Item(var __, var value, var items) when items.size() == 1 -> eval(value).get_item(eval(items.get(0)));
        case Node.Cmp(var __, var items) -> eval_cmp(items) ? TRUE : FALSE;
        case Node.While(var __, var cond, var body) -> {
          eval_while(cond, body);
          yield NONE;
        }
        case Node.For(var __, Node.Ident(var ___, var item), var items, var body) -> {
          eval_for(item, items, body);
          yield NONE;
        }
        case Node.Continue(var __) -> {
          if(!in_loop) { throw new InterpreterError("`continue` not in loop"); }
          throw new Control.Continue();
        }
        case Node.Break(var __) -> {
          if(!in_loop) { throw new InterpreterError("`break` not in loop"); }
          throw new Control.Break();
        }
        case Node.Throw(var __, var exception) -> { throw new Control.Exception(eval(exception)); }
        case Node.Try(var __, var try_block, Node.Ident(var ___, var exception), var catch_block) -> {
          yield eval_try(try_block, exception, catch_block);
        }
        case Node.This(var __) -> {
          if(this_obj == null) { throw new InterpreterError("invalid `this` outside of class"); }
          yield this_obj;
        }
        case Node.Class(var __, Node.Ident(var ___, var name), Node.Seq(var ____, var declarations)) -> {
          declare(name, class_declaration(name, declarations));
          yield NONE;
        }
        case Node.Return(var __, var expr) -> {
          if(!in_fn) { throw new InterpreterError("`return` not in function"); }
          throw new Control.Return(expr != null ? eval(expr) : NONE);
        }
        case Node.Print(var __, var expr) -> { System.out.println(eval(expr)); yield NONE; }
        default -> throw new NotImplemented(Node.show(n));
      };
      return result;
    }

    public Value eval_all(java.util.List<Node> exprs) {
      if(exprs.isEmpty()) return NONE;
      var it = exprs.iterator();
      while(true) {
        var expr = it.next();
        var value = eval(expr);
        if(!it.hasNext()) {
          return value;
        }
      }
    }

    public Value eval_seq(java.util.List<Node> exprs, boolean in_loop) {
      return (new Scope(this, this_obj, in_loop, this.in_fn)).eval_all(exprs);
    }

    private Obj.Cls class_declaration(String name, java.util.List<Node> declarations) {
      var fields = new HashMap<String, Integer>();
      var constructor_statements = new ArrayList<Node>();
      HashMap<String, Obj.Descriptor> attrs = new HashMap<String, Obj.Descriptor>();
      for(var declaration : declarations) {
        switch(declaration) {
          case Node.VarAssign(var _____, Node.Ident(var ______, var field_name), var value) -> {
            var field = fields.size();
            fields.put(field_name, field);
            if(attrs.containsKey(field_name)) {
              throw new InterpreterError("duplicated attribute: " + field_name);
            }
            attrs.put(field_name, new Obj.Field(field));
            constructor_statements.add(new Node.Assign(null, new Node.Attr(null, new Node.This(null), field_name), value));
          }
          case Node.Func(var _____, Node.Attr(var ______, Node.This(var _______), var attr), var body) -> {
            var getter = new Obj.Method(new Param[0], this, body);
            switch (attrs.get(attr)) {
              case null -> attrs.put(attr, new Obj.Property(getter, null));
              case Obj.Property(var existing_getter, var setter) -> {
                if(existing_getter != null) { throw new InterpreterError("duplicated property getter: " + attr); }
                attrs.put(attr, new Obj.Property(getter, setter));
              }
              default -> throw new InterpreterError("duplicated attribute: " + attr);
            }
          }
          case Node.Func(var _____, Node.Arg(var ______, Node.Attr(var _______, Node.This(var ________), var attr), Node.Ident(var _________, var param)), var body) -> {
            var setter = new Obj.Method(new Param[] {new Param(param, null)}, this, body);
            switch (attrs.get(attr)) {
              case null -> attrs.put(attr, new Obj.Property(null, setter));
              case Obj.Property(var getter, var existing_setter) -> {
                if(existing_setter != null) { throw new InterpreterError("duplicated property setter: " + attr); }
                attrs.put(attr, new Obj.Property(getter, setter));
              }
              default -> throw new InterpreterError("duplicated attribute: " + attr);
            }
          }
          case Node.Func(var _____, Node.Call(var ______, Node.Ident(var _______, var method_name), var params), var body) -> {
            attrs.put(method_name, new Obj.Method(get_params(params), this, body));
          }
          default -> throw new NotImplemented(Node.show(declaration));
        }
      }
      Obj.Method constructor = new Obj.Method(new Param[0], this, new Node.Seq(null, constructor_statements));
      return new Obj.Cls(name, fields.size(), constructor, attrs);
    }

    private boolean eval_cmp(java.util.List<Node> items) {
      assert items.size() >= 3 && items.size() % 2 == 1;
      var left = to_int(eval(items.get(0)));
      for(int i = 1; i < items.size(); i += 2) {
        var op = switch(items.get(i)) {
          case Node.Ident(var ___, var name) -> name;
          default -> { assert false; yield null; }
        };
        var right = to_int(eval(items.get(i + 1)));
        var cmp = left.compareTo(right);
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
          return false;
        }
        left = right;
      }
      return true;
    }

    private void eval_while(Node cond, Node.Seq body) {
      while(to_bool(eval(cond))) {
        try { eval_seq(body.exprs(), true); }
        catch(Control.Break e) { break; }
        catch(Control.Continue e) { continue; }
      }
    }

    private void eval_for(String item, Node items, Node.Seq body) {
      var it = Runtime.iter(eval(items));
      while(it.hasNext()) {
        var scope = new Scope(this, this_obj, true, in_fn);
        scope.declare(item, it.next());
        try { scope.eval(body); }
        catch(Control.Break e) { break; }
        catch(Control.Continue e) { continue; }
      }
    }

    private Value eval_try(Node.Seq try_block, String exception, Node.Seq catch_block) {
      try { return eval(try_block); }
      catch(Control.Exception e) {
        var scope = new Scope(this, this_obj, in_loop, in_fn);
        scope.declare(exception, e.value);
        return scope.eval(catch_block);
      }
    }
  }

  public static Value eval(Node n) { return new Scope(root, null, false, false).eval(n); }
}
