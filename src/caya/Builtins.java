package caya;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

import scala.collection.JavaConverters;
import scala.collection.mutable.ArrayDeque;
import scala.collection.ArrayOps;

import caya.Runtime.Value;

public final class Builtins {
  public static final Int math_sign(Int value) {
    return switch(value.value.signum()) {
      case -1 -> new Int(BigInteger.ONE.negate());
      case 0 -> new Int(BigInteger.ZERO);
      case 1 -> new Int(BigInteger.ONE);
      default -> { throw new RuntimeException("impossible"); }
    };
  }

  public final static class Int extends Value {
    public final BigInteger value;
    public Int(BigInteger value) { this.value = value; }
    public Int(int value) { this.value = BigInteger.valueOf(value); }
    public Int(long value) { this.value = BigInteger.valueOf(value); }
    @Override public String toString() { return value.toString(); }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public boolean equals(Object other) { return other instanceof Int i && i.value.equals(this.value); }
  }

  public final static class Bool extends Value {
    public final boolean value;
    private Bool(boolean value) { this.value = value; }
    @Override public String toString() { return value ? "true" : "false"; }
  }

  public final static class None extends Value {
    private None() {}
    @Override public String toString() { return "none"; }
  }

  public final static class Stop extends Value {
    private Stop() {}
    @Override public String toString() { return "iter.stop"; }
  }

  public final static None NONE = new None();
  public final static Bool TRUE = new Bool(true);
  public final static Bool FALSE = new Bool(false);
  public final static Stop STOP = new Stop();

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
    public Value call(Value obj, Value[] args) { return get(obj).call(args, null); }
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

  public static abstract class BuiltinValue extends Value {
    public abstract HashMap<String, Descriptor> attrs();

    @Override public final Value get_attr(String attr) {
      var descriptor = attrs().get(attr);
      if(descriptor == null) {
        throw new Interpreter.InterpreterError("object of class `" + getClass() + "` has no attribute `" + attr + "`");
      }
      return descriptor.get(this);
    }

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
  }

  public static final class BoundMethod extends Value {
    public final Value obj;
    public final Method method;
    public BoundMethod(Value obj, Method method) { this.obj = obj; this.method = method; }
    @Override public Value call(Value[] args, Map<String, Value> named_args) {
      if(named_args != null && !named_args.isEmpty()) {
        throw new Interpreter.InterpreterError("builtin method `" + method.m().getName() + "` cannot be called with named arguments");
      }
      return method.call(obj, args);
    }
    @Override public int hashCode() { return Runtime.combine_hash(BoundMethod.class, obj, method); }
    @Override public boolean equals(Object other) { return other instanceof BoundMethod m && m.obj.equals(this.obj) && m.method.equals(this.method); }
  }

  public static final class Function extends Value {
    public final Method method;
    public Function(Method method) { this.method = method; }
    public Function(String fn) { this(new Method(find_unique(Builtins.class, fn))); }
    @Override public Value call(Value[] args, Map<String, Value> named_args) {
      if(named_args != null && !named_args.isEmpty()) {
        throw new Interpreter.InterpreterError("builtin function `" + method.m().getName() + "` cannot be called with named arguments");
      }
      return method.call(null, args);
    }
    @Override public int hashCode() { return method.hashCode(); }
    @Override public boolean equals(Object other) { return other instanceof Function f && f.method.equals(this.method); }
  }

  public final static class Str extends BuiltinValue {
    public final String value;
    public Str(String value) { this.value = value; }
    @Override public String toString() { return value; }

    public Int size() { return new Int(value.length()); }
    public Value join(Value items) {
      var result = new StringBuilder();
      var first = true;
      var it = Runtime.iter(items);
      while(it.hasNext()) {
        var item = it.next();
        if(first) {
          first = false;
        } else {
          result.append(value);
        }
        result.append(item);
      }
      return new Str(result.toString());
    }

    public final HashMap<String, Descriptor> attrs() { return ATTRS; }
    public static final HashMap<String, Descriptor> ATTRS = BuiltinValue.resolve_attrs(Str.class,
      new String[] {"size"},
      new String[] {"join"}
    );

    @Override public int hashCode() { return value.hashCode(); }
    @Override public boolean equals(Object other) { return other instanceof Str s && s.value.equals(this.value); }
  }

  public final static class Atom extends BuiltinValue {
    public final String name;
    public Atom(String name) { this.name = name; }
    @Override public String toString() { return "`" + name; }

    public Str name() { return new Str(name); }

    public final HashMap<String, Descriptor> attrs() { return ATTRS; }
    public static final HashMap<String, Descriptor> ATTRS = BuiltinValue.resolve_attrs(Atom.class,
      new String[] {"name"},
      new String[] {}
    );

    @Override public int hashCode() { return Runtime.combine_hash(Atom.class, name); }
    @Override public boolean equals(Object other) { return other instanceof Atom a && a.name.equals(this.name); }
  }

  public final static class List extends BuiltinValue {
    public final ArrayDeque<Value> data;
    public List() { data = new ArrayDeque<>(4); }
    public List(Value... items) {
      data = new ArrayDeque<>(items.length);
      data.addAll(new ArrayOps.ArrayIterator<>(items));
    }
    @Override public String toString() { return data.mkString("![", ", ", "]"); }

    public void push(Value item) { data.prepend(item); }
    public void append(Value... items) { for(var item : items) { data.append(item); } }
    public Value pop() { return data.removeHead(true); }
    public Value shift() { return data.removeLast(true); }
    public Int size() { return new Int(data.size()); }
    public Value last() { return data.last(); }
    public Iterator iter() { return new Iterator(JavaConverters.asJava(this.data.iterator())); }

    @Override public Value get_item(Value item) { return this.data.apply(Interpreter.to_int32(item)); }
    @Override public void set_item(Value item, Value value) { data.update(Interpreter.to_int32(item), value); }

    public final HashMap<String, Descriptor> attrs() { return ATTRS; }
    public static final HashMap<String, Descriptor> ATTRS = BuiltinValue.resolve_attrs(List.class,
      new String[] {"size", "last"},
      new String[] {"push", "append", "pop", "shift", "iter"}
    );

    @Override public int hashCode() { throw new Interpreter.InterpreterError("mutable list is not hashable"); }
    @Override public boolean equals(Object other) { return other instanceof List l && l.data.equals(this.data); }
  }

  public final static class Dict extends BuiltinValue {
    public final LinkedHashMap<Value, Value> data;
    public Dict() { data = new LinkedHashMap<>(); }
    public Dict(Map<Value, Value> data) { this.data = new LinkedHashMap<>(data); }

    public Value get(Value key) { return data.get(key); }
    public Int size() { return new Int(data.size()); }
    public void clear() { data.clear(); }

    @Override public Value get_item(Value key) { return data.get(key); }
    @Override public void set_item(Value key, Value value) { data.put(key, value); }

    public final HashMap<String, Descriptor> attrs() { return ATTRS; }
    public static final HashMap<String, Descriptor> ATTRS = BuiltinValue.resolve_attrs(List.class,
      new String[] {"size"},
      new String[] {"clear"}
    );

    @Override public int hashCode() { throw new Interpreter.InterpreterError("mutable dict is not hashable"); }
    @Override public boolean equals(Object other) { return other instanceof Dict d && d.data.equals(this.data); }
  }

  public final static class Index extends BuiltinValue {
    public final scala.collection.immutable.HashMap<Value, Value> data;
    public Index() { data = new scala.collection.immutable.HashMap<>(); }
    public Index(scala.collection.immutable.HashMap<Value, Value> data) { this.data = data; }
    public Index(Map<Value, Value> data) { this.data = scala.collection.immutable.HashMap.from(JavaConverters.asScala(data)); }

    public Value get(Value key) { return data.apply(key); }
    public Index update(Value key, Value value) { return new Index(data.updated(key, value)); }
    public Int size() { return new Int(data.size()); }

    @Override public Value get_item(Value key) { return data.apply(key); }

    public final HashMap<String, Descriptor> attrs() { return ATTRS; }
    public static final HashMap<String, Descriptor> ATTRS = BuiltinValue.resolve_attrs(Index.class,
      new String[] {"size"},
      new String[] {"get", "update"}
    );

    @Override public int hashCode() { return Runtime.hash_mapping(JavaConverters.asJava(this.data).entrySet()); }
    @Override public boolean equals(Object other) { return other instanceof Index d && d.data.equals(this.data); }
  }

  public final static class Module extends Value {
    public final String name;
    public final HashMap<String, Value> attrs;
    public Module(String name, HashMap<String, Value> attrs) {
      this.name = name; this.attrs = attrs;
    }

    public Value get_attr(String attr) {
      var value = attrs.get(attr);
      if(value == null) { throw new Interpreter.AttrError(getClass(), attr); }
      return value;
    }

    public String toString() { return "module " + name; }
  }

  public final static class Iterator extends BuiltinValue {
    public final java.util.Iterator<Value> it;
    public Iterator(java.util.Iterator<Value> it) { this.it = it; }

    public Value next() { return it.hasNext() ? it.next() : STOP; }

    public final HashMap<String, Descriptor> attrs() { return ATTRS; }
    public static final HashMap<String, Descriptor> ATTRS = BuiltinValue.resolve_attrs(Iterator.class,
      new String[] {},
      new String[] {"next"}
    );
  }
}
