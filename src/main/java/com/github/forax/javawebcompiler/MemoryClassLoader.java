// MemoryClassLoader.java
package com.github.forax.javawebcompiler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MemoryClassLoader extends ClassLoader {
  private final HashMap<String, byte[]> classes = new HashMap<>();

  public void addClass(String name, byte[] bytes) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(bytes);
    classes.put(name, bytes);
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    var bytecode = classes.get(name);
    if (bytecode == null) throw new ClassNotFoundException(name);
    return defineClass(name, bytecode, 0, bytecode.length);
  }
}