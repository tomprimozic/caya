package caya;

import java.util.HashMap;

import caya.Runtime.Value;

public final class Obj extends Value {
  public final Cls cls;
  public final Value[] fields;
  public Obj(Cls cls) { this.cls = cls; this.fields = new Value[cls.num_fields]; }

  public Value get_attr(String attr) { return cls.get_obj_attr(this, attr); }
  public void set_attr(String attr, Value value) { cls.set_obj_attr(this, attr, value); }

  public static final class Cls extends Value {
    public final String name;
    public final int num_fields;
    public final HashMap<String, Descriptor> attrs;
    public final Method constructor;

    public Cls(String name, int num_fields, Method constructor, HashMap<String, Descriptor> attrs) {
      this.name = name;
      this.num_fields = num_fields;
      this.attrs = attrs;
      this.constructor = constructor;
    }

    public Value call(Value[] args) {
      var obj = new Obj(this);
      constructor.get(obj).call(args);
      return obj;
    }

    private Descriptor get_descriptor(String attr) {
      var descriptor = attrs.get(attr);
      if(descriptor == null) throw new Interpreter.InterpreterError("object of class " + name + " has no attribute " + attr);
      return descriptor;
    }

    public Value get_obj_attr(Obj obj, String attr) { return get_descriptor(attr).get(obj); }
    public void set_obj_attr(Obj obj, String attr, Value value) { get_descriptor(attr).set(obj, value); }
  }

  public static interface Descriptor {
    public Value get(Obj obj);
    public default void set(Obj obj, Value value) { throw new Interpreter.NotImplemented(this.getClass() + ".set"); }
  }

  record Field(int field) implements Descriptor {
    public Value get(Obj obj) { return obj.fields[field]; }
    public void set(Obj obj, Value value) { obj.fields[field] = value; }
  }

  record Method(String[] params, Interpreter.Scope closure, Node body) implements Descriptor {
    public Value get(Obj obj) { return new BoundMethod(obj, this); }
    public Value call(Obj obj, Value[] args) { return Interpreter.eval_function(params, closure, body, args, obj); }
  }

  record Property(Method getter, Method setter) implements Descriptor {
    public Property(Method getter, Method setter) {
      this.getter = getter;
      this.setter = setter;
      assert getter == null || getter.params.length == 0;
      assert setter == null || setter.params.length == 1;
    }
    public Value get(Obj obj) { return getter.call(obj, new Value[0]); }
    public void set(Obj obj, Value value) { setter.call(obj, new Value[] {value}); }
  }

  public final static class BoundMethod extends Value {
    public final Obj obj;
    public final Method method;
    public BoundMethod(Obj obj, Method method) { this.obj = obj; this.method = method; }
    public Value call(Value[] args) { return method.call(obj, args); }
  }
}
