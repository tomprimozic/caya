package caya;

public final class Runtime {
  public static abstract class Value {
    public Value call(Value[] args) { throw new Interpreter.NotImplemented(); }
    public Value get_attr(String attr) { throw new Interpreter.NotImplemented(); }
    public void set_attr(String attr, Value value) { throw new Interpreter.NotImplemented(); }
    public Value get_item(Value item) { throw new Interpreter.NotImplemented(); }
    public void set_item(Value item, Value value) { throw new Interpreter.NotImplemented(); }
  }

  public static final class Function extends Value {
    public final String[] params;
    public final Interpreter.Scope closure;
    public final Node body;
    public Function(String[] params, Interpreter.Scope closure, Node body) { this.params = params; this.closure = closure; this.body = body; }
    public Value call(Value[] args) { return Interpreter.eval_function(params, closure, body, args, null); }
  }
}
