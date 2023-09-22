package dev.mcenv.spy;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.function.Function;
import java.util.jar.JarFile;

import static org.objectweb.asm.Opcodes.*;

final class Agent {
  public static void premain(
    final @NotNull String args,
    final @NotNull Instrumentation instrumentation
  ) throws IOException {
    instrumentation.appendToSystemClassLoaderSearch(getOrExtractBrigadier());
    instrumentation.addTransformer(new ClassFileTransformer() {
      private static byte @NotNull [] transform(
        final byte @NotNull [] classfileBuffer,
        final @NotNull Function<@NotNull ClassVisitor, @NotNull ClassVisitor> createClassVisitor
      ) {
        final var classReader = new ClassReader(classfileBuffer);
        final var classWriter = new ClassWriter(classReader, 0);
        final var classVisitor = createClassVisitor.apply(classWriter);
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
      }

      @Override
      public byte[] transform(
        final ClassLoader loader,
        final @NotNull String className,
        final Class<?> classBeingRedefined,
        final ProtectionDomain protectionDomain,
        final byte @NotNull [] classfileBuffer
      ) {
        return switch (className) {
          case "net/minecraft/server/MinecraftServer" -> transform(classfileBuffer, ModNameTransformer::new);
          case "com/mojang/brigadier/CommandDispatcher" ->
            transform(classfileBuffer, v -> new CommandInjector(v, args));
          default -> null;
        };
      }
    });
  }

  @Contract(" -> new")
  private static @NotNull JarFile getOrExtractBrigadier() throws IOException {
    try (final var server = new JarFile(System.getProperty("spy.server"))) {
      try (final var libraries = new BufferedInputStream(server.getInputStream(server.getEntry("META-INF/libraries.list")))) {
        final var entries = new String(libraries.readAllBytes(), StandardCharsets.UTF_8).split("\n");
        final var brigadierEntry = Arrays
          .stream(entries)
          .map(FileEntry::parse)
          .filter(entry -> entry.path.startsWith("com/mojang/brigadier/"))
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("No brigadier was found"));
        final var brigadierOutPath = Paths.get("libraries", brigadierEntry.path);

        if (!Files.exists(brigadierOutPath) || !checkIntegrity(brigadierOutPath, brigadierEntry.hash)) {
          try (final var brigadier = new BufferedInputStream(server.getInputStream(server.getEntry("META-INF/libraries/" + brigadierEntry.path)))) {
            Files.createDirectories(brigadierOutPath.getParent());
            try (final var out = new BufferedOutputStream(Files.newOutputStream(brigadierOutPath))) {
              brigadier.transferTo(out);
            }
          }
          System.out.printf("Unpacking %s (libraries:%s) to %s\n", brigadierEntry.path, brigadierEntry.id, brigadierOutPath);
        }

        return new JarFile("libraries/" + brigadierEntry.path);
      }
    }
  }

  private static boolean checkIntegrity(
    final @NotNull Path file,
    final @NotNull String expectedHash
  ) throws IOException {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    try (final var output = Files.newInputStream(file)) {
      output.transferTo(new DigestOutputStream(OutputStream.nullOutputStream(), digest));
      final var actualHash = byteToHex(digest.digest());
      if (actualHash.equalsIgnoreCase(expectedHash)) {
        return true;
      } else {
        System.out.printf("Expected file %s to have hash %s, but got %s\n", file, expectedHash, actualHash);
        return false;
      }
    }
  }

  private static @NotNull String byteToHex(
    final byte @NotNull [] bytes
  ) {
    final var result = new StringBuilder(bytes.length * 2);
    for (final var b : bytes) {
      result.append(Character.forDigit(b >> 4 & 0xf, 16));
      result.append(Character.forDigit(b & 0xf, 16));
    }
    return result.toString();
  }

  private record FileEntry(
    @NotNull String hash,
    @NotNull String id,
    @NotNull String path
  ) {
    @Contract("_ -> new")
    public static @NotNull FileEntry parse(
      final @NotNull String string
    ) {
      final var fields = string.split("\t");
      if (fields.length != 3) {
        throw new IllegalStateException("Malformed library entry: " + string);
      } else {
        return new FileEntry(fields[0], fields[1], fields[2]);
      }
    }
  }

  private static final class ModNameTransformer extends ClassVisitor {
    public ModNameTransformer(
      final @NotNull ClassVisitor classVisitor
    ) {
      super(ASM9, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
      final int access,
      final @NotNull String name,
      final @NotNull String descriptor,
      final @Nullable String signature,
      final @Nullable String @NotNull [] exceptions
    ) {
      final var parent = super.visitMethod(access, name, descriptor, signature, exceptions);
      if (name.equals("getServerModName") && descriptor.equals("()Ljava/lang/String;")) {
        return new MethodVisitor(ASM9, parent) {
          @Override
          public void visitLdcInsn(
            final @NotNull Object value
          ) {
            super.visitLdcInsn("spy");
          }
        };
      } else {
        return parent;
      }
    }
  }

  private static final class CommandInjector extends ClassVisitor {
    private final @NotNull String commands;
    private final @Nullable String args;

    public CommandInjector(
      final @NotNull ClassVisitor classVisitor,
      final @NotNull String rawArgs
    ) {
      super(ASM9, classVisitor);
      final var strings = rawArgs.split(",", 2);
      this.commands = strings[0];
      this.args = strings.length > 1 ? strings[1] : null;
    }

    @Override
    public MethodVisitor visitMethod(
      final int access,
      final @NotNull String name,
      final @NotNull String descriptor,
      final @Nullable String signature,
      final @Nullable String @NotNull [] exceptions
    ) {
      final var parent = super.visitMethod(access, name, descriptor, signature, exceptions);
      if (name.equals("<init>") && descriptor.equals("(Lcom/mojang/brigadier/tree/RootCommandNode;)V")) {
        return new MethodVisitor(ASM9, parent) {
          @Override
          public void visitInsn(
            final int opcode
          ) {
            if (opcode == RETURN) {
              visitTypeInsn(NEW, commands);
              visitInsn(DUP);
              visitMethodInsn(INVOKESPECIAL, commands, "<init>", "()V", false);
              visitVarInsn(ALOAD, 0);
              if (args == null) {
                visitInsn(ACONST_NULL);
              } else {
                visitLdcInsn(args);
              }
              visitMethodInsn(INVOKEVIRTUAL, commands, "register", "(Lcom/mojang/brigadier/CommandDispatcher;Ljava/lang/String;)V", false);
            }
            super.visitInsn(opcode);
          }
        };
      } else {
        return parent;
      }
    }
  }
}
