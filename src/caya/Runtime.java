package caya;

import java.util.HashMap;
import java.util.Map;
import com.dynatrace.hash4j.hashing.Hashing;


public final class Runtime {
  public interface Callable {
    public Value call(Value[] args, Map<String, Value> named_args);
  }

  public static abstract class Value implements Callable {
    @Override public Value call(Value[] args, Map<String, Value> named_args) { throw new Interpreter.InterpreterError("object of type `" + this.getClass() + "` cannot be called"); }
    public Value get_attr(String attr) { throw new Interpreter.AttrError(this.getClass(), attr); }
    public void set_attr(String attr, Value value) { throw new Interpreter.AttrError(this.getClass(), attr); }
    public Value get_item(Value item) { throw new Interpreter.InterpreterError("object of type `" + this.getClass() + "` cannot be subscripted"); }
    public void set_item(Value item, Value value) { throw new Interpreter.InterpreterError("object of type `" + this.getClass() + "` cannot be subscripted"); }
    public abstract Type type();
  }

  public static abstract class Type extends Value {
    public static final Builtins.Type TYPE = new Builtins.Type("type", null, new HashMap<>(), new HashMap<>());
    @Override final public Builtins.Type type() { return TYPE; }
  }

  public record Param(String name, Node default_value) {}

  public static final class Function extends Value {
    public final Param[] params;
    public final Interpreter.Scope closure;
    public final Node body;
    public Function(Param[] params, Interpreter.Scope closure, Node body) { this.params = params; this.closure = closure; this.body = body; }
    @Override public Value call(Value[] args, Map<String, Value> named_args) { return Interpreter.eval_function(params, closure, body, args, named_args, null); }
    public static final Builtins.Type TYPE = new Builtins.Type("fn", null, new HashMap<>(), new HashMap<>());
    @Override public Builtins.Type type() { return TYPE; }
  }

  public static int combine_hash(int a, int b) {
    return Hashing.murmur3_32().hashStream().putInt(a).putInt(b).getAsInt();
  }

  public static int combine_hash(Object a, Object b) {
    return Hashing.murmur3_32().hashStream().putInt(a.hashCode()).putInt(b.hashCode()).getAsInt();
  }

  public static int combine_hash(Object a, Object b, Object c) {
    return Hashing.murmur3_32().hashStream().putInt(a.hashCode()).putInt(b.hashCode()).putInt(c.hashCode()).getAsInt();
  }

  public static int hash_sequence(Iterable<?> items) {
    var h = Hashing.murmur3_32().hashStream();
    var count = 0;
    for(var item : items) {
      count += 1;
      h.putInt(item.hashCode());
    }
    h.putInt(count);
    return h.getAsInt();
  }

  public static <K, V> int hash_mapping(Iterable<java.util.Map.Entry<K, V>> entries) {
    int hash = 0;
    var count = 0;
    for(var entry : entries) {
      hash += combine_hash(entry.getKey().hashCode(), entry.getValue().hashCode());
      count += 1;
    }
    return combine_hash(hash, count);
  }

  public static java.util.Iterator<Value> iter(Value value) {
    var iter = value.get_attr("iter").call(new Value[0], null);
    return iter instanceof Builtins.Iterator i ? i.it : new java.util.Iterator<Value>() {
      private Value next = null;
      private final Value next_method = iter.get_attr("next");
      private void fetch() { if(next == null) { next = next_method.call(new Value[0], null); } }
      @Override public boolean hasNext() { fetch(); return next != Builtins.STOP; }
      @Override public Value next() {
        fetch();
        if(next == Builtins.STOP) { throw new java.util.NoSuchElementException(); }
        var result = next;
        next = null;
        return result;
      }
    };
  }
}
