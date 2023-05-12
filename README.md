# <samp>spy</samp>

[![test](https://github.com/mcenv/spy/actions/workflows/test.yml/badge.svg)](https://github.com/mcenv/spy/actions/workflows/test.yml)

<samp>spy</samp> is a highly-compatible code injector for Minecraft: Java Edition[^1].

## Features

- Almost no dependency on the Minecraft core code[^2]
- Code injection through commands via [brigadier](https://github.com/Mojang/brigadier)

## Example

The following code launches the `server.jar`

- With the `spy` commands registered
- With the `Hello from spy!` arguments passed
- With the `nogui` Minecraft option passed
- With the `-Xms2G` and `-Xmx2G` JVM options passed

```java
import com.mojang.brigadier.CommandDispatcher;
import dev.mcenv.spy.*;

import java.io.IOException;
import java.nio.file.Paths;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

public class Main {
  public static void main(String[] args) throws IOException, InterruptedException {
    final ProcessBuilder launcher = Spy.create(
      Paths.get("server.jar"),
      MyCommands.class,
      "Hello from spy!",
      new String[]{"nogui"},
      new String[]{"-Xms2G", "-Xmx2G"}
    );
    launcher.inheritIO().start().waitFor();
  }

  public static final class MyCommands implements Commands {
    @Override
    public void register(final CommandDispatcher<Object> dispatcher, final String args) {
      dispatcher.register(
        literal("spy")
          .executes(c -> {
            System.out.println(args); // Hello from spy!
            return 0;
          })
      );
    }
  }
}
```

[^1]: NOT OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG.
[^2]: The only exception is the dependency on `net.minecraft.server.MinecraftServer#getServerModName`. However, since this method is not obfuscated, it is likely to be a highly-compatible method provided for mod developers.
