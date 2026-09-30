package com.lindedaoqwq.linclient.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Engine-level patches (run in the obfuscated runtime, so SRG names are used).
 * All patches are fail-safe: if a target is missing the class is returned untouched.
 *
 * 1. EntityRenderer.func_78472_g (updateLightmap): skip recomputing + re-uploading the
 *    16x16 lightmap texture when nothing relevant changed.
 * 2. EntityRenderer.func_78482_e (hurtCameraEffect): No Hurt Cam module.
 * 3. TileEntitySignRenderer.func_192841_a (render): sign-text distance culling. Returns
 *    early when the sign is too far for its text to be legible, which skips splitText +
 *    getStringWidth + drawString for all four lines.
 * 4. LayerArmorBase.func_177178_a (getArmorResource 3-arg): memoises the resolved armour
 *    ResourceLocation so the nested String.format and 40-char hash are paid once.
 *
 * Note: 1.12.2's EffectRenderer.addEffect has no stable SRG mapping, so the particle
 * throttle is only armed on 1.8.9.
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
     * 1.12.2: TileEntitySignRenderer.render(TileEntity, double, double, double, float, int, float).
     * -> inject  if (EngineHooks.skipSignText(te)) return;  at the very top.
     * this = local 0, the tile entity is local 1. Both the TileEntity and the TileEntitySign
     * overload share the SRG name; the guard is identical for either.
     */
    private static byte[] patchSign(byte[] bytes) {
        final String target = "func_192841_a";
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int acc, String n, String desc, String sig, String[] ex) {
                MethodVisitor mv = super.visitMethod(acc, n, desc, sig, ex);
                if (!target.equals(n)) return mv;
                // only the (TileEntity, ...) overload owns a plain TileEntity local at slot 1
                if (desc == null || !desc.startsWith("(Lnet/minecraft/tileentity/TileEntity;")) return mv;
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
     * armour slot, every frame. The returned ResourceLocation is a pure function of that
     * path, so an equal path can reuse the instance vanilla produced first time.
     *
     * Every ARETURN is rewritten to  return EngineHooks.armorResolve(<value>);
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
