package com.github.forax.javawebcompiler;

import tools.jackson.databind.ObjectMapper;

import module java.base;

public class Application {
  private record CompileRequest(String code){
    private CompileRequest {
      Objects.requireNonNull(code);
    }
  }

  private record CompileResponse(String className, List<Compiler.Diagnostic> diagnostics) {}

  private static final Pattern CLASSNAME_PATTERN = Pattern.compile("class\\s+(\\w+)");

  // Dynamic class name extraction
  private static String classNameExtractor(String code) {
    var m = CLASSNAME_PATTERN.matcher(code);
    return m.find() ? m.group(1) : "Main";
  }

  static void main(String[] args) {
    var app = JExpress.express();

    // Serve the static frontend files from "public"
    app.use(JExpress.staticFiles(Path.of("public")));
    var objectMapper = new ObjectMapper();

    app.post("/compile", (req, res) -> {
      try {
        var body = req.bodyText();
        var compileRequest = objectMapper.readValue(body, CompileRequest.class);
        var sourceCode = compileRequest.code();
        var className = classNameExtractor(sourceCode);
        var newLoader = new MemoryClassLoader();
        var diagnostics = Compiler.compileInMemory(className, sourceCode, newLoader);

        res.json(objectMapper.writeValueAsString(new CompileResponse(className, diagnostics)));
      } catch (Exception e) {
        res.status(500).json("""
            {"error": "Internal Server Error"}
        """);
      }
    });

    app.post("/run", (req, res) -> {
      try {
        var body = req.bodyText();
        var compileRequest = objectMapper.readValue(body, CompileRequest.class);
        var sourceCode = compileRequest.code();
        var className = classNameExtractor(sourceCode);
        var newLoader = new MemoryClassLoader();
        var diagnostics = Compiler.compileInMemory(className, sourceCode, newLoader);
        var runResult = Runner.runFromMemory(className, newLoader, diagnostics);
        res.send(objectMapper.writeValueAsString(runResult));
      } catch (Exception e) {
        res.status(500).json("""
          {"error": "Internal Server Error"}
        """);
      }
    });
    app.listen(8080);
    System.out.println("Web site on http://localhost:8080/index.html");
  }
}