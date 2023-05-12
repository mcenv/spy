package dev.mcenv.spy;

import com.mojang.brigadier.CommandDispatcher;

@FunctionalInterface
public interface Commands {
  void register(
    final CommandDispatcher<Object> dispatcher,
    final String args
  );
}
