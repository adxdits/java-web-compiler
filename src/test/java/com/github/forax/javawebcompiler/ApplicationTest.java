package com.github.forax.javawebcompiler;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class ApplicationTest {
  @Test
  public void compileValidCode() {
    var code = """
      public class Main {
        public static void main(String[] args) {
          System.out.println("Hello");
        }
      }
      """;
    var result = Compiler.compileInMemory("Main", code, new MemoryClassLoader());
    assertTrue(result.isEmpty());
  }

  @Test
  public void diagnosticContainsLineAndColumn() {
    var source = """
      public class Main {
        public static void main(String[] args) {
          int x = "ksdjfj";
        }
      }
    """;
    var diagnostics = Compiler.compileInMemory("Main", source, new MemoryClassLoader());
    assertFalse(diagnostics.isEmpty());
    var first = diagnostics.getFirst();
    assertTrue(first.line() > 0);
    assertTrue(first.column() > 0);
    assertNotNull(first.message());
    assertFalse(first.message().isEmpty());
  }

    @Test
    public void runHelloWorld() throws Exception {
      var code = """
        public class Main {
          public static void main(String[] args) {
            System.out.println("Hello");
          }
        }
      """;
      var loader = new MemoryClassLoader();
      var diagnostics = Compiler.compileInMemory("Main", code, loader);
      var result = Runner.runFromMemory("Main", loader, diagnostics);
      assertEquals(new Runner.RunResult("Hello\n", List.of()), result);
  }

    @Test
    public void runWithNoCompiledCode() {
      var loader = new MemoryClassLoader();
      assertThrows(ClassNotFoundException.class, () -> Runner.runFromMemory("Main", loader, List.of()));
    }

    @Test
    public void runMultipleLines() throws Exception {
      var code = """
        public class Main {
          public static void main(String[] args) {
          System.out.println("line1");
          System.out.println("line2");
          }
        }
      """;
      var loader = new MemoryClassLoader();
      var diagnostics = Compiler.compileInMemory("Main", code, loader);
      var result = Runner.runFromMemory("Main", loader, diagnostics);
      assertEquals(new Runner.RunResult("line1\nline2\n", List.of()), result);
    }

    @Test
    public void runEmptyOutput() throws Exception {
      var code = """
        public class Main {
          public static void main(String[] args) {}
        }
      """;
      var loader = new MemoryClassLoader();
      var diagnostics = Compiler.compileInMemory("Main", code, loader);
      var result = Runner.runFromMemory("Main", loader, diagnostics);
      assertEquals(new Runner.RunResult("", List.of()), result);
    }

  @Test
  public void compileWithDifferentClassName() {
    var code = """
      public class Test {
        public static void main(String[] args) {
        }
      }
      """;
    var loader = new MemoryClassLoader();
    var diagnostics = Compiler.compileInMemory("Test", code,loader);

    assertTrue(diagnostics.isEmpty());
  }

  @Test
  public void compileWithWrongClassNameShouldFail() {
    var code = """
      public class Test {
      }
      """;

    var loader = new MemoryClassLoader();
    var diagnostics = Compiler.compileInMemory("Main", code,loader);

    assertFalse(diagnostics.isEmpty());
  }

  @Test
  public void compileClassWithoutMainShouldStillCompile() {
    var code = """
        public class Test {
            int x = 10;
        }
        """;

    var loader = new MemoryClassLoader();
    var diagnostics = Compiler.compileInMemory("Test", code,loader);

    assertTrue(diagnostics.isEmpty());
  }
  @Test
  public void compileWrongCodeWithMultipleErrors(){
    var code = """
        public class Main {
            System.out.println("Hello");
            int a = "";
        }
        """;
    var loader = new MemoryClassLoader();
    var diagnostics = Compiler.compileInMemory("Main", code,loader);
    assertFalse(diagnostics.isEmpty());
    assertEquals(2, diagnostics.size());
  }
}
