package caya;

import scala.collection.immutable.HashMap;
import scala.collection.Map;

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
    var it = fields.iterator();
    while(it.hasNext()) {
      if(first) {
        first = false;
      } else {
        s.append(", ");
      }
      var pair = it.next();
      s.append(pair._1());
      s.append('=');
      s.append(pair._2());
    }
    s.append('}');
    return s.toString();
  }
}
