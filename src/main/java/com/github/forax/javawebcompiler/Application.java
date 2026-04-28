package com.github.forax.javawebcompiler;

import module java.base;
import module java.compiler;

import tools.jackson.databind.ObjectMapper;

public class Application {
  private record CompileRequest(String code){
    private CompileRequest {
      Objects.requireNonNull(code);
    }
  }

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

        res.send(objectMapper.writeValueAsString(diagnostics));
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
        if (!diagnostics.isEmpty()) {
          throw new Error("TODO Marko");
        }
        var output = Runner.runFromMemory(className, newLoader);
        res.send(objectMapper.writeValueAsString(Map.of("output", output)));
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