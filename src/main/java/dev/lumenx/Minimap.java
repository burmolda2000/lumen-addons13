package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.List;

/**
 * Minimap: мини-карта окрестностей (вид сверху по цветам блоков, как в Xaero/JourneyMap):
 * вращение по взгляду, квадрат или круг, игроки и мобы точками, стороны света, координаты.
 * Карта обновляется порциями по несколько рядов за кадр, поэтому не создаёт всплесков нагрузки.
 */
public final class Minimap {
    public static final Module M = new Module("Minimap",
            "Map of the area around you", Category.VISUALS, false, true);
    static final Setting RANGE   = M.add(Setting.num("range", "Range", 32, 16, 48, 4));
    static final Setting SCALE   = M.add(Setting.num("scale", "Scale", 2, 1, 4, 1));
    static final Setting SHAPE   = M.add(Setting.mode("shape", "Shape", 0, "Square", "Circle"));
    static final Setting ROTATE  = M.add(Setting.bool("rotate", "Rotate with player", true));
    static final Setting CORNER  = M.add(Setting.mode("corner", "Corner", 0, "Top right", "Top left", "Bottom right", "Bottom left"));
    static final Setting OFF_X   = M.add(Setting.num("off_x", "Offset X", 6, 0, 300, 1));
    static final Setting OFF_Y   = M.add(Setting.num("off_y", "Offset Y", 6, 0, 300, 1));
    static final Setting OPACITY = M.add(Setting.num("opacity", "Opacity (%)", 90, 30, 100, 5));
    static final Setting PLAYERS = M.add(Setting.bool("players", "Show players", true));
    static final Setting HOSTILE = M.add(Setting.bool("hostile", "Show hostile", true));
    static final Setting PASSIVE = M.add(Setting.bool("passive", "Show passive", false));
    static final Setting COMPASS = M.add(Setting.bool("compass", "Show compass", true));
    static final Setting COORDS  = M.add(Setting.bool("coords", "Show coordinates", true));

    private static int n;
    private static int[] shown = new int[0], next = new int[0], hNext = new int[0];
    private static int row, sOx, sOz, sPy, ox, oz;
    private static boolean have, cave;
    private static ClientWorld lastWorld;

    private Minimap() { }

    private static int shade(int rgb, float f) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * f));
        int g = Math.min(255, Math.round(((rgb >> 8) & 255) * f));
        int b = Math.min(255, Math.round((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    /** Один столбец карты: цвет верхнего блока и его высота. */
    private static void column(ClientWorld w, int x, int z, int idx, int r, BlockPos.Mutable mp) {
        int top = cave ? sPy + 1 : w.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1;
        int bottom = Math.max(w.getBottomY(), top - (cave ? 24 : 48));
        int color = 0, height = bottom;
        for (int y = top; y >= bottom; y--) {
            mp.set(x, y, z);
            BlockState st = w.getBlockState(mp);
            if (st.isAir()) continue;
            int mc = st.getMapColor(w, mp).color;
            if (mc == 0) continue;
            height = y;
            float f = 1.0f;
            if (r > 0) {
                int hn = hNext[idx - n];
                f = y > hn ? 1.18f : y < hn ? 0.82f : 1.0f;
            }
            color = 0xFF000000 | shade(mc, f);
            break;
        }
        next[idx] = color;
        hNext[idx] = height;
    }

    /** Обновляет часть рядов карты; когда проход закончен — публикует результат. */
    private static void refresh(ClientWorld w, ClientPlayerEntity p) {
        int range = (int) RANGE.num, size = range * 2;
        if (size != n || w != lastWorld) {
            n = size;
            shown = new int[n * n];
            next = new int[n * n];
            hNext = new int[n * n];
            row = 0;
            have = false;
            lastWorld = w;
        }
        if (row == 0) {
            sOx = (int) Math.floor(p.getX()) - range;
            sOz = (int) Math.floor(p.getZ()) - range;
            sPy = p.getBlockY();
            cave = w.getDimension().hasCeiling() || !w.isSkyVisible(p.getBlockPos());
        }
        BlockPos.Mutable mp = new BlockPos.Mutable();
        int perFrame = Math.max(3, n / 10);
        for (int k = 0; k < perFrame && row < n; k++, row++) {
            for (int c = 0; c < n; c++) column(w, sOx + c, sOz + row, row * n + c, row, mp);
        }
        if (row >= n) {
            System.arraycopy(next, 0, shown, 0, n * n);
            ox = sOx; oz = sOz;
            have = true;
            row = 0;
        }
    }

    static void hud(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p, float td, long now) {
        if (!M.enabled) { have = false; return; }
        ClientWorld w = mc.world;
        if (w == null) return;
        refresh(w, p);
        if (!have) return;

        int cell = (int) SCALE.num, half = n / 2, S = n * cell;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        boolean left = CORNER.mode == 1 || CORNER.mode == 3, top = CORNER.mode <= 1;
        int x0 = left ? (int) OFF_X.num : sw - S - (int) OFF_X.num;
        int y0 = top ? (int) OFF_Y.num : sh - S - (int) OFF_Y.num - (COORDS.bool ? 12 : 0);
        boolean circle = SHAPE.mode == 1;
        float op = (float) OPACITY.num / 100f;
        int alpha = (int) (op * 255f) << 24;

        Fx.fill(ctx, x0 - 1, y0 - 1, x0 + S + 1, y0 + S + 1, 0x000000, 0.55f);

        // направления: f — «вперёд» по карте, r — «вправо»
        float yawEff = ROTATE.bool ? p.getYaw(td) : 180f;
        double rad = Math.toRadians(yawEff);
        double fx = -Math.sin(rad), fz = Math.cos(rad), rx = -fz, rz = fx;
        double px = p.getX(), pz = p.getZ();

        for (int sy = 0; sy < n; sy++) {
            int runStart = 0, runColor = 0;
            for (int sx = 0; sx <= n; sx++) {
                int col = 0;
                if (sx < n) {
                    double dx = sx - half + 0.5, dy = sy - half + 0.5;
                    if (!(circle && dx * dx + dy * dy > (double) half * half)) {
                        double wx = px + fx * (-dy) + rx * dx, wz = pz + fz * (-dy) + rz * dx;
                        int gx = (int) Math.floor(wx) - ox, gz = (int) Math.floor(wz) - oz;
                        if (gx >= 0 && gx < n && gz >= 0 && gz < n) col = shown[gz * n + gx];
                    }
                }
                if (col != runColor || sx == n) {
                    if (runColor != 0) {
                        ctx.fill(x0 + runStart * cell, y0 + sy * cell, x0 + sx * cell, y0 + (sy + 1) * cell,
                                alpha | (runColor & 0xFFFFFF));
                    }
                    runStart = sx;
                    runColor = col;
                }
            }
        }

        int cx = x0 + S / 2, cy = y0 + S / 2;

        // существа
        if (PLAYERS.bool || HOSTILE.bool || PASSIVE.bool) {
            List<Entity> es = w.getOtherEntities(p, p.getBoundingBox().expand(n / 2.0),
                    e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());
            for (Entity e : es) {
                boolean pl = e instanceof PlayerEntity, ho = e instanceof HostileEntity;
                if (pl ? !PLAYERS.bool : ho ? !HOSTILE.bool : !PASSIVE.bool) continue;
                double rx0 = e.getX() - px, rz0 = e.getZ() - pz;
                double dx = rx0 * rx + rz0 * rz, dy = -(rx0 * fx + rz0 * fz);
                int ex = cx + (int) Math.round(dx * cell), ey = cy + (int) Math.round(dy * cell);
                if (Math.abs(ex - cx) > S / 2 - 2 || Math.abs(ey - cy) > S / 2 - 2) continue;
                if (circle && (double) (ex - cx) * (ex - cx) + (double) (ey - cy) * (ey - cy) > (S / 2.0 - 3) * (S / 2.0 - 3)) continue;
                int rgb = pl ? 0xFF3B3B : ho ? 0xFF9A3B : 0x55FF7A;
                Fx.fill(ctx, ex - 2, ey - 2, ex + 3, ey + 3, 0x000000, 0.8f);
                Fx.fill(ctx, ex - 1, ey - 1, ex + 2, ey + 2, rgb, 1f);
            }
        }

        // стрелка игрока
        double yawP = Math.toRadians(p.getYaw(td));
        double pfx = -Math.sin(yawP), pfz = Math.cos(yawP);
        double hx = pfx * rx + pfz * rz, hy = -(pfx * fx + pfz * fz);
        double ang = Math.atan2(hy, hx);
        float tipX = cx + (float) Math.cos(ang) * 6f, tipY = cy + (float) Math.sin(ang) * 6f;
        float lX = cx + (float) Math.cos(ang + 2.5) * 4.5f, lY = cy + (float) Math.sin(ang + 2.5) * 4.5f;
        float rX = cx + (float) Math.cos(ang - 2.5) * 4.5f, rY = cy + (float) Math.sin(ang - 2.5) * 4.5f;
        Fx.thinLine(ctx, tipX, tipY, lX, lY, 0xFFFFFF, 1f);
        Fx.thinLine(ctx, tipX, tipY, rX, rY, 0xFFFFFF, 1f);
        Fx.thinLine(ctx, lX, lY, rX, rY, 0xFFFFFF, 0.8f);

        // рамка
        int bc = 0xB07CE0;
        if (!circle) {
            Fx.fill(ctx, x0 - 1, y0 - 1, x0 + S + 1, y0, bc, 1f);
            Fx.fill(ctx, x0 - 1, y0 + S, x0 + S + 1, y0 + S + 1, bc, 1f);
            Fx.fill(ctx, x0 - 1, y0, x0, y0 + S, bc, 1f);
            Fx.fill(ctx, x0 + S, y0, x0 + S + 1, y0 + S, bc, 1f);
        } else {
            for (int i = 0; i < 96; i++) {
                double a = i * Math.PI * 2.0 / 96.0;
                int bx = cx + (int) Math.round(Math.cos(a) * (S / 2.0)), by = cy + (int) Math.round(Math.sin(a) * (S / 2.0));
                Fx.fill(ctx, bx - 1, by - 1, bx + 1, by + 1, bc, 1f);
            }
        }

        // стороны света
        if (COMPASS.bool) {
            double rr = S / 2.0 - 7;
            Object[][] dirs = { { "N", 0.0, -1.0, 0xFF5555 }, { "E", 1.0, 0.0, 0xFFFFFF }, { "S", 0.0, 1.0, 0xFFFFFF }, { "W", -1.0, 0.0, 0xFFFFFF } };
            for (Object[] d : dirs) {
                double nx = (Double) d[1], nz = (Double) d[2];
                double dx = nx * rx + nz * rz, dy = -(nx * fx + nz * fz);
                int lx = cx + (int) Math.round(dx * rr), ly = cy + (int) Math.round(dy * rr);
                ctx.drawText(mc.textRenderer, (String) d[0], lx - 3, ly - 4, 0xFF000000 | (Integer) d[3], true);
            }
        }

        if (COORDS.bool) {
            String t = p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ();
            ctx.drawText(mc.textRenderer, t, x0 + S / 2 - mc.textRenderer.getWidth(t) / 2, y0 + S + 3, 0xFFFFFFFF, true);
        }
    }
}
