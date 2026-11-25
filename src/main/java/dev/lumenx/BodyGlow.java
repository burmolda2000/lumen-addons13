package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/** Body glow: светящийся ореол вокруг моделей игроков (эллипс по их хитбоксу). */
public final class BodyGlow {
    public static final Module M = new Module("Body glow",
            "Glow around player models", Category.VISUALS, false, true);
    static final Setting STYLE    = M.add(Setting.mode("style", "Style", 0, "Ellipse", "Model outline"));
    static final Setting TARGETS  = M.add(Setting.mode("targets", "Targets", 0, "Players", "All living"));
    static final Setting LOS      = M.add(Setting.bool("los", "Line of sight only", true));
    static final Setting COLOR    = M.add(Setting.mode("color", "Color", 6, Fx.PAL));
    static final Setting HEALTH   = M.add(Setting.bool("health", "Color by health", false));
    static final Setting SIZE     = M.add(Setting.num("size", "Size", 4, 0, 16, 1));
    static final Setting STRENGTH = M.add(Setting.num("strength", "Strength (%)", 70, 10, 100, 5));
    static final Setting SELF     = M.add(Setting.bool("self", "Show yourself", false));

    private BodyGlow() { }

    /** Режим «Model outline»: заявляет цели для контура по силуэту модели (невидимые игроки не подсвечиваются). */
    static void collect(ClientWorld w, ClientPlayerEntity me, MinecraftClient mc) {
        if (!M.enabled || STYLE.mode != 1) return;
        long now = System.currentTimeMillis();
        double md2 = 48.0 * 48.0;
        if (SELF.bool && !Fx.firstPerson(mc)) Glow.want(me, glowColor(me, now));
        if (TARGETS.mode == 0) {
            for (AbstractClientPlayerEntity pl : w.getPlayers()) {
                if (pl == me || pl.isSpectator() || pl.isInvisible()) continue;
                if (me.squaredDistanceTo(pl) > md2) continue;
                if (LOS.bool && !me.canSee(pl)) continue;
                Glow.want(pl, glowColor(pl, now));
            }
        } else {
            for (Entity e : w.getOtherEntities(me, me.getBoundingBox().expand(48.0),
                    x -> x instanceof LivingEntity && x.isAlive() && !x.isSpectator() && !x.isInvisible()
                            && !(x instanceof ArmorStandEntity))) {
                if (LOS.bool && !me.canSee(e)) continue;
                Glow.want(e, glowColor(e, now));
            }
        }
    }

    private static int glowColor(Entity e, long now) {
        if (HEALTH.bool && e instanceof LivingEntity le) {
            return Fx.lerp(0xFF3B3B, 0x55FF7A, MathHelper.clamp(le.getHealth() / le.getMaxHealth(), 0f, 1f));
        }
        return Fx.color(COLOR.mode, e.getId() * 0.13f + now / 3000f);
    }

    static void draw(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity me, List<Entity> near, float td, long now) {
        if (!M.enabled || STYLE.mode == 1) return;
        if (SELF.bool && !Fx.firstPerson(mc)) one(ctx, me, td, now);
        for (Entity e : near) {
            if (TARGETS.mode == 0 && !(e instanceof PlayerEntity)) continue;
            if (e instanceof ArmorStandEntity) continue;
            if (me.squaredDistanceTo(e) > 48.0 * 48.0) continue;
            one(ctx, e, td, now);
        }
    }

    private static void one(DrawContext ctx, Entity e, float td, long now) {
        float[][] pts = Fx.PTS;
        if (!Fx.projectBox(Fx.lerped(e, td), pts)) return;
        Fx.rect(pts, Fx.RECT);
        float cx = (Fx.RECT[0] + Fx.RECT[2]) / 2f, cy = (Fx.RECT[1] + Fx.RECT[3]) / 2f;
        float rx = (Fx.RECT[2] - Fx.RECT[0]) / 2f * 0.9f, ry = (Fx.RECT[3] - Fx.RECT[1]) / 2f * 1.02f;
        if (rx < 2f || ry < 2f) return;
        float str = (float) STRENGTH.num / 100f;
        float size = (float) SIZE.num;

        for (int layer = 0; layer < 3; layer++) {
            float k = size * layer * 0.5f;
            float al = str * (layer == 0 ? 0.9f : layer == 1 ? 0.5f : 0.25f);
            int steps = 56;
            for (int i = 0; i < steps; i++) {
                float a = (float) (i * 2.0 * Math.PI / steps);
                int rgb;
                if (HEALTH.bool && e instanceof LivingEntity le) {
                    rgb = Fx.lerp(0xFF3B3B, 0x55FF7A, MathHelper.clamp(le.getHealth() / le.getMaxHealth(), 0f, 1f));
                } else rgb = Fx.color(COLOR.mode, i / (float) steps + now / 3000f);
                float x = cx + MathHelper.cos(a) * (rx + k);
                float y = cy + MathHelper.sin(a) * (ry + k);
                Fx.glowDot(ctx, x, y, rgb, al, 0);
            }
        }
    }
}
