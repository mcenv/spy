# <samp>spy</samp>

[![test](https://github.com/mcenv/spy/actions/workflows/test.yml/badge.svg)](https://github.com/mcenv/spy/actions/workflows/test.yml)

<samp>spy</samp> is a highly-compatible code injector for Minecraft: Java Edition[^1].

## Features

- Almost no dependency on the Minecraft core code[^2]
- Code injection through commands via [brigadier](https://github.com/Mojang/brigadier)

## Example

The following code launches the `server.jar` with the `nogui` option passed and the `spy` command registered:

```java
import dev.mcenv.spy.*;

import java.nio.file.Paths;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

public final class Main {
  public static void main(final String[] args) {
    Spy.launch(Paths.get("server.jar"), SpyCommands.class, "nogui");
  }

  public final static class SpyCommands implements Commands {
    @Override
    public void register(final CommandDispatcher<Object> dispatcher) {
      dispatcher.register(
        literal("spy")
          .executes(c -> 0)
      );
    }
  }
}
```

[^1]: NOT OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG.
[^2]: The only exception is the dependency on `net.minecraft.server.MinecraftServer#getServerModName`. However, since this method is not obfuscated, it is likely to be a highly-compatible method provided for mod developers.
