package com.github.forax.javawebcompiler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

public final class Runner {
  private Runner(){
      throw new AssertionError("no instances");
  }

  record RunResult(String output, List<Compiler.Diagnostic> errors) {}

  static RunResult runFromMemory(String className, MemoryClassLoader loader, List<Compiler.Diagnostic> diagnostics) throws Exception {
    Objects.requireNonNull(className);
    Objects.requireNonNull(loader);
    Objects.requireNonNull(diagnostics);

    if (!diagnostics.isEmpty()) {
      return new RunResult("", diagnostics);
    }

    var runClass = loader.loadClass(className);
    var method = runClass.getMethod("main", String[].class);
    var out = new ByteArrayOutputStream();
    var old = System.out;
    System.setOut(new PrintStream(out));
    try {
        method.invoke(null, (Object) new String[]{});
    } finally {
        System.setOut(old);
    }

    return new RunResult(out.toString(), List.of());
  }
}