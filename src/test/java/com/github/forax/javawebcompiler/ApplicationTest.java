package com.github.forax.javawebcompiler;

import org.junit.jupiter.api.Test;

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
    var result = Application.compileInMemory("Main", code);
    assertTrue(result.isEmpty());
  }
}
