package caya;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import caya.Builtins.BuiltinValue;
import caya.Builtins.Descriptor;
import caya.Builtins.Int;
import caya.Runtime.Value;

public final class Vector<E extends Value> extends BuiltinValue implements Iterable<E> {
  // copied from https://github.com/tomprimozic/vector/blob/master/src/experiment/vector/Vector.java
  private record Pair(Object[] left, Object[] right) {}

  private static final Object[] EMPTY_ARRAY = new Object[]{};
  private static final int SHIFT = 5;
  private static final int SIZE = 1 << SHIFT;      // size of a trie level
  private static final int MASK = SIZE - 1;

  private final Left<E> left;
  private final Right<E> right;

  private Vector(Left<E> left, Right<E> right) {
    assert Math.abs(left.shift - right.shift) <= SHIFT;   // THE invariant
    this.left = left;
    this.right = right;
  }

  public static final Vector<? extends Value> empty = new Vector<Value>(Left.empty, Right.empty);

  public Value get_item(Value item) { return get(Interpreter.to_int32(item)); }

  public final HashMap<String, Descriptor> attrs() { return ATTRS; }
  public static final HashMap<String, Descriptor> ATTRS = BuiltinValue.resolve_attrs(Vector.class,
    new String[] {"size", "first", "last"},
    new String[] {"push", "append", "update", "pop", "shift", "iter"}
  );

  public static Vector<? extends Value> create(Value[] args, Map<String, Value> named_args) {
    if(named_args != null && !named_args.isEmpty()) {
      throw new Interpreter.InterpreterError("vector(...) cannot be called with named arguments");
    }
    return Vector.make(args);
  }

  public static final Builtins.Type TYPE = new Builtins.Type("vector", Vector::create, new HashMap<>(Map.of("empty", empty)), ATTRS);

  // TODO: can these be optimised?
  public Value first() { return get(0); }
  public Value last() { return get(size_int32() - 1); }

  private static byte shift(int size) {
    int rem = size - 1;
    byte shift = 0;
    while(rem > 0) {
      rem = rem >> SHIFT;
      shift += SHIFT;
    }
    return shift;
  }
  public int size_int32() { return left.extra.length + left.data_size + right.data_size + right.extra.length; }
  public Int size() { return new Int(size_int32()); }

  @SuppressWarnings("unchecked")
  public E get(int i) {
    if(!(0 <= i && i < size_int32())) { throw new IndexOutOfBoundsException(i); }
    var left_size = left.extra.length + left.data_size;
    return (E) (i < left_size ? left.get(i) : right.get(i - left_size));
  }

  public Vector<E> update(Int i, E element) { return update_int32(i.value.intValueExact(), element); }

  public Vector<E> update_int32(int i, E element) {
    if(!(0 <= i && i < size_int32())) { throw new IndexOutOfBoundsException(i); }
    var left_size = left.extra.length + left.data_size;
    if(i < left_size) {
      return new Vector<>(left.update(i, element), right);
    } else {
      return new Vector<>(left, right.update(i - left_size, element));
    }
  }

  public java.util.List<E> toList() {
    var result = new java.util.ArrayList<E>(size_int32());
    for(var e : this) { result.add(e); }
    return result;
  }

  public int hashCode() { return Runtime.hash_sequence(this); }

  @Override public boolean equals(Object other) {
    if(other instanceof Vector v && size_int32() == v.size_int32()) {
      var this_it = iterator();
      var other_it = iterator();
      while(this_it.hasNext()) {
        if(!this_it.next().equals(other_it.next())) { return false;}
      }
      return true;
    } else { return false; }
  }

  private static class VectorIterator<E extends Value> implements Iterator<E> {
    private Object[] current;
    private Left<E> left;
    private Right<E> right;
    private int n = 0;
    private int i = 0;

    VectorIterator(Vector<E> v) {
      left = v.left;
      right = v.right;
      if(v.left.extra.length > 0) {
        current = v.left.extra;
      } else {
        advance();
      }
    }

    private void advance() {
      i = 0;
      if(left != null) {
        if(n < left.data_size) {
          current = left.get_array(n);
          n = n + SIZE;
          return;
        } else {
          left = null;
          n = 0;
        }
      }
      if(right != null) {
        if (n < right.data_size) {
          current = right.get_array(n);
          n = n + SIZE;
        } else {
          current = right.extra;
          right = null;
        }
        return;
      }
      current = EMPTY_ARRAY;
    }

    @Override
    public boolean hasNext() {
      return i < current.length;
    }

    @SuppressWarnings("unchecked")
    @Override
    public E next() {
      var next = current[i];
      if(i < current.length - 1) {
        i = i + 1;
      } else {
        advance();
      }
      return (E) next;
    }
  }
  @Override
  public Iterator<E> iterator() { return new VectorIterator<>(this); }

  @SuppressWarnings("unchecked")
  public Builtins.Iterator iter() { return new Builtins.Iterator((Iterator<Value>) this.iterator()); }

  public static <E extends Value> Vector<E> make(E[] items) { return make(java.util.Arrays.stream(items)); }
  public static <E extends Value> Vector<E> make(Stream<E> items) { return make(items.iterator()); }
  public static <E extends Value> Vector<E> make(Iterable<E> items) { return make(items.iterator()); }

  @SuppressWarnings("unchecked")
  public static <E extends Value> Vector<E> make(Iterator<E> items) {
    // TODO: this can probably be optimised
    Vector<E> result = (Vector<E>) Vector.empty;
    while(items.hasNext()) { result = result.append(items.next()); }
    return result;
  }

  public String toString() {
    var s = new StringBuilder("[");
    var first = true;
    var it = iterator();
    while(it.hasNext()) {
      if(first) {
        first = false;
      } else {
        s.append(", ");
      }
      var item = it.next();
      s.append(item);
    }
    s.append(']');
    return s.toString();
  }

  private <A extends Half<E, A, B>, B extends Half<E, B, A>> Vector<E> push_internal(E e, A other, B to) {
    if(to.extra.length == SIZE && to.data_size >= (1 << to.shift) && other.shift < to.shift) {
      if(other.data == null) {
        return to.vector(
            other.make(SIZE, to.data, other.extra),
            to.make(SIZE, to.extra, new Object[]{e})
        );
      } else {
        var split = to.split(
            other.data,
            to.data,
            single(to.shift - SHIFT, to.extra)
        );
        return to.vector(
            other.make(other.data_size + to.data_size / 2, split.left, other.extra),
            to.make(to.data_size / 2 + SIZE, split.right, new Object[]{e})
        );
      }
    } else {
      return to.vector(other, to.push(e));
    }
  }

  public Vector<E> append_one(E e) { return push_internal(e, left, right); }
  public Vector<E> push_one(E e) { return push_internal(e, right, left); }

  @SafeVarargs
  final public Vector<E> append(E... items) { /* push_right */
    var result = this;
    for(var item : items) { result = result.append_one(item); }
    return result;
  }

  @SafeVarargs
  final public Vector<E> push(E... items) { /* push_left or prepend */
    var result = this;
    for(var item : items) { result = result.push_one(item); }
    return result;
  }

  private <A extends Half<E, A, B>, B extends Half<E, B, A>> Vector<E> pop(A other, B from) {
    if(from.extra.length <= 1) {         // will change `data`
      if (other.shift > from.shift) {   // need to take care of maintaining invariant
        if(from.data_size == SIZE || from.data_size == (1 << (from.shift - SHIFT)) + SIZE) {
          // would reduce `from.depth` and break the invariant
          Object[] other_data, from_data;
          Object[] from_extra = from.shift == SHIFT ? from.data : from.end();
          if(from.extra.length == 0) {
            from_extra = from.pop(from_extra);
          }
          int moved;
          if(other.data.length == 2 && ((Object[]) other.data[other.end_index(other.data)]).length == 1) {
            // naively moving data from `other` to `from` would break the invariant in the *other* direction
            moved = (1 << (other.shift - SHIFT)) / 2;
            var split = from.split(
                ((Object[]) other.data[other.end_index(other.data)])[0],
                (Object[]) other.data[other.start_index(other.data)],
                from.data[from.start_index(from.data)]
            );
            other_data = split.left;
            from_data = split.right;
          } else {
            // move last element of `other.data` to `from.data` to maintain the invariant
            if(other.data.length == 2) {
              other_data = (Object[]) other.data[other.end_index(other.data)];
            } else {
              other_data = from.pop(other.data);
            }
            moved = 1 << (other.shift - SHIFT);
            if (from.shift == SHIFT) {
              from_data = (Object[]) other.data[other.start_index(other.data)];
            } else {
              from_data = from.array2(
                  other.data[other.start_index(other.data)],
                  new Object[]{from.data[from.start_index(from.data)]}
              );
            }
          }
          return from.vector(
              other.make(other.data_size - moved, other_data, other.extra),
              from.make(from.data_size + moved - SIZE, from_data, from_extra)
          );
        } else if(from.extra.length + from.shift == 0) {      // we're popping from an empty half, take data from the other half
          return from.vector(
              other.make(0, null, other.extra),
              from.make(0, null, from.pop(other.data))
          );
        }
      } else if(from.extra.length + from.shift == 0 && other.shift == 0) {
        if(other.extra.length > 0) {
          return from.vector(
              other.make(0, null, EMPTY_ARRAY),
              from.make(0, null, from.pop(other.extra))
          );
        } else {
          throw new IllegalStateException("empty vector");
        }
      }
    }
    return from.vector(other, from.pop());
  }

  public Vector<E> shift() { /* pop_right or removeLast */ return pop(left, right); }
  public Vector<E> pop() { /* pop_left or removeHead */ return pop(right, left); }

  private static Object[] single(int shift, Object[] array) {
    for(; shift > SHIFT; shift -= SHIFT) {
      array = new Object[]{array};
    }
    return array;
  }

  private abstract static class Half<E extends Value, This extends Half<E, This, That>, That extends Half<E, That, This>> {
    final byte shift;
    final Object[] extra;
    final Object[] data;
    final int data_size;

    public Half(int data_size, Object[] data, Object[] extra) {
      assert data == null || data.length > 1;
      assert extra != null;
      this.extra = extra;
      this.data = data;
      this.data_size = data_size;
      this.shift = shift(data_size);
    }

    abstract This make(int data_size, Object[] data, Object[] extra);
    abstract Vector<E> vector(That end, This start);
    abstract int start_index(Object[] array);
    abstract int end_index(Object[] array);
    abstract Object[] end();

    protected Pair split(Object[] array) {
      var start = new Object[SIZE / 2 + 1];
      var end = new Object[SIZE / 2 + 1];
      System.arraycopy(array, 0, start, 1, SIZE / 2);
      System.arraycopy(array, SIZE / 2, end, 0, SIZE / 2);
      return new Pair(start, end);
    }

    protected Pair split(Object start, Object[] array, Object end) {
      var split = this.split(array);
      var start_data = split.left;
      start_data[this.start_index(start_data)] = start;
      var end_data = split.right;
      end_data[this.end_index(end_data)] = end;
      return new Pair(start_data, end_data);
    }

    protected Object[] array2(Object start, Object end) {
      Object[] array = new Object[2];
      array[start_index(array)] = start;
      array[end_index(array)] = end;
      return array;
    }

    protected abstract Object[] push(Object[] array, Object element);
    protected This push(E e) {
      if(extra.length < SIZE) {
        return make(data_size, data, push(extra, e));
      } else {
        Object[] new_data;
        if(data == null) {
          new_data = extra;
        } else if(data_size >= 1 << shift) {    // need new level
          new_data = array2(data, single(shift, extra));
        } else {
          new_data = push_node(data, shift - SHIFT, extra);
        }
        return make(data_size + SIZE, new_data, new Object[]{e});
      }
    }

    private Object[] push_node(Object[] parent, int shift, Object[] array) {
      if(shift == 0) { return array; }
      int i = (data_size >> shift) & MASK;
      if(i < parent.length) {
        Object[] updated = parent.clone();
        int j = end_index(updated);
        updated[j] = push_node((Object[]) parent[j], shift - SHIFT, array);
        return updated;
      } else {
        return push(parent, single(shift, array));
      }
    }

    protected abstract Object[] pop(Object[] array);
    protected This pop() {
      if(extra.length > 1) {
        return make(data_size, data, pop(extra));
      } else if(data == null) {
        if(extra.length == 0) { throw new IllegalStateException("empty"); }
        return make(0, null, EMPTY_ARRAY);
      } else {
        Object[] new_extra;
        Object[] new_data;
        if(data_size == SIZE) {
          new_extra = data;
          new_data = null;
        } else {
          new_extra = end();
          if (data_size == (1 << (shift - SHIFT)) + SIZE) {  // reduce depth
            new_data = (Object[]) data[start_index(data)];
          } else {
            new_data = pop_node(data, shift - SHIFT);
          }
        }
        if(extra.length == 0) {
          new_extra = pop(new_extra);
        }
        return make(data_size - SIZE, new_data, new_extra);
      }
    }

    private Object[] pop_node(Object[] parent, int shift) {
      int mask = (1 << shift) - SIZE;         // selects all *next* indices
      if(((data_size - 1) & mask) == 0) {     // all next indices are 0 -> remove whole column
        return pop(parent);
      } else {
        Object[] updated = parent.clone();
        int i = end_index(updated);
        updated[i] = pop_node((Object[]) updated[i], shift - SHIFT);
        return updated;
      }
    }
  }

  private final static class Right<E extends Value> extends Half<E, Right<E>, Left<E>> {
    private Right(int data_size, Object[] data, Object[] extra) {
      super(data_size, data, extra);
    }
    private static final Right<Value> empty = new Right<>(0, null, EMPTY_ARRAY);
    protected Right<E> make(int data_size, Object[] data, Object[] extra) { return new Right<>(data_size, data, extra); }
    protected Vector<E> vector(Left<E> left, Right<E> right) { return new Vector<>(left, right); }
    protected int start_index(Object[] array) { return 0; }
    protected int end_index(Object[] array) { return array.length - 1; }
    protected Object[] end() { return get_array(data_size - 1); }

    protected Object[] push(Object[] array, Object element) {
      Object[] result = new Object[array.length + 1];
      System.arraycopy(array, 0, result, 0, array.length);
      result[array.length] = element;
      return result;
    }
    protected Object[] pop(Object[] array) {
      Object[] result = new Object[array.length - 1];
      System.arraycopy(array, 0, result, 0, result.length);
      return result;
    }

    private Object get(int i) {
      if(i >= data_size) { return extra[i - data_size]; }
      Object[] a = get_array(i);
      return a[i & MASK];
    }

    private Object[] get_array(int i) {
      int shift = this.shift;
      Object[] a = data;
      while(shift > SHIFT) {
        shift -= SHIFT;
        a = (Object[]) a[(i >> shift) & MASK];
      }
      return a;
    }

    private Right<E> update(int i, Object element) {
      if(i >= data_size) {
        Object[] new_extra = extra.clone();
        new_extra[i - data_size] = element;
        return new Right<>(data_size, this.data, new_extra);
      } else {
        return new Right<>(data_size, update_array(this.data, this.shift, i, element), this.extra);
      }
    }

    private Object[] update_array(Object[] a, int shift, int i, Object element) {
      a = a.clone();
      if(shift > SHIFT) {
        shift -= SHIFT;
        int j = (i >> shift) & MASK;
        a[j] = update_array((Object[]) a[j], shift, i, element);
      } else {
        a[i & MASK] = element;
      }
      return a;
    }
  }
  private final static class Left<E extends Value> extends Half<E, Left<E>, Right<E>> {
    private Left(int data_size, Object[] data, Object[] extra) {
      super(data_size, data, extra);
    }
    private static final Left<Value> empty = new Left<>(0, null, EMPTY_ARRAY);
    protected Left<E> make(int data_size, Object[] data, Object[] extra) { return new Left<>(data_size, data, extra); }
    protected Vector<E> vector(Right<E> right, Left<E> left) { return new Vector<>(left, right); }
    protected int start_index(Object[] array) { return array.length - 1; }
    protected int end_index(Object[] array) { return 0; }
    protected Object[] end() { return get_array(0); }

    protected Object[] push(Object[] array, Object element) {
      Object[] result = new Object[array.length + 1];
      System.arraycopy(array, 0, result, 1, array.length);
      result[0] = element;
      return result;
    }
    protected Object[] pop(Object[] array) {
      Object[] result = new Object[array.length - 1];
      System.arraycopy(array, 1, result, 0, result.length);
      return result;
    }

    public Pair split(Object[] array) {
      var pair = super.split(array);
      return new Pair(pair.right, pair.left);
    }

    private Object get(int i) {
      if(i < extra.length) { return extra[i]; }
      i -= extra.length;
      Object[] a = get_array(i);
      return a[i & MASK];
    }

    private Object[] get_array(int i) {
      i += ((1 << this.shift) - data_size);    // adjust because of right bias
      int shift = this.shift;
      Object[] a = data;
      while(shift > SHIFT) {
        shift -= SHIFT;
        a = (Object[]) a[((i >> shift) & MASK) - SIZE + a.length];
      }
      return a;
    }

    private Left<E> update(int i, Object element) {
      if(i < extra.length) {
        Object[] new_extra = extra.clone();
        new_extra[i] = element;
        return new Left<>(data_size, this.data, new_extra);
      } else {
        i += ((1 << this.shift) - data_size);    // adjust because of right bias
        return new Left<>(data_size, update_array(this.data, this.shift, i, element), this.extra);
      }
    }

    private Object[] update_array(Object[] a, int shift, int i, Object element) {
      a = a.clone();
      if(shift > SHIFT) {
        shift -= SHIFT;
        int j = ((i >> shift) & MASK) - SIZE + a.length;
        a[j] = update_array((Object[]) a[j], shift, i, element);
      } else {
        a[i & MASK] = element;
      }
      return a;
    }
  }
}