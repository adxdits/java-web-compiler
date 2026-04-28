package com.github.forax.javawebcompiler;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Compiler {
    record Diagnostic(long line, long column, String message) {}
    private record CompilerResult(boolean success, DiagnosticCollector<Object> diagnostics) {}

    // Package private for testing
    static List<Diagnostic> compileInMemory(String className, String sourceCode) {
        var compileResult = compileTask(className, sourceCode);
        return compilationResultHandler(compileResult);
    }

    private static CompilerResult compileTask (String className, String sourceCode){
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<>();

        var file = new SimpleJavaFileObject(
                URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
                JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return sourceCode;
            }
        };

        var compilationUnits = List.of(file);
        var task = compiler.getTask(null, null, diagnostics, null, null, compilationUnits);

        var success = task.call();
        return new CompilerResult(success, diagnostics);
    }

    private static List<Diagnostic> compilationResultHandler(CompilerResult compilerResult){
        var result = new ArrayList<Diagnostic>();
        if (!compilerResult.success) {
            for (var diagnostic : compilerResult.diagnostics.getDiagnostics()) {
                result.add(new Diagnostic(
                        diagnostic.getLineNumber(),
                        diagnostic.getColumnNumber(),
                        diagnostic.getMessage(Locale.FRANCE)));
            }
        }
        return result;
    }
}
