# <samp>spy</samp>

[![test](https://github.com/mcenv/spy/actions/workflows/test.yml/badge.svg)](https://github.com/mcenv/spy/actions/workflows/test.yml)

<samp>spy</samp> is a highly-compatible code injector for Minecraft: Java Edition[^1].

## Example

The following code launches the `server.jar` in the current directory with the `nogui` option passed and the `spy` command registered:

```java
import dev.mcenv.spy.*;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;

public final class Main {
  public static void main(final String[] args) {
    Spy.launch(SpyCommands.class, "nogui");
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
