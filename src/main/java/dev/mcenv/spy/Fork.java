package dev.mcenv.spy;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

final class Fork {
  public static void main(final String[] args) throws Throwable {
    MethodHandles
      .lookup()
      .findStatic(
        Class.forName(
          "net.minecraft.bundler.Main",
          true,
          new URLClassLoader(new URL[]{Paths.get(System.getProperty("spy.server")).toUri().toURL()})
        ),
        "main",
        MethodType.methodType(Void.TYPE, String[].class)
      )
      .asFixedArity()
      .invoke((Object) args);
  }
}
