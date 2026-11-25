package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import dev.lumen.hud.TargetTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

/** Target animations: дополнительные анимации Target ESP — «захват» квадратом, скобки, кольца, орбита, прицел и др. */
public final class TargetAnim {
    public static final Module M = new Module("Target animations",
            "More Target ESP animations (capture, rings, orbit...)", Category.VISUALS, false, true);
    static final Setting MODE  = M.add(Setting.mode("mode", "Mode", 0,
            "Capture", "Brackets", "Rings", "Orbit", "Reticle", "Pulse", "Diamond", "Spiral"));
    static final Setting COLOR = M.add(Setting.mode("color", "Color", 2, Fx.PAL));
    static final Setting SPEED = M.add(Setting.num("speed", "Speed", 1, 0.3, 3, 0.1));
    static final Setting SIZE  = M.add(Setting.num("size", "Size", 1, 0.5, 2, 0.1));
    static final Setting AIMED = M.add(Setting.bool("aimed", "Use aimed entity", true));

    private static LivingEntity cur;
    private static long acquired;
    private static final float[] A = new float[3], B = new float[3];

    private TargetAnim() { }

    static void draw(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity me, float td, long now) {
        if (!M.enabled) { cur = null; return; }
        LivingEntity t = TargetTracker.entity;
        if ((t == null || !t.isAlive()) && AIMED.bool && mc.crosshairTarget instanceof EntityHitResult r
                && r.getEntity() instanceof LivingEntity le) t = le;
        if (t == null || t == me || !t.isAlive()) { cur = null; return; }
        if (t != cur) { cur = t; acquired = now; }

        Box b = Fx.lerped(t, td);
        double cx = (b.minX + b.maxX) / 2.0, cz = (b.minZ + b.maxZ) / 2.0;
        double h = b.maxY - b.minY, w = Math.max(b.maxX - b.minX, b.maxZ - b.minZ);
        float spd = (float) SPEED.num, size = (float) SIZE.num;
        float time = now / 1000f * spd;

        switch (MODE.mode) {
            case 0, 1 -> screenFrame(ctx, b, now, MODE.mode == 0);
            case 2 -> { // Rings: два кольца бегают вверх-вниз по телу
                double rad = Math.max(0.5, w * 0.9) * size;
                for (int k = 0; k < 2; k++) {
                    double y = b.minY + h * (0.5 + 0.5 * Math.sin(time * 2.0 + k * Math.PI));
                    ring(ctx, cx, y, cz, rad, Fx.color(COLOR.mode, time * 0.2f + k * 0.5f), 0.95f, 40);
                }
                ring(ctx, cx, b.minY + 0.05, cz, rad * 1.15, Fx.color(COLOR.mode, 0f), 0.35f, 40);
            }
            case 3 -> { // Orbit: светящиеся точки со шлейфом
                double rad = Math.max(0.6, w * 1.0) * size;
                for (int i = 0; i < 4; i++) {
                    for (int tr = 0; tr < 7; tr++) {
                        double ang = time * 2.4 - tr * 0.13 + i * Math.PI / 2.0;
                        double y = b.minY + h * (0.5 + 0.3 * Math.sin(time * 1.5 + i));
                        if (Fx.to(cx + Math.cos(ang) * rad, y, cz + Math.sin(ang) * rad, A)) {
                            Fx.glowDot(ctx, A[0], A[1], Fx.color(COLOR.mode, i / 4f + time * 0.1f), (1f - tr / 7f) * 0.95f, tr == 0 ? 1 : 0);
                        }
                    }
                }
            }
            case 4 -> reticle(ctx, b, time, size);
            case 5 -> { // Pulse: расходящиеся кольца по земле
                for (int k = 0; k < 3; k++) {
                    float ph = (time * 0.6f + k / 3f) % 1f;
                    double rad = (0.3 + ph * 1.6 * size) * Math.max(1.0, w);
                    ring(ctx, cx, b.minY + 0.05, cz, rad, Fx.color(COLOR.mode, ph), 1f - ph, 40);
                }
            }
            case 6 -> diamond(ctx, cx, b.maxY + 0.55 + 0.08 * Math.sin(time * 2.0), cz, time, size);
            default -> { // Spiral: спираль поднимается вокруг цели
                double rad = Math.max(0.5, w * 0.9) * size;
                int n = 60;
                for (int i = 0; i < n; i++) {
                    float f = i / (float) n;
                    double ang = f * Math.PI * 6.0 + time * 3.0;
                    double y = b.minY + h * ((f + time * 0.2f) % 1f);
                    if (Fx.to(cx + Math.cos(ang) * rad, y, cz + Math.sin(ang) * rad, A)) {
                        Fx.glowDot(ctx, A[0], A[1], Fx.color(COLOR.mode, f + time * 0.15f), 0.9f, 0);
                    }
                }
            }
        }
    }

    /** Окружность в мире на высоте y. */
    private static void ring(DrawContext ctx, double cx, double y, double cz, double rad, int rgb, float alpha, int n) {
        boolean prev = false;
        float px = 0, py = 0;
        for (int i = 0; i <= n; i++) {
            double a = 2.0 * Math.PI * i / n;
            boolean ok = Fx.to(cx + Math.cos(a) * rad, y, cz + Math.sin(a) * rad, A);
            if (ok && prev) Fx.thinLine(ctx, px, py, A[0], A[1], rgb, alpha);
            px = A[0]; py = A[1]; prev = ok;
        }
    }

    /** Capture: квадрат «захвата» сжимается к цели и фиксируется; Brackets: только уголки. */
    private static void screenFrame(DrawContext ctx, Box b, long now, boolean capture) {
        if (!Fx.projectBox(b, Fx.PTS)) return;
        Fx.rect(Fx.PTS, Fx.RECT);
        float cx = (Fx.RECT[0] + Fx.RECT[2]) / 2f, cy = (Fx.RECT[1] + Fx.RECT[3]) / 2f;
        float base = Math.max(Fx.RECT[2] - Fx.RECT[0], Fx.RECT[3] - Fx.RECT[1]) / 2f * 1.12f * (float) SIZE.num + 5f;

        float tt = capture ? MathHelper.clamp((now - acquired) / (550f / (float) SPEED.num), 0f, 1f) : 1f;
        float e = 1f - (1f - tt) * (1f - tt) * (1f - tt);
        float half = base * (1f + 1.7f * (1f - e));
        float ang = (1f - e) * 1.5708f;                    // квадрат довернётся на 90° пока сжимается
        boolean locked = tt >= 1f;
        if (locked) half *= 1f + 0.035f * MathHelper.sin(now / 140f);

        int rgb = locked ? Fx.color(COLOR.mode, 0f) : Fx.lerp(0xFFFFFF, Fx.color(COLOR.mode, 0f), e);
        float[] xs = new float[4], ys = new float[4];
        for (int i = 0; i < 4; i++) {
            float a = ang + i * 1.5708f + 0.7854f;
            xs[i] = cx + MathHelper.cos(a) * half * 1.4142f;
            ys[i] = cy + MathHelper.sin(a) * half * 1.4142f;
        }
        if (capture && !locked) {
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) & 3;
                Fx.thinLine(ctx, xs[i], ys[i], xs[j], ys[j], rgb, 0.45f * (1f - e) + 0.2f);
            }
        }
        // уголки (толстые)
        for (int i = 0; i < 4; i++) {
            int n = (i + 1) & 3, p = (i + 3) & 3;
            float nx = xs[i] + (xs[n] - xs[i]) * 0.3f, ny = ys[i] + (ys[n] - ys[i]) * 0.3f;
            float px = xs[i] + (xs[p] - xs[i]) * 0.3f, py = ys[i] + (ys[p] - ys[i]) * 0.3f;
            Fx.thinLine(ctx, xs[i], ys[i], nx, ny, rgb, 1f);
            Fx.thinLine(ctx, xs[i] + 1, ys[i], nx + 1, ny, rgb, 0.7f);
            Fx.thinLine(ctx, xs[i], ys[i], px, py, rgb, 1f);
            Fx.thinLine(ctx, xs[i], ys[i] + 1, px, py + 1, rgb, 0.7f);
        }
        if (locked) Fx.glowDot(ctx, cx, cy, rgb, 0.9f, 1);
    }

    /** Reticle: вращающийся прицел вокруг цели. */
    private static void reticle(DrawContext ctx, Box b, float time, float size) {
        if (!Fx.projectBox(b, Fx.PTS)) return;
        Fx.rect(Fx.PTS, Fx.RECT);
        float cx = (Fx.RECT[0] + Fx.RECT[2]) / 2f, cy = (Fx.RECT[1] + Fx.RECT[3]) / 2f;
        float r = Math.max(Fx.RECT[2] - Fx.RECT[0], Fx.RECT[3] - Fx.RECT[1]) / 2f * 0.75f * size + 8f;
        int rgb = Fx.color(COLOR.mode, time * 0.2f);
        boolean prev = false;
        float px = 0, py = 0;
        for (int i = 0; i <= 36; i++) {
            float a = i * 6.2832f / 36f;
            float x = cx + MathHelper.cos(a) * r, y = cy + MathHelper.sin(a) * r;
            if (i > 0) Fx.thinLine(ctx, px, py, x, y, rgb, 0.55f);
            px = x; py = y;
        }
        for (int k = 0; k < 4; k++) {
            float a = time * 1.6f + k * 1.5708f;
            float c = MathHelper.cos(a), s = MathHelper.sin(a);
            Fx.thinLine(ctx, cx + c * (r - 6), cy + s * (r - 6), cx + c * (r + 7), cy + s * (r + 7), rgb, 1f);
        }
        Fx.glowDot(ctx, cx, cy, rgb, 0.85f, 1);
    }

    /** Diamond: вращающийся ромб-бипирамида над головой. */
    private static void diamond(DrawContext ctx, double cx, double cy, double cz, float time, float size) {
        double r = 0.22 * size, hh = 0.30 * size;
        int rgb = Fx.color(COLOR.mode, time * 0.15f);
        float[][] v = new float[6][];
        boolean ok = true;
        double[][] pts = new double[6][];
        pts[0] = new double[] { cx, cy + hh, cz };
        pts[1] = new double[] { cx, cy - hh, cz };
        for (int i = 0; i < 4; i++) {
            double a = time * 2.2 + i * Math.PI / 2.0;
            pts[2 + i] = new double[] { cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r };
        }
        for (int i = 0; i < 6; i++) {
            float[] o = new float[3];
            if (!Fx.to(pts[i][0], pts[i][1], pts[i][2], o)) { ok = false; break; }
            v[i] = o;
        }
        if (!ok) return;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) & 3;
            Fx.thinLine(ctx, v[0][0], v[0][1], v[2 + i][0], v[2 + i][1], rgb, 0.9f);
            Fx.thinLine(ctx, v[1][0], v[1][1], v[2 + i][0], v[2 + i][1], rgb, 0.9f);
            Fx.thinLine(ctx, v[2 + i][0], v[2 + i][1], v[2 + j][0], v[2 + j][1], rgb, 0.6f);
        }
        Fx.glowDot(ctx, v[0][0], v[0][1], rgb, 0.9f, 1);
    }
}
