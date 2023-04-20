package dev.mcenv.spy;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public final class Spy {
  public static int execute(
    final Class<?> register,
    final String... args
  ) throws Throwable {
    final var java = ProcessHandle.current().info().command().orElseThrow();
    final var javaagent = Paths.get(Spy.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    final var type = register.getName().replace('.', '/');
    final var classpath = System.getProperty("java.class.path");
    final var command = new ArrayList<String>();
    Collections.addAll(command, java, "-javaagent:" + javaagent + "=" + type, "-cp", classpath, Fork.class.getName());
    Collections.addAll(command, args);
    return new ProcessBuilder(command).inheritIO().start().waitFor();
  }
}
