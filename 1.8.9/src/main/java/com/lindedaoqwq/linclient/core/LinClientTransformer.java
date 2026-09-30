package com.lindedaoqwq.linclient.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Engine-level patches (run in the obfuscated runtime, so SRG names are used).
 * All patches are fail-safe: if a target is missing the class is returned untouched.
 *
 * 1. EntityRenderer.updateLightmap : skip the 256-entry recompute + 16x16 texture upload
 *    when neither the world nor the gamma changed (at most every 250 ms).
 * 2. EntityRenderer.hurtCameraEffect : No Hurt Cam module.
 * 3. EffectRenderer.addEffect : engine-level particle throttle.
 * 4. TileEntitySignRenderer.renderTileEntityAt : sign-text distance culling. Returns early
 *    when the sign is too far for its text to be legible, which skips splitText +
 *    getStringWidth + drawString for all four lines.
 * 5. LayerArmorBase.getArmorResource : caches the resolved armour ResourceLocation so the
 *    nested String.format and 40-char hash are paid once instead of every frame.
 */
public class LinClientTransformer implements IClassTransformer {
    private static final String HOOKS = "com/lindedaoqwq/linclient/hooks/EngineHooks";

    public LinClientTransformer() {
        System.out.println("[LinClient] coremod transformer registered");
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        try {
            if ("net.minecraft.client.renderer.EntityRenderer".equals(transformedName)) {
                byte[] out = patch(bytes, "func_78472_g", 0, "func_78482_e", 1);
                System.out.println("[LinClient] EntityRenderer patched: lightmap-skip + nohurtcam armed");
                return out;
            }
            if ("net.minecraft.client.particle.EffectRenderer".equals(transformedName)) {
                byte[] out = patch(bytes, "func_78873_a", 2, null, -1);
                System.out.println("[LinClient] EffectRenderer patched: particle throttle armed");
                return out;
            }
            if ("net.minecraft.client.renderer.tileentity.TileEntitySignRenderer".equals(transformedName)) {
                byte[] out = patchSign(bytes);
                System.out.println("[LinClient] TileEntitySignRenderer patched: sign-text culling armed");
                return out;
            }
            if ("net.minecraft.client.renderer.entity.layers.LayerArmorBase".equals(transformedName)) {
                byte[] out = patchArmor(bytes);
                System.out.println("[LinClient] LayerArmorBase patched: armour texture cache armed");
                return out;
            }
        } catch (Throwable t) {
            System.out.println("[LinClient] transform failed for " + transformedName + ": " + t);
        }
        return bytes;
    }

    // ---------------------------------------------------------------- guard patches

    /** kind: 0 = skipLightmap, 1 = noHurtCam, 2 = skipParticle */
    private static byte[] patch(byte[] bytes, String m1, int k1, String m2, int k2) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String n, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(acc, n, desc, sig, ex);
                if (m1.equals(n)) return new Guard(mv, k1);
                if (m2 != null && m2.equals(n)) return new Guard(mv, k2);
                return mv;
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    private static class Guard extends MethodVisitor {
        private final int kind;
        Guard(MethodVisitor mv, int kind) { super(Opcodes.ASM5, mv); this.kind = kind; }

        @Override
        public void visitCode() {
            super.visitCode();
            String hook = kind == 0 ? "skipLightmap" : (kind == 1 ? "noHurtCam" : "skipParticle");
            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, hook, "()Z", false);
            Label cont = new Label();
            super.visitJumpInsn(Opcodes.IFEQ, cont);
            super.visitInsn(Opcodes.RETURN);
            super.visitLabel(cont);
        }
    }

    // ---------------------------------------------------------------- sign text culling

    /**
     * TileEntitySignRenderer.renderTileEntityAt(TileEntity, double, double, double, float, int)
     * -> inject  if (EngineHooks.skipSignText(te)) return;  at the very top.
     * The first argument is the tile entity, so we reload local 1 (this=0).
     */
    private static byte[] patchSign(byte[] bytes) {
        final String target = "func_180535_a";
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String n, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(acc, n, desc, sig, ex);
                if (!target.equals(n)) return mv;
                final String argDesc = "(L" + Type.getObjectType(
                        "net/minecraft/tileentity/TileEntity").getInternalName() + ";)Z";
                return new MethodVisitor(Opcodes.ASM5, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        super.visitVarInsn(Opcodes.ALOAD, 1);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "skipSignText",
                                "(Ljava/lang/Object;)Z", false);
                        Label cont = new Label();
                        super.visitJumpInsn(Opcodes.IFEQ, cont);
                        super.visitInsn(Opcodes.RETURN);
                        super.visitLabel(cont);
                    }
                };
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    // ---------------------------------------------------------------- armour texture cache

    /**
     * LayerArmorBase.getArmorResource(ItemArmor, boolean, String) builds the armour texture
     * path with a nested String.format and then hashes a ~40-char string, for every worn
     * armour slot, every frame. The value it returns is a pure function of the path it built,
     * so the returned ResourceLocation can simply be memoised by that path.
     *
     * Every ARETURN of the 3-arg overload is rewritten to
     *   return EngineHooks.armorResolve(<value>);
     * which returns the previously seen instance for an equal path. The instance handed back
     * is byte-for-byte the one vanilla created the first time, so nothing downstream changes.
     */
    private static byte[] patchArmor(byte[] bytes) {
        final String target = "func_177178_a";   // getArmorResource(ItemArmor, boolean, String)
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String n, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(acc, n, desc, sig, ex);
                if (!target.equals(n)) return mv;
                return new MethodVisitor(Opcodes.ASM5, mv) {
                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == Opcodes.ARETURN) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOKS, "armorResolve",
                                    "(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/util/ResourceLocation;",
                                    false);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }
}
