package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

import java.util.List;

/** Health indicator: сердечки над видимыми игроками: здоровье и золотые сердца поглощения. */
public final class HealthIndicator {
    public static final Module M = new Module("Health indicator",
            "Hearts of visible players", Category.VISUALS, false, true);
    static final Setting TARGETS = M.add(Setting.mode("targets", "Targets", 0, "Players", "All living"));
    static final Setting STYLE   = M.add(Setting.mode("style", "Style", 0, "Hearts", "Number", "Both"));
    static final Setting ABSORB  = M.add(Setting.bool("absorb", "Show absorption", true));
    static final Setting SELF    = M.add(Setting.bool("self", "Show yourself", false));
    static final Setting SIZE    = M.add(Setting.num("size", "Size", 1, 1, 3, 1));
    static final Setting MAXD    = M.add(Setting.num("max_dist", "Max distance", 64, 16, 128, 8));

    private static final String[] HEART = { ".XX.XX.", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X..." };
    private static final float[] A = new float[3];

    private HealthIndicator() { }

    static void draw(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity me, List<Entity> near, float td, long now) {
        if (!M.enabled) return;
        if (SELF.bool && !Fx.firstPerson(mc)) one(ctx, mc, me, td);
        double md2 = MAXD.num * MAXD.num;
        for (Entity e : near) {
            if (!(e instanceof LivingEntity le) || e instanceof ArmorStandEntity) continue;
            if (TARGETS.mode == 0 && !(e instanceof PlayerEntity)) continue;
            if (e.isInvisible()) continue;   // невидимых игроков не раскрываем
            if (me.squaredDistanceTo(e) > md2) continue;
            one(ctx, mc, le, td);
        }
    }

    private static void one(DrawContext ctx, MinecraftClient mc, LivingEntity e, float td) {
        Box b = Fx.lerped(e, td);
        double cx = (b.minX + b.maxX) / 2.0, cz = (b.minZ + b.maxZ) / 2.0;
        if (!Fx.to(cx, b.maxY + 0.2, cz, A)) return;

        int s = (int) SIZE.num;
        float hp = Math.max(0f, e.getHealth()), max = Math.max(1f, e.getMaxHealth());
        float abs = ABSORB.bool ? e.getAbsorptionAmount() : 0f;
        boolean hearts = STYLE.mode != 1, number = STYLE.mode != 0;

        int cell = 8 * s, rowH = 7 * s + 1;
        int total = Math.min(20, (int) Math.ceil(max / 2f));
        int perRow = 10;
        int rows = (total + perRow - 1) / perRow;
        int absHearts = Math.min(10, (int) Math.ceil(abs / 2f));

        int bottomY = Math.round(A[1]);
        int y = bottomY;
        int cxs = Math.round(A[0]);

        if (hearts) {
            // нижний ряд — здоровье (идём снизу вверх), над ним — поглощение
            for (int r = rows - 1; r >= 0; r--) {
                int inRow = Math.min(perRow, total - r * perRow);
                int w = inRow * cell - s;
                y -= rowH;
                for (int i = 0; i < inRow; i++) {
                    float rem = hp - (r * perRow + i) * 2f;
                    int state = rem >= 1.5f ? 2 : rem >= 0.5f ? 1 : 0;
                    heart(ctx, cxs - w / 2 + i * cell, y, s, state, 0xFF3B3B);
                }
            }
            if (absHearts > 0) {
                int w = absHearts * cell - s;
                y -= rowH;
                for (int i = 0; i < absHearts; i++) {
                    float rem = abs - i * 2f;
                    int state = rem >= 1.5f ? 2 : rem >= 0.5f ? 1 : 0;
                    heart(ctx, cxs - w / 2 + i * cell, y, s, state, 0xFFD700);
                }
            }
        }
        if (number) {
            String t = String.format("%.1f", hp) + (abs > 0.01f ? " +" + String.format("%.1f", abs) : "");
            int tw = mc.textRenderer.getWidth(t);
            y -= 11;
            ctx.drawText(mc.textRenderer, t, cxs - tw / 2, y, abs > 0.01f ? 0xFFFFD700 : 0xFFFF5555, true);
        }
    }

    /** state: 0 — пустое, 1 — половина, 2 — целое. */
    private static void heart(DrawContext ctx, int x, int y, int s, int state, int rgb) {
        for (int r = 0; r < 6; r++) {          // тёмная обводка
            for (int c = 0; c < 7; c++) {
                if (HEART[r].charAt(c) == 'X') Fx.fill(ctx, x + c * s - 1, y + r * s - 1, x + (c + 1) * s + 1, y + (r + 1) * s + 1, 0x000000, 0.55f);
            }
        }
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) {
                if (HEART[r].charAt(c) != 'X') continue;
                boolean filled = state == 2 || (state == 1 && c < 4);
                Fx.fill(ctx, x + c * s, y + r * s, x + (c + 1) * s, y + (r + 1) * s, filled ? rgb : 0x3A2A32, 1f);
            }
        }
        if (state > 0) Fx.fill(ctx, x + s, y + s, x + 2 * s, y + 2 * s, 0xFFFFFF, 0.55f);   // блик
    }
}
