package com.lindedaoqwq.linclient.util;

import com.lindedaoqwq.linclient.config.ModConfig;
import com.lindedaoqwq.linclient.core.Modules;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.nio.IntBuffer;

/**
 * In-game motion blur, implemented from scratch as a temporal accumulation
 * buffer: every frame the live world is blended with the previous frame's
 * result (result = current * (1 - a) + history * a). Fast camera / player
 * movement then leaves a trailing smear; a static scene converges to the sharp
 * frame. Minecraft draws the HUD afterwards, so the HUD stays crisp.
 *
 * Triggered from RenderGameOverlayEvent.Pre (gameplay only) — menus do not fire
 * that event, so the blur is purely an in-world effect.
 */
public final class MotionBlur {

    private MotionBlur() {}

    private static final String BLEND =
            "uniform sampler2D t0; uniform sampler2D t1; uniform float a;" +
            "void main(){" +
            " vec3 c = mix(texture2D(t0, gl_TexCoord[0].xy).rgb," +
            "              texture2D(t1, gl_TexCoord[0].xy).rgb, a);" +
            " gl_FragColor = vec4(c, 1.0); }";

    private static boolean inited = false, ok = false;
    private static int prog = 0, uT0, uT1, uA;
    private static int capTex = 0, histTex = 0, histFbo = 0, tmpTex = 0, tmpFbo = 0;

    public static void reset() {
        capTex = histTex = tmpTex = 0;
        histFbo = tmpFbo = 0;
    }

    private static boolean init() {
        if (inited) return ok;
        inited = true;
        int vs = compile(org.lwjgl.opengl.GL20.GL_VERTEX_SHADER,
                "void main(){ gl_TexCoord[0]=gl_MultiTexCoord0; gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex; }");
        int fs = compile(org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER, BLEND);
        if (vs < 0 || fs < 0) return false;
        prog = ARBShaderObjects.glCreateProgramObjectARB();
        ARBShaderObjects.glAttachObjectARB(prog, vs);
        ARBShaderObjects.glAttachObjectARB(prog, fs);
        ARBShaderObjects.glLinkProgramARB(prog);
        if (ARBShaderObjects.glGetObjectParameteriARB(prog, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == 0) {
            System.err.println("[LinClient] motionblur link: " + ARBShaderObjects.glGetInfoLogARB(prog, 1024));
            return false;
        }
        ARBShaderObjects.glDeleteObjectARB(vs);
        ARBShaderObjects.glDeleteObjectARB(fs);
        uT0 = ARBShaderObjects.glGetUniformLocationARB(prog, "t0");
        uT1 = ARBShaderObjects.glGetUniformLocationARB(prog, "t1");
        uA = ARBShaderObjects.glGetUniformLocationARB(prog, "a");
        ok = true;
        return true;
    }

    private static int compile(int t, String s) {
        int sh = ARBShaderObjects.glCreateShaderObjectARB(t);
        ARBShaderObjects.glShaderSourceARB(sh, s);
        ARBShaderObjects.glCompileShaderARB(sh);
        if (ARBShaderObjects.glGetObjectParameteriARB(sh, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == 0) {
            System.err.println("[LinClient] motionblur shader: " + ARBShaderObjects.glGetInfoLogARB(sh, 1024));
            return -1;
        }
        return sh;
    }

    private static void quad() {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(0, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(1, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(1, 1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(0, 1);
        GL11.glEnd();
    }

    public static void render(int w, int h) {
        if (!Modules.on("motionblur")) { reset(); return; }
        try {
            if (!init()) return;
            float a = ModConfig.value("motionblur.amount", 0.5F);
            if (a < 0.02F) { reset(); return; }
            if (a > 0.85F) a = 0.85F;

            if (capTex == 0) capTex = BlurUtils.createTex(w, h);
            if (histTex == 0) {
                histTex = BlurUtils.createTex(w, h); histFbo = BlurUtils.createFbo(histTex);
                tmpTex = BlurUtils.createTex(w, h); tmpFbo = BlurUtils.createFbo(tmpTex);
            }

            // Capture the live world into capTex.
            BlurUtils.capture(capTex, w, h);

            boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
            IntBuffer vb = BufferUtils.createIntBuffer(4);
            GL11.glGetInteger(GL11.GL_VIEWPORT, vb);
            IntBuffer fbb = BufferUtils.createIntBuffer(1);
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING, fbb);
            int fb = fbb.get(0);

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glOrtho(0, 1, 0, 1, -1, 1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_BLEND);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);

            // Blend current world with previous frame -> tmp.
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, tmpFbo);
            GL11.glViewport(0, 0, w, h);
            ARBShaderObjects.glUseProgramObjectARB(prog);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, capTex);
            ARBShaderObjects.glUniform1iARB(uT0, 0);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, histTex);
            ARBShaderObjects.glUniform1iARB(uT1, 1);
            ARBShaderObjects.glUniform1fARB(uA, a);
            quad();
            ARBShaderObjects.glUseProgramObjectARB(0);

            // Paint the blended result back to the screen (replaces sharp world).
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL11.glViewport(0, 0, w, h);
            BlurUtils.blit(tmpTex, w, h, 0, 0F);

            // Swap history and tmp so next frame blends against this result.
            int ht = histTex, hf = histFbo, tt = tmpTex, tf = tmpFbo;
            histTex = tt; histFbo = tf; tmpTex = ht; tmpFbo = hf;

            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            if (blend) GL11.glEnable(GL11.GL_BLEND);
            else GL11.glDisable(GL11.GL_BLEND);
            GL11.glViewport(vb.get(0), vb.get(1), vb.get(2), vb.get(3));
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fb);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        } catch (Throwable t) { /* never crash the render loop */ }
    }
}
