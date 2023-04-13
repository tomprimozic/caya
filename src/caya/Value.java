package caya;

import java.math.BigInteger;

public abstract class Value {
    public final static class Int extends Value {
        public final BigInteger value;
        public Int(BigInteger value) { this.value = value; }
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
}
