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
 *    16x16 lightmap texture when nothing relevant changed. Runs every tick; skipping most
 *    invocations removes constant light-value evaluation and GL texture upload cost.
 * 2. EntityRenderer.func_78482_e (hurtCameraEffect): No Hurt Cam module.
 * 3. EffectRenderer.func_78873_a (addEffect): engine-level particle throttle.
 */
public class LinClientTransformer implements IClassTransformer {
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
            // 1.12.2: EffectRenderer.addEffect has no SRG mapping; particle control uses the vanilla setting instead.
        } catch (Throwable t) {
            System.out.println("[LinClient] transform failed for " + transformedName + ": " + t);
        }
        return bytes;
    }

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
            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                    "com/lindedaoqwq/linclient/hooks/EngineHooks", hook, "()Z", false);
            Label cont = new Label();
            super.visitJumpInsn(Opcodes.IFEQ, cont);
            super.visitInsn(Opcodes.RETURN);
            super.visitLabel(cont);
        }
    }
}
