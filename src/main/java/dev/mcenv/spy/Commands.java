package dev.mcenv.spy;

import com.mojang.brigadier.CommandDispatcher;

public interface Commands {
  void register(
    final CommandDispatcher<Object> dispatcher,
    final String args
  );
}
