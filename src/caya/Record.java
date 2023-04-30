package caya;

import scala.collection.immutable.HashMap;
import scala.collection.Map;
import scala.collection.JavaConverters;

import java.util.NoSuchElementException;

import caya.Runtime.Value;

public final class Record extends Value {
  public final HashMap<String, Value> fields;
  public Record() {
    this.fields = new HashMap<>();
  }
  public Record(HashMap<String, Value> fields) { this.fields = fields; }
  public Record(Map<String, Value> fields) { this.fields = HashMap.from(fields); }

  public Value get_attr(String attr) {
    try {
      return fields.apply(attr);
    } catch (NoSuchElementException e) {
      throw new Interpreter.AttrError(getClass(), attr);
    }
  }

  public String toString() {
    var s = new StringBuilder("{");
    var first = true;
    for(var entry : JavaConverters.asJava(fields).entrySet()) {
      if(first) {
        first = false;
      } else {
        s.append(", ");
      }
      s.append(entry.getKey());
      s.append('=');
      s.append(entry.getValue());
    }
    s.append('}');
    return s.toString();
  }

  @Override public int hashCode() { return Runtime.combine_hash(Record.class.hashCode(), Runtime.hash_mapping(JavaConverters.asJava(fields).entrySet())); }
}
