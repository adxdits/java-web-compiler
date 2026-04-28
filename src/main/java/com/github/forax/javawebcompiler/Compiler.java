package com.github.forax.javawebcompiler;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Compiler {
  private Compiler(){
      throw new AssertionError("no instances");
  }
  record Diagnostic(long line, long column, String message, String kind) {}
  private record CompilerResult(boolean success, DiagnosticCollector<Object> diagnostics, MemoryClassLoader loader) {}
  record CompileRequest(String code){
    CompileRequest {
      Objects.requireNonNull(code);
    }
  }

  // Package private for testing
  static List<Diagnostic> compileInMemory(String className, String sourceCode, MemoryClassLoader loader) {
    var compileResult = compileTask(className, sourceCode, loader);
    return compilationResultHandler(compileResult);
  }

  private static CompilerResult compileTask (String className, String sourceCode, MemoryClassLoader loader){
    var compiler = ToolProvider.getSystemJavaCompiler();
    if(compiler == null){
      throw new IllegalStateException("Compiler not available");
    }
    var diagnostics = new DiagnosticCollector<>();
    var fileManager = new MemoryFileManager(compiler.getStandardFileManager(null, null, null), loader);

    var file = new SimpleJavaFileObject(
            URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
            JavaFileObject.Kind.SOURCE) {
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return sourceCode;
        }
    };

    var compilationUnits = List.of(file);
    var task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);

    var success = task.call();
    return new CompilerResult(success, diagnostics, loader);
  }

  private static List<Diagnostic> compilationResultHandler(CompilerResult compilerResult){
    var result = new ArrayList<Diagnostic>();
    if (!compilerResult.success) {
      for (var diagnostic : compilerResult.diagnostics.getDiagnostics()) {
        result.add(new Diagnostic(
                diagnostic.getLineNumber(),
                diagnostic.getColumnNumber(),
                diagnostic.getMessage(Locale.FRANCE),
                diagnostic.getKind().name()
        ));
      }
    }

    return result;
  }
}
