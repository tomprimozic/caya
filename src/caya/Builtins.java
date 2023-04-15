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

  public final static class List extends BuiltinValue {
    public final ArrayDeque<Value> data;
    public List() { data = new ArrayDeque<>(4); }
    public List(Value... items) {
      data = new ArrayDeque<>(items.length);
      data.addAll(new ArrayOps.ArrayIterator<>(items));
    }
    public String toString() { return data.mkString("[", ", ", "]"); }

    public void push(Value item) { data.prepend(item); }
    public void append(Value item) { data.append(item); }
    public Value pop() { return data.removeHead(true); }
    public Value shift() { return data.removeLast(true); }
    public Int size() { return new Int(data.size()); }

    public Value get_item(Value item) { return this.data.apply(((Int) item).value.intValueExact()); }
    public void set_item(Value item, Value value) { data.update(((Int) item).value.intValueExact(), value); }

    public final HashMap<String, Descriptor> fields() { return FIELDS; }
    public static final HashMap<String, Descriptor> FIELDS = BuiltinValue.resolve_fields(List.class,
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

    public final HashMap<String, Descriptor> fields() { return FIELDS; }
    public static final HashMap<String, Descriptor> FIELDS = BuiltinValue.resolve_fields(List.class,
      new String[] {"size"},
      new String[] {"clear"}
    );
  }
}
