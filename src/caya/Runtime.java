package caya;

import java.util.HashMap;

public final class Runtime {
  public static final class NotImplemented extends RuntimeException {}

  public static Value null_to_none(Value value) { return value == null ? Builtins.NONE : value; }

  public static abstract class Value {
    public Value call(Value[] args) { throw new NotImplemented(); }
    public Value get_attr(String field) { throw new NotImplemented(); }
    public void set_attr(String field, Value value) { throw new NotImplemented(); }
    public Value get_item(Value item) { throw new NotImplemented(); }
    public void set_item(Value item, Value value) { throw new NotImplemented(); }
  }

  public static abstract class BuiltinValue extends Value {
    public abstract HashMap<String, Descriptor> fields();

    public Value get_attr(String field) { return fields().get(field).get(this); }

    public static HashMap<String, Descriptor> resolve_fields(Class<?> cls, String[] fields, String[] methods) {
      var descriptors = new HashMap<String, Descriptor>();
      for(var field : fields) {
        descriptors.put(field, new BuiltinProperty(get_method(cls, field)));
      }
      for(var method : methods) {
        descriptors.put(method, new BuiltinMethod(find_unique(cls, method)));
      }
      return descriptors;
    }

    private static java.lang.reflect.Method find_unique(Class<?> cls, String name) {
      java.lang.reflect.Method found = null;
      for(var m : cls.getMethods()) {
        if(m.getName().equals(name)) {
          if(found != null) {
            throw new RuntimeException("duplicated builting \"" + name + "\"!");
          }
          found = m;
        }
      }
      if(found == null) {
        throw new RuntimeException("no builtin \"" + name + "\"");
      }
      return found;
    }

    private static java.lang.reflect.Method get_method(Class<?> cls, String name, Class<?>... param_types) {
      try {
        return cls.getMethod(name, param_types);
      } catch (SecurityException | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static final class BoundMethod extends Value {
    public final Value obj;
    public final Descriptor method;
    public BoundMethod(Value obj, Descriptor method) { this.obj = obj; this.method = method; }

    public Value call(Value[] args) { return method.call(obj, args); }
  }

  public static final class Function extends Value {
    public final String[] params;
    public final Interpreter.Scope closure;
    public final Node body;
    public Function(String[] params, Interpreter.Scope closure, Node body) { this.params = params; this.closure = closure; this.body = body; }
    public Value call(Value[] args) {
      if(params.length != args.length) {
        throw new Interpreter.InterpreterError("expected " + params.length + " arguments, got " + args.length);
      }
      var scope = new Interpreter.Scope(this.closure);
      for(int i = 0; i < params.length; i++) {
        scope.set(params[i], args[i]);
      }
      return scope.eval(this.body);
    }
  }

  public static interface Descriptor {
    public Value call(Value obj, Value[] args);
    public Value get(Value obj);
    public default Value set(Value obj, Value value) { throw new NotImplemented(); }
  }

  record BuiltinProperty(java.lang.reflect.Method m) implements Descriptor {
    public Value get(Value obj) {
      try {
        return null_to_none((Value) m.invoke(obj));
      } catch (Throwable e) { throw new RuntimeException(e); }
    }
    public Value call(Value obj, Value[] args) { return ((Value) get(obj)).call(args); }
  }

  record BuiltinMethod(java.lang.reflect.Method m) implements Descriptor {
    public Value get(Value obj) { return new BoundMethod(obj, this); }
    public Value call(Value obj, Value[] args) {
      try {
        return null_to_none((Value) m.invoke(obj, prepare_arguments(m, args)));
      } catch (Throwable e) { throw new RuntimeException(e); }
    }

    private static Object[] prepare_arguments(java.lang.reflect.Executable e, Object[] args) {
      if (e.isVarArgs()) {
        var varargs_type = e.getParameterTypes()[e.getParameterCount() - 1];
        var length = args.length - e.getParameterCount() + 1;
        var varargs = java.lang.reflect.Array.newInstance(varargs_type.componentType(), length);
        System.arraycopy(args, e.getParameterCount() - 1, varargs, 0, length);
        args = java.util.Arrays.copyOf(args, e.getParameterCount(), Object[].class);
        args[args.length - 1] = varargs;
      }
      return args;
    }
  }
}
