package com.github.forax.javawebcompiler;

import module java.base;
import module java.compiler;

import tools.jackson.databind.ObjectMapper;

import javax.tools.ToolProvider;

public class Application {

  private record Diagnostic(long line, long column, String message) {} 

  static void main(String[] args) {
    var app = JExpress.express();

    // Serve the static frontend files from "public"
    app.use(JExpress.staticFiles(Path.of("public")));

    var objectMapper = new ObjectMapper();

    app.post("/compile", (req, res) -> {
      try {
        var body = req.bodyText();
        var tree = objectMapper.readTree(body);
        var sourceCode = tree.get("code").asString();

        var diagnostics = compileInMemory("Main", sourceCode);
        res.send(objectMapper.writeValueAsString(diagnostics));
      } catch (Exception e) {
        res.status(500).json("""
            {"error": "Internal Server Error"}
            """);
      }
    });

    app.listen(8080);
    System.out.println("Web site on http://localhost:8080/index.html");
  }

  // Package private for testing
  static List<Diagnostic> compileInMemory(String className, String sourceCode) {
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
    var result = new ArrayList<Diagnostic>();

    if (!success) {
      for (var diagnostic : diagnostics.getDiagnostics()) {
        result.add(new Diagnostic(
            diagnostic.getLineNumber(),
            diagnostic.getColumnNumber(),
            diagnostic.getMessage(Locale.FRANCE)));
      }
    }
    return result;
  }
}