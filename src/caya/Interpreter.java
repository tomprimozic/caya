package caya;

public class Interpreter {
    private static Value error(String msg) {
        throw new RuntimeException(msg);
    }

    public Value eval(Node n) {
        Value result = switch(n) {
            case Node.Int(var loc, var value) -> new Value.Int(value);
            case Node.None(var loc) -> Value.NONE;
            case Node.Bool(var loc, var value) -> value ? Value.TRUE : Value.FALSE;
            default -> error("not implemented");
        };
        return result;
    }
}
