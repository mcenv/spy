package dev.mcenv.spy;

import com.mojang.brigadier.CommandDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Commands {
  void register(
    final @NotNull CommandDispatcher<Object> dispatcher,
    final @Nullable String args
  );
}
