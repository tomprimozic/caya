package caya;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import caya.Builtins.*;
import caya.Runtime.Value;
import caya.Runtime.Type;

public class JVM {
  public static final class Cls extends Type {
    public final Class<?> cls;
    public Cls(Class<?> cls) { this.cls = cls; }

    @Override public String toString() { return "JVM class " + cls.toString(); }
    @Override public int hashCode() { return cls.hashCode(); }
    @Override public boolean equals(Object other) { return cls.equals(other); }

    @Override public Value call(Value[] args, Map<String, Value> named_args) {
      if(named_args != null && !named_args.isEmpty()) {
        throw new Interpreter.InterpreterError("JVM constructor for `" + cls + "` cannot be called with named arguments");
      }
      try {
        return wrap(ConstructorUtils.invokeConstructor(cls, unwrap(args)));
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
        throw new Interpreter.Control.Exception(new Obj(e));
      }
    }
  }

  public static Object[] unwrap(Value[] values) {
    var result = new Object[values.length];
    for(int i = 0; i < values.length; i++) {
      result[i] = unwrap(values[i]);
    }
    return result;
  }

  public static Object unwrap(Value value) {
    return switch(value) {
      case Obj obj -> obj.obj;
      case Bool b -> b.value;
      case Str str -> str.value;
      case None __ -> null;
      default -> value;
    };
  }

  public static Value wrap(Object obj) {
    return switch(obj) {
      case null -> Builtins.NONE;
      case Boolean b -> b ? Builtins.TRUE : Builtins.FALSE;
      case String str -> new Str(str);
      case Value value -> value;
      default -> new Obj(obj);
    };
  }

  public static final class Obj extends Value {
    public final Object obj;
    public Cls cls;
    public Obj(Object obj) { this.obj = obj; }

    @Override public String toString() {
      if(obj == null) { return "null"; }
      if(obj.getClass().isArray()) { return Arrays.deepToString((Object[]) obj); }
      return obj.toString();
    }
    @Override public int hashCode() { return obj.hashCode(); }
    @Override public boolean equals(Object other) { return obj.equals(other); }

    @Override public final Value get_attr(String attr) { return new BoundMethod(obj, attr); }
    @Override public Cls type() { if(cls == null) { cls = new Cls(obj.getClass()); }; return cls; }
  }

  public static final class BoundMethod extends Value {
    public final Object obj;
    public final String method_name;
    public BoundMethod(Object obj, String method_name) { this.obj = obj; this.method_name = method_name; }
    @Override public Value call(Value[] args, Map<String, Value> named_args) {
      if(named_args != null && !named_args.isEmpty()) {
        throw new Interpreter.InterpreterError("JVM method `" + obj.getClass() + "." + method_name + "` cannot be called with named arguments");
      }
      try {
        return wrap(invoke_method(obj, method_name, unwrap(args)));
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        throw new Interpreter.Control.Exception(new Obj(e));
      }
    }
    @Override public int hashCode() { return Runtime.combine_hash(BoundMethod.class, obj, method_name); }
    @Override public boolean equals(Object other) { return other instanceof BoundMethod m && m.obj.equals(this.obj) && m.method_name.equals(this.method_name); }
    public static final Builtins.Type TYPE = new Builtins.Type("builtins.jvm_bound_method", null, new HashMap<>(), new HashMap<>());
    @Override public Builtins.Type type() { return TYPE; }
  }

  public static boolean verify_module_access(java.lang.Module currentModule, Class<?> memberClass) {
    // copied from jdk.internal.reflect.Reflection.verifyModuleAccess
    var memberModule = memberClass.getModule();
    if (currentModule == memberModule) {
        return true;
    } else {
        return memberModule.isExported(memberClass.getPackageName(), currentModule);
    }
  }

  public static Object invoke_method(final Object object, final String method_name, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    // copied from org.apache.commons.lang3.reflect.MethodUtils.invokeMethod and modified to get an accessible superclass
    final Class<?>[] parameter_types = ClassUtils.toClass(args);
    Class<?> cls = object.getClass();
    while(!verify_module_access(JVM.class.getModule(), cls)) {
      cls = cls.getSuperclass();
    }
    var method = MethodUtils.getMatchingAccessibleMethod(cls, method_name, parameter_types);
    if (method == null) {
      throw new NoSuchMethodException("No such accessible method: " + method_name + "() on object: " + object.getClass().getName());
    }
    return method.invoke(object, prepare_arguments(method, args));
  }

  public static Object[] prepare_arguments(java.lang.reflect.Executable e, Object[] args) {
    if (e.isVarArgs()) {
      var varargs_type = e.getParameterTypes()[e.getParameterCount() - 1];
      var length = args.length - e.getParameterCount() + 1;
      var varargs = java.lang.reflect.Array.newInstance(varargs_type.componentType(), length);
      System.arraycopy(args, e.getParameterCount() - 1, varargs, 0, length);
      args = java.util.Arrays.copyOf(args, e.getParameterCount(), Object[].class);
      args[args.length - 1] = varargs;
    }
    return args;
  }
}