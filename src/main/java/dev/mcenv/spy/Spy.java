package dev.mcenv.spy;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public final class Spy {
  public static int launch(
    final Class<?> commands,
    final String[] jvmArgs,
    final String[] mcArgs
  ) throws Throwable {
    final var java = ProcessHandle.current().info().command().orElseThrow();
    final var javaagent = Paths.get(Spy.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    final var type = commands.getName().replace('.', '/');
    final var classpath = System.getProperty("java.class.path");
    final var command = new ArrayList<String>();
    Collections.addAll(command, jvmArgs);
    Collections.addAll(command, java, "-javaagent:" + javaagent + "=" + type, "-cp", '"' + classpath + '"', Fork.class.getName());
    Collections.addAll(command, mcArgs);
    return new ProcessBuilder(command).inheritIO().start().waitFor();
  }

  public static int launch(
    final Class<?> commands,
    final String... mcArgs
  ) throws Throwable {
    return launch(commands, new String[0], mcArgs);
  }
}
