package dev.mcenv.spy;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

public final class Agent {
  public static void premain(
    final String args,
    final Instrumentation instrumentation
  ) {
    instrumentation.addTransformer(new ClassFileTransformer() {
      private static byte[] transform(
        final byte[] classfileBuffer,
        final Function<ClassVisitor, ClassVisitor> createClassVisitor
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
        final String className, Class<?> classBeingRedefined,
        final ProtectionDomain protectionDomain,
        final byte[] classfileBuffer
      ) {
        return switch (className) {
          case "net/minecraft/server/MinecraftServer" -> transform(classfileBuffer, ModNameTransformer::new);
          case "com/mojang/brigadier/CommandDispatcher" -> transform(classfileBuffer, v -> new CommandInjector(v, args));
          default -> null;
        };
      }
    });
  }

  private static final class ModNameTransformer extends ClassVisitor {
    public ModNameTransformer(final ClassVisitor classVisitor) {
      super(ASM9, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final String[] exceptions
    ) {
      final var parent = super.visitMethod(access, name, descriptor, signature, exceptions);
      if (name.equals("getServerModName") && descriptor.equals("()Ljava/lang/String;")) {
        return new MethodVisitor(ASM9, parent) {
          @Override
          public void visitLdcInsn(final Object value) {
            super.visitLdcInsn("spy");
          }
        };
      } else {
        return parent;
      }
    }
  }

  private static final class CommandInjector extends ClassVisitor {
    private final String commands;
    private final String args;

    public CommandInjector(
      final ClassVisitor classVisitor,
      final String rawArgs
    ) {
      super(ASM9, classVisitor);
      final var strings = rawArgs.split(",", 2);
      this.commands = strings[0];
      this.args = strings.length > 1 ? strings[1] : null;
    }

    @Override
    public MethodVisitor visitMethod(
      final int access,
      final String name,
      final String descriptor,
      final String signature,
      final String[] exceptions
    ) {
      final var parent = super.visitMethod(access, name, descriptor, signature, exceptions);
      if (name.equals("<init>") && descriptor.equals("(Lcom/mojang/brigadier/tree/RootCommandNode;)V")) {
        return new MethodVisitor(ASM9, parent) {
          @Override
          public void visitInsn(final int opcode) {
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
