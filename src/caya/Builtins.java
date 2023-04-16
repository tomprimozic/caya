package caya;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.HashMap;

import scala.collection.mutable.ArrayDeque;
import scala.collection.ArrayOps;

import caya.Runtime.Value;
import caya.Runtime.BuiltinValue;
import caya.Runtime.Descriptor;

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

    public Value get_item(Value item) { return this.data.apply(Interpreter.to_int(item).value.intValueExact()); }
    public void set_item(Value item, Value value) { data.update(Interpreter.to_int(item).value.intValueExact(), value); }

    public final HashMap<String, Descriptor> attrs() { return ATTRS; }
    public static final HashMap<String, Descriptor> ATTRS = BuiltinValue.resolve_attrs(List.class,
      new String[] {"size"},
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
