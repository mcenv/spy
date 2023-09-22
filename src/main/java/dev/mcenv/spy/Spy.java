package dev.mcenv.spy;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public final class Spy {
  @Contract("_, _, _, _, _ -> new")
  public static @NotNull ProcessBuilder create(
    final @NotNull Path server,
    final @NotNull Class<?> commands,
    final @Nullable String args,
    final @NotNull String @Nullable [] mcArgs,
    final @NotNull String @Nullable [] jvmArgs
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
    final var command = new ArrayList<String>(6 + (mcArgs == null ? 0 : mcArgs.length) + (jvmArgs == null ? 0 : jvmArgs.length));
    command.add(java);
    if (jvmArgs != null) {
      Collections.addAll(command, jvmArgs);
    }
    Collections.addAll(command,
      "-javaagent:" + javaagent + "=" + type + (args == null ? "" : ("," + args)),
      "-Dspy.server=" + server,
      "-cp",
      classpath,
      Fork.class.getName()
    );
    if (mcArgs != null) {
      Collections.addAll(command, mcArgs);
    }
    return new ProcessBuilder(command);
  }
}
