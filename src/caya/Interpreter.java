package caya;

import java.util.HashMap;
import java.util.HashSet;
import java.math.BigInteger;
import java.util.ArrayList;

import caya.Runtime.Value;
import caya.Builtins.*;
import static caya.Builtins.*;

public final class Interpreter {
  public static class InterpreterError extends RuntimeException {
    public InterpreterError(String msg) { super(msg); }
  }
  public static final class NotImplemented extends RuntimeException {
    public NotImplemented(String reason) { super(reason); }
  }
  public static final class AttrError extends InterpreterError {
    public final Class<?> cls;
    public final String attr;
    public AttrError(Class<?> cls, String attr) { super("object of type `" + cls + "` has no attribute `" + attr + "`"); this.cls = cls; this.attr = attr; }
  }


  public static final class ReturnException extends RuntimeException {
    Value value;
    public ReturnException(Value value) { this.value = value; }
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

  public static boolean to_bool(Value value) {
    if(value instanceof Bool b) {
      return b.value;
    } else {
      throw new InterpreterError("expected bool, got " + value);
    }
  }

  public static Value eval_function(String[] params, Scope closure, Node body, Value[] args, Obj this_obj) {
    if(params.length != args.length) {
      throw new Interpreter.InterpreterError("expected " + params.length + " arguments, got " + args.length);
    }
    var scope = new Interpreter.Scope(closure, this_obj);
    for(int i = 0; i < params.length; i++) {
      scope.declare(params[i], args[i]);
    }
    try {
      return scope.eval(body);
    } catch (Interpreter.ReturnException e) {
      return e.value;
    }
  }

  public static final Scope root = new Scope();
  static {
    root.assign("sign", new Builtins.Function("sign"));
  }

  public static final class Scope {
    private final HashMap<String, Value> bindings = new HashMap<>();
    private final Scope parent;
    private final Obj this_obj;
    public Scope() { this.parent = null; this.this_obj = null; }
    public Scope(Scope parent, Obj this_obj) { this.parent = parent; this.this_obj = this_obj; }

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

    private String[] get_params(java.util.List<Node> params) {
      var param_names = new String[params.size()];
      for(int i = 0; i < params.size(); i++) {
        if(params.get(i) instanceof Node.Ident(var _____, var name)) {
          param_names[i] = name;
        } else {
          throw new InterpreterError("parameters must be identifiers");
        }
      }
      return param_names;
    }

    public Value eval(Node n) {
      Value result = switch(n) {
        case Node.Int(var __, var value) -> new Int(value);
        case Node.Str(var __, var value) -> new Str(value);
        case Node.None(var __) -> NONE;
        case Node.Bool(var __, var value) -> value ? TRUE : FALSE;
        case Node.Atom(var __, var name) -> new Atom(name);
        case Node.Ident(var __, var name) -> lookup(name);
        case Node.Array(var __, var items) -> new List(items.stream().map(this::eval).toArray(size -> new Value[size]));
        case Node.Tuple(var __, var items) -> new List(items.stream().map(this::eval).toArray(size -> new Value[size]));  // TODO: should be immutable, either Vector or Tuple
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
        case Node.Call(var __, var fn, var args) -> eval(fn).call(args.stream().map(this::eval).toArray(size -> new Value[size]));
        case Node.Seq(var __, var exprs) -> {
          if(exprs.isEmpty()) yield NONE;
          var it = exprs.iterator();
          var scope = new Scope(this, this_obj);
          while(true) {
            var expr = it.next();
            var value = scope.eval(expr);
            if(!it.hasNext()) {
              yield value;
            }
          }
        }
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
          while(to_bool(eval(cond))) { eval(body); }
          yield NONE;
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
          throw new ReturnException(expr != null ? eval(expr) : NONE);
        }
        default -> throw new NotImplemented(Node.show(n));
      };
      return result;
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
            var getter = new Obj.Method(new String[0], this, body);
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
            var setter = new Obj.Method(new String[] {param}, this, body);
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
      Obj.Method constructor = new Obj.Method(new String[0], this, new Node.Seq(null, constructor_statements));
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
  }

  public static Value eval(Node n) { return new Scope(root, null).eval(n); }
}
