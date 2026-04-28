package com.github.forax.javawebcompiler;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public final class ApplicationTest {
  @Test
  public void compileValidCode() {
    var code =
        """
      public class Main {
        public static void main(String[] args) {
          System.out.println("Hello");
        }
      }
      """;
    var result = Compiler.compileInMemory("Main", code);
    assertTrue(result.isEmpty());
  }

  @Test
  public void diagnosticContainsLineAndColumn() {
    var source =
        """
        public class Main {
            public static void main(String[] args) {
                int x = "ksdjfj";
            }
        }
        """;
    var diagnostics = Compiler.compileInMemory("Main", source);
    assertFalse(diagnostics.isEmpty());
    var first = diagnostics.getFirst();
    assertTrue(first.line() > 0);
    assertTrue(first.column() > 0);
    assertNotNull(first.message());
    assertFalse(first.message().isEmpty());
  }
  @Test
  public void warningOnlyCodeIsSuccess(){
    var source = """
        import java.util.ArrayList;
        public class Main {
            public static void main(String[] args) {
                ArrayList list = new ArrayList();
            }
        }
        """;
    var diagnostic = Compiler.compileInMemory("Main", source);
    assertTrue(diagnostic.isEmpty());
  }
}
