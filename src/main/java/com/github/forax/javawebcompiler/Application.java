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

  static void main(String[] args) {
    var app = JExpress.express();

    // Serve the static frontend files from "public"
    app.use(JExpress.staticFiles(Path.of("public")));
    var objectMapper = new ObjectMapper();

    app.post("/compile", (req, res) -> {
      try {
        var compileRequest = objectMapper.readValue(req.bodyText(), CompileRequest.class);
        var newLoader = new MemoryClassLoader();
        var diagnostics = Compiler.compileInMemory("Main", compileRequest.code(), newLoader);

        res.send(objectMapper.writeValueAsString(diagnostics));
      } catch (Exception e) {
        res.status(500).json("""
            {"error": "Internal Server Error"}
        """);
      }
    });

    app.post("/run", (req, res) -> {
      try {
        var compileRequest = objectMapper.readValue(req.bodyText(), CompileRequest.class);
        var newLoader = new MemoryClassLoader();
        var diagnostics = Compiler.compileInMemory("Main", compileRequest.code(), newLoader);
        if (!diagnostics.isEmpty()) {
          throw new Error("TODO Marko");
        }
        var output = Runner.runFromMemory("Main", newLoader);
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