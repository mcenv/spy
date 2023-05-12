package dev.mcenv.spy;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public final class Spy {
  public static ProcessBuilder create(
    final Path server,
    final Class<?> commands,
    final String args,
    final String[] mcArgs,
    final String[] jvmArgs
  ) {
    final var java = ProcessHandle.current().info().command().orElseThrow();
    final String javaagent;
    try {
      javaagent = Paths.get(Spy.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    final var type = commands.getName().replace('.', '/');
    final var classpath = System.getProperty("java.class.path");
    final var command = new ArrayList<String>(mcArgs.length + jvmArgs.length + 6);
    command.add(java);
    Collections.addAll(command, jvmArgs);
    Collections.addAll(command,
      "-javaagent:" + javaagent + "=" + type + (args == null ? "" : ("," + args)),
      "-Dspy.server=" + server,
      "-cp",
      classpath,
      Fork.class.getName()
    );
    Collections.addAll(command, mcArgs);
    return new ProcessBuilder(command);
  }
}
