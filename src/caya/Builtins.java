package caya;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.HashMap;

import scala.collection.mutable.ArrayDeque;
import scala.collection.ArrayOps;

import caya.Runtime.Value;

public final class Builtins {

  public final static class Int extends Value {
    public final BigInteger value;
    public Int(BigInteger value) { this.value = value; }
    public Int(Integer value) { this.value = BigInteger.valueOf(value); }
    public String toString() { return value.toString(); }
  }

  public final static class Bool extends Value {
    public final boolean value;
    public Bool(boolean value) { this.value = value; }
    public String toString() { return Boolean.toString(value); }
  }

  public final static class None extends Value {
    public String toString() { return "none"; }
  }

  public final static None NONE = new None();
  public final static Bool TRUE = new Bool(true);
  public final static Bool FALSE = new Bool(false);

  public static Value null_to_none(Value value) { return value == null ? Builtins.NONE : value; }

  public static interface Descriptor {
    public Value call(Value obj, Value[] args);
    public Value get(Value obj);
  }

  record Property(java.lang.reflect.Method m) implements Descriptor {
    public Value get(Value obj) {
      try {
        return null_to_none((Value) m.invoke(obj));
      } catch (Throwable e) { throw new RuntimeException(e); }
    }
    public Value call(Value obj, Value[] args) { return get(obj).call(args); }
  }

  record Method(java.lang.reflect.Method m) implements Descriptor {
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
  public static abstract class BuiltinValue extends Value {
    public abstract HashMap<String, Descriptor> attrs();

    public Value get_attr(String attr) { return attrs().get(attr).get(this); }

    public static HashMap<String, Descriptor> resolve_attrs(Class<?> cls, String[] props, String[] methods) {
      var descriptors = new HashMap<String, Descriptor>();
      for(var prop : props) {
        descriptors.put(prop, new Property(get_method(cls, prop)));
      }
      for(var method : methods) {
        descriptors.put(method, new Method(find_unique(cls, method)));
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
    public final Method method;
    public BoundMethod(Value obj, Method method) { this.obj = obj; this.method = method; }

    public Value call(Value[] args) { return method.call(obj, args); }
  }

  public final static class Str extends BuiltinValue {
    public final String value;
    public Str(String value) { this.value = value; }
    public String toString() { return value; }

    public Int size() { return new Int(value.length()); }
    public Value join(List items) { return new Str(items.data.mkString(this.value)); }

    public final HashMap<String, Descriptor> attrs() { return ATTRS; }
    public static final HashMap<String, Descriptor> ATTRS = BuiltinValue.resolve_attrs(Str.class,
      new String[] {"size"},
      new String[] {"join"}
    );
  }

  public final static class List extends BuiltinValue {
    public final ArrayDeque<Value> data;
    public List() { data = new ArrayDeque<>(4); }
    public List(Value... items) {
      data = new ArrayDeque<>(items.length);
      data.addAll(new ArrayOps.ArrayIterator<>(items));
    }
    public String toString() { return data.mkString("[", ", ", "]"); }

    public void push(Value item) { data.prepend(item); }
    public void append(Value... items) { for(var item : items) { data.append(item); } }
    public Value pop() { return data.removeHead(true); }
    public Value shift() { return data.removeLast(true); }
    public Int size() { return new Int(data.size()); }
    public Value last() { return data.last(); }

    public Value get_item(Value item) { return this.data.apply(Interpreter.to_int(item).value.intValueExact()); }
    public void set_item(Value item, Value value) { data.update(Interpreter.to_int(item).value.intValueExact(), value); }

    public final HashMap<String, Descriptor> attrs() { return ATTRS; }
    public static final HashMap<String, Descriptor> ATTRS = BuiltinValue.resolve_attrs(List.class,
      new String[] {"size", "last"},
      new String[] {"push", "append", "pop", "shift"}
    );
  }

  class Dict extends BuiltinValue {
    public final LinkedHashMap<Value, Value> data;
    public Dict() { data = new LinkedHashMap<>(); }

    public Value get(Value key) { return data.get(key); }
    public Int size() { return new Int(data.size()); }
    public void clear() { data.clear(); }

    public Value get_item(Value key) { return data.get(key); }
    public void set_item(Value key, Value value) { data.put(key, value); }

    public final HashMap<String, Descriptor> attrs() { return ATTRS; }
    public static final HashMap<String, Descriptor> ATTRS = BuiltinValue.resolve_attrs(List.class,
      new String[] {"size"},
      new String[] {"clear"}
    );
  }
}
