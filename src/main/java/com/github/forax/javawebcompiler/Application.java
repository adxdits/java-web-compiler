package com.github.forax.javawebcompiler;

import module java.base;
import module java.compiler;

import javax.tools.ToolProvider;
import tools.jackson.databind.ObjectMapper;

public class Application {

  record Diagnostic(long line, long column, String message) {}

  private record CompilerResult(boolean success, DiagnosticCollector<Object> diagnostics) {}

  static void main(String[] args) {
    var app = JExpress.express();

    // Serve the static frontend files from "public"
    app.use(JExpress.staticFiles(Path.of("public")));

    var objectMapper = new ObjectMapper();

    app.post(
        "/compile",
        (req, res) -> {
          try {
            var body = req.bodyText();
            var tree = objectMapper.readTree(body);
            var sourceCode = tree.get("code").asString();

            var diagnostics = compileInMemory("Main", sourceCode);
            res.send(objectMapper.writeValueAsString(diagnostics));
          } catch (Exception e) {
            res.status(500)
                .json(
                    """
            {"error": "Internal Server Error"}
            """);
          }
        });

    app.listen(8080);
    System.out.println("Web site on http://localhost:8080/index.html");
  }

  // Package private for testing
  static List<Diagnostic> compileInMemory(String className, String sourceCode) {
    var compileResult = compileTask(className, sourceCode);
    return compilationResultHandler(compileResult);
  }

  private static CompilerResult compileTask(String className, String sourceCode) {
    var compiler = ToolProvider.getSystemJavaCompiler();
    var diagnostics = new DiagnosticCollector<>();

    var file =
        new SimpleJavaFileObject(
            URI.create(
                "string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension),
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

  private static List<Diagnostic> compilationResultHandler(CompilerResult compilerResult) {
    var result = new ArrayList<Diagnostic>();
    if (!compilerResult.success) {
      for (var diagnostic : compilerResult.diagnostics.getDiagnostics()) {
        result.add(
            new Diagnostic(
                diagnostic.getLineNumber(),
                diagnostic.getColumnNumber(),
                diagnostic.getMessage(Locale.FRANCE)));
      }
    }
    return result;
  }
}
