package dev.mcenv.spy;

import com.mojang.brigadier.CommandDispatcher;

@FunctionalInterface
public interface Register {
  void apply(final CommandDispatcher<Object> dispatcher);
}
