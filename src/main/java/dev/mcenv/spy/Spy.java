package dev.mcenv.spy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public final class Spy {
  public static Process launch(
    final Path server,
    final Class<?> commands,
    final String[] jvmArgs,
    final String[] mcArgs
  ) throws IOException {
    final var java = ProcessHandle.current().info().command().orElseThrow();
    final String javaagent;
    try {
      javaagent = Paths.get(Spy.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    final var type = commands.getName().replace('.', '/');
    final var classpath = System.getProperty("java.class.path");
    final var command = new ArrayList<String>();
    Collections.addAll(command, jvmArgs);
    Collections.addAll(command,
      java,
      "-javaagent:" + javaagent + "=" + type,
      "-Dspy.server=" + server,
      "-cp",
      classpath,
      Fork.class.getName()
    );
    Collections.addAll(command, mcArgs);
    return new ProcessBuilder(command).start();
  }

  public static Process launch(
    final Path server,
    final Class<?> commands,
    final String... mcArgs
  ) throws IOException {
    return launch(server, commands, new String[0], mcArgs);
  }
}
