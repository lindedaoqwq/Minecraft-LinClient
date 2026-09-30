package com.lindedaoqwq.linclient.util;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Reusable screen-space blur toolkit (separable Gaussian at half resolution),
 * written from scratch. Used to soften the main-menu panorama and as the
 * back-buffer capture / blit primitives for in-game motion blur.
 *
 * Everything is wrapped so a GL failure (lost context, ancient GPU) never
 * throws into the render loop.
 *
 * Note: this LWJGL build exposes shader object *creation* through GL20 but the
 * *query / uniform* entry points through ARBShaderObjects, so we use the ARB
 * set consistently.
 */
public final class BlurUtils {

    private BlurUtils() {}

    private static final String VERT =
            "void main(){ gl_TexCoord[0]=gl_MultiTexCoord0; gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex; }";
    private static final String FRAG =
            "uniform sampler2D tex; uniform vec2 dir; uniform vec2 texel; uniform float radius;" +
            "uniform vec3 overlay; uniform float oa;" +
            "void main(){" +
            " vec4 s=vec4(0.0); float t=0.0; float sig=max(radius*0.5,0.0001);" +
            " for(int i=-8;i<=8;i++){ float x=float(i); float w=exp(-(x*x)/(2.0*sig*sig));" +
            "   s+=texture2D(tex,gl_TexCoord[0].xy+dir*texel*radius*x)*w; t+=w; }" +
            " vec3 c=(s/t).rgb; c=mix(c,overlay,oa); gl_FragColor=vec4(c,1.0); }";

    private static boolean inited = false, ok = false;
    private static int prog = 0;
    private static int uTex, uDir, uTexel, uRadius, uOverlay, uOa;
    private static int texA = 0, fboA = 0, texB = 0, fboB = 0, aW = 0, aH = 0;
    private static final Map<Integer, int[]> capSize = new HashMap<Integer, int[]>();

    private static boolean init() {
        if (inited) return ok;
        inited = true;
        int vs = compile(GL20_VERTEX_SHADER(), VERT);
        int fs = compile(GL20_FRAGMENT_SHADER(), FRAG);
        if (vs < 0 || fs < 0) return false;
        prog = ARBShaderObjects.glCreateProgramObjectARB();
        ARBShaderObjects.glAttachObjectARB(prog, vs);
        ARBShaderObjects.glAttachObjectARB(prog, fs);
        ARBShaderObjects.glLinkProgramARB(prog);
        if (ARBShaderObjects.glGetObjectParameteriARB(prog, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == 0) {
            System.err.println("[LinClient] blur link: " + ARBShaderObjects.glGetInfoLogARB(prog, 1024));
            return false;
        }
        ARBShaderObjects.glDeleteObjectARB(vs);
        ARBShaderObjects.glDeleteObjectARB(fs);
        uTex = ARBShaderObjects.glGetUniformLocationARB(prog, "tex");
        uDir = ARBShaderObjects.glGetUniformLocationARB(prog, "dir");
        uTexel = ARBShaderObjects.glGetUniformLocationARB(prog, "texel");
        uRadius = ARBShaderObjects.glGetUniformLocationARB(prog, "radius");
        uOverlay = ARBShaderObjects.glGetUniformLocationARB(prog, "overlay");
        uOa = ARBShaderObjects.glGetUniformLocationARB(prog, "oa");
        ok = true;
        return true;
    }

    private static int GL20_VERTEX_SHADER() { return org.lwjgl.opengl.GL20.GL_VERTEX_SHADER; }
    private static int GL20_FRAGMENT_SHADER() { return org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER; }

    private static int compile(int type, String src) {
        int s = ARBShaderObjects.glCreateShaderObjectARB(type);
        ARBShaderObjects.glShaderSourceARB(s, src);
        ARBShaderObjects.glCompileShaderARB(s);
        if (ARBShaderObjects.glGetObjectParameteriARB(s, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == 0) {
            System.err.println("[LinClient] blur shader: " + ARBShaderObjects.glGetInfoLogARB(s, 1024));
            return -1;
        }
        return s;
    }

    private static void texParams(int tex) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    public static int createTex(int w, int h) {
        int t = GL11.glGenTextures();
        texParams(t);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, w, h, 0, GL11.GL_RGB,
                GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        return t;
    }

    public static int createFbo(int tex) {
        int f = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, f);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, tex, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        return f;
    }

    /** Copy the current back buffer into `tex` (re-allocates only on size change). */
    public static void capture(int tex, int w, int h) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        texParams(tex);
        int[] d = capSize.get(tex);
        if (d == null || d[0] != w || d[1] != h) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, w, h, 0, GL11.GL_RGB,
                    GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            capSize.put(tex, new int[]{w, h});
        }
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        GL11.glReadBuffer(GL11.GL_BACK);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
    }

    private static void ensure(int w, int h) {
        int pw = Math.max(1, w / 2), ph = Math.max(1, h / 2);
        if (texA == 0) { texA = createTex(pw, ph); fboA = createFbo(texA); }
        if (texB == 0) { texB = createTex(pw, ph); fboB = createFbo(texB); }
        if (aW != pw || aH != ph) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texA);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, pw, ph, 0, GL11.GL_RGB,
                    GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texB);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, pw, ph, 0, GL11.GL_RGB,
                    GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            aW = pw; aH = ph;
        }
    }

    private static void quad() {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(0, 0);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(1, 0);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(1, 1);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(0, 1);
        GL11.glEnd();
    }

    private static void ortho() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, 1, 0, 1, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
    }

    private static void unortho() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    private static void pass(int src, int dstFbo, int pw, int ph, float dx, float dy,
                             float r, float oa, float or, float og, float ob) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, dstFbo);
        GL11.glViewport(0, 0, pw, ph);
        ARBShaderObjects.glUseProgramObjectARB(prog);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, src);
        ARBShaderObjects.glUniform1iARB(uTex, 0);
        ARBShaderObjects.glUniform2fARB(uDir, dx, dy);
        ARBShaderObjects.glUniform2fARB(uTexel, 1f / (float) pw, 1f / (float) ph);
        ARBShaderObjects.glUniform1fARB(uRadius, r);
        ARBShaderObjects.glUniform3fARB(uOverlay, or, og, ob);
        ARBShaderObjects.glUniform1fARB(uOa, oa);
        quad();
        ARBShaderObjects.glUseProgramObjectARB(0);
    }

    /** Paint a texture to the screen as a full-screen quad (passthrough + optional overlay). */
    public static void blit(int tex, int w, int h, int overlay, float oa) {
        if (!init()) return;
        float or = ((overlay >> 16) & 255) / 255f;
        float og = ((overlay >> 8) & 255) / 255f;
        float ob = (overlay & 255) / 255f;
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glViewport(0, 0, w, h);
        ARBShaderObjects.glUseProgramObjectARB(prog);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        ARBShaderObjects.glUniform1iARB(uTex, 0);
        ARBShaderObjects.glUniform2fARB(uDir, 0, 0);
        ARBShaderObjects.glUniform2fARB(uTexel, 1f / (float) w, 1f / (float) h);
        ARBShaderObjects.glUniform1fARB(uRadius, 0);
        ARBShaderObjects.glUniform3fARB(uOverlay, or, og, ob);
        ARBShaderObjects.glUniform1fARB(uOa, oa);
        quad();
        ARBShaderObjects.glUseProgramObjectARB(0);
    }

    /** Capture the current back buffer, blur it, and paint it back as the background. */
    public static void renderBlur(int w, int h, int overlay, float oa, float radius) {
        try {
            if (!init()) return;
            boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
            IntBuffer vb = BufferUtils.createIntBuffer(4);
            GL11.glGetInteger(GL11.GL_VIEWPORT, vb);
            IntBuffer fbb = BufferUtils.createIntBuffer(1);
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING, fbb);
            int fb = fbb.get(0);
            ortho();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_BLEND);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            int src = createTex(w, h);
            capture(src, w, h);
            int pw = Math.max(1, w / 2), ph = Math.max(1, h / 2);
            ensure(w, h);
            float or = ((overlay >> 16) & 255) / 255f;
            float og = ((overlay >> 8) & 255) / 255f;
            float ob = (overlay & 255) / 255f;
            pass(src, fboA, pw, ph, 1, 0, radius, 0, 0, 0, 0);
            pass(texA, fboB, pw, ph, 0, 1, radius, oa, or, og, ob);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            GL11.glViewport(0, 0, w, h);
            ARBShaderObjects.glUseProgramObjectARB(prog);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texB);
            ARBShaderObjects.glUniform1iARB(uTex, 0);
            ARBShaderObjects.glUniform2fARB(uDir, 0, 0);
            ARBShaderObjects.glUniform2fARB(uTexel, 1f / (float) pw, 1f / (float) ph);
            ARBShaderObjects.glUniform1fARB(uRadius, 0);
            ARBShaderObjects.glUniform3fARB(uOverlay, or, og, ob);
            ARBShaderObjects.glUniform1fARB(uOa, oa);
            quad();
            ARBShaderObjects.glUseProgramObjectARB(0);
            GL11.glDeleteTextures(src);
            unortho();
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
