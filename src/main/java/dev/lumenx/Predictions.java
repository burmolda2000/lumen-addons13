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
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Predictions: для летящего предмета (жемчуг, снежок, зелье, стрела...) считает точку падения,
 * ставит метку в мире и показывает отдельное окно: расстояние, время и вид сверху на место падения.
 */
public final class Predictions {
    public static final Module M = new Module("Predictions",
            "Shows where flying projectiles will land", Category.VISUALS, false, true);
    static final Setting TRACK  = M.add(Setting.mode("track", "Track", 0, "Ender pearls", "All throwables"));
    static final Setting OTHERS = M.add(Setting.bool("others", "Enemy projectiles", true));
    static final Setting WINDOW = M.add(Setting.bool("window", "Window", true));
    static final Setting MARKER = M.add(Setting.bool("marker", "World marker", true));
    static final Setting PATH   = M.add(Setting.bool("path", "Path line", true));
    static final Setting ZOOM   = M.add(Setting.num("zoom", "Window zoom", 4, 3, 8, 1));
    static final Setting OFF_X  = M.add(Setting.num("off_x", "Offset X", 0, 0, 400, 1));
    static final Setting OFF_Y  = M.add(Setting.num("off_y", "Offset Y", 0, 0, 300, 1));

    private static final Set<String> PEARL = Set.of("ender_pearl");
    private static final Set<String> ALL = Set.of("ender_pearl", "snowball", "egg", "splash_potion",
            "lingering_potion", "potion", "experience_bottle", "arrow", "spectral_arrow", "trident");

    private static final class Pred {
        Entity e; boolean mine; String id; Vec3d land; int ticks; List<Vec3d> path = new ArrayList<>();
    }

    private static final List<Pred> CUR = new ArrayList<>();
    private static long computedAt;
    private static int[] colors = new int[0];
    private static BlockPos colPos;
    private static long colAt;
    private static final float[] T = new float[3];

    private Predictions() { }

    private static String idOf(Entity e) {
        return Registries.ENTITY_TYPE.getId(e.getType()).getPath();
    }

    private static double gravity(String id) {
        return switch (id) {
            case "ender_pearl", "snowball", "egg" -> 0.03;
            case "experience_bottle" -> 0.07;
            default -> 0.05;
        };
    }

    private static boolean accepted(Entity e) {
        if (!(e instanceof ProjectileEntity)) return false;
        if (e.getVelocity().lengthSquared() < 0.01) return false;
        String id = idOf(e);
        return (TRACK.mode == 0 ? PEARL : ALL).contains(id);
    }

    private static Pred simulate(ClientWorld w, Entity e, boolean mine) {
        Box b = e.getBoundingBox();
        Vec3d pos = new Vec3d((b.minX + b.maxX) / 2.0, (b.minY + b.maxY) / 2.0, (b.minZ + b.maxZ) / 2.0);
        Vec3d v = e.getVelocity();
        String id = idOf(e);
        double g = gravity(id);
        boolean arrow = id.contains("arrow") || id.equals("trident");
        Pred pr = new Pred();
        pr.e = e; pr.mine = mine; pr.id = id;
        pr.path.add(pos);
        for (int i = 0; i < 160; i++) {
            Vec3d next = pos.add(v);
            BlockHitResult hit = w.raycast(new RaycastContext(pos, next,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, e));
            if (hit.getType() != HitResult.Type.MISS) {
                pr.land = hit.getPos();
                pr.ticks = i + 1;
                pr.path.add(pr.land);
                return pr;
            }
            pos = next;
            pr.path.add(pos);
            boolean water = w.getFluidState(BlockPos.ofFloored(pos)).isIn(net.minecraft.registry.tag.FluidTags.WATER);
            v = v.multiply(water ? (arrow ? 0.6 : 0.8) : 0.99).subtract(0, g, 0);
            if (pos.y < w.getBottomY() - 16) return null;
        }
        return null;
    }

    /** Считает предсказания и рисует метки в мире (нужна проекция). */
    static void world(DrawContext ctx, MinecraftClient mc, ClientWorld w, ClientPlayerEntity p, float td, long now) {
        if (!M.enabled) { CUR.clear(); return; }
        CUR.clear();
        List<Entity> found = new ArrayList<>(w.getOtherEntities(p, p.getBoundingBox().expand(96.0), Predictions::accepted));
        found.sort((a, b) -> Double.compare(
                (isMine(a, p) ? -1e9 : 0) + a.squaredDistanceTo(p), (isMine(b, p) ? -1e9 : 0) + b.squaredDistanceTo(p)));
        int n = 0;
        for (Entity e : found) {
            if (n >= 3) break;
            boolean mine = isMine(e, p);
            if (!mine && !OTHERS.bool) continue;
            Pred pr = simulate(w, e, mine);
            if (pr != null) { CUR.add(pr); n++; }
        }
        computedAt = now;

        for (Pred pr : CUR) {
            int rgb = pr.mine ? 0x55E0FF : 0xFF4444;
            if (PATH.bool) {
                for (int i = 0; i < pr.path.size(); i += 2) {
                    Vec3d q = pr.path.get(i);
                    if (Fx.to(q.x, q.y, q.z, T)) Fx.fill(ctx, Math.round(T[0]), Math.round(T[1]), Math.round(T[0]) + 2, Math.round(T[1]) + 2, rgb, 0.6f);
                }
            }
            if (MARKER.bool) {
                Vec3d l = pr.land;
                boolean prev = false;
                float px = 0, py = 0;
                for (int i = 0; i <= 24; i++) {
                    double th = 2.0 * Math.PI * i / 24.0;
                    boolean ok = Fx.to(l.x + Math.cos(th) * 0.5, l.y + 0.05, l.z + Math.sin(th) * 0.5, T);
                    if (ok && prev) Fx.thinLine(ctx, px, py, T[0], T[1], rgb, 0.95f);
                    px = T[0]; py = T[1]; prev = ok;
                }
                if (Fx.to(l.x, l.y + 0.6, l.z, T)) {
                    String label = pr.id.replace('_', ' ') + "  " + Math.round(Math.sqrt(p.squaredDistanceTo(l.x, l.y, l.z))) + "m  "
                            + String.format("%.1fs", pr.ticks / 20f);
                    int tw = mc.textRenderer.getWidth(label);
                    ctx.drawText(mc.textRenderer, label, Math.round(T[0] - tw / 2f), Math.round(T[1]) - 8, 0xFF000000 | rgb, true);
                }
            }
        }
    }

    private static boolean isMine(Entity e, ClientPlayerEntity p) {
        return e instanceof ProjectileEntity pe && pe.getOwner() == p;
    }

    private static int shade(int rgb, float f) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 255) * f));
        int g = Math.min(255, Math.round(((rgb >> 8) & 255) * f));
        int b = Math.min(255, Math.round((rgb & 255) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Отдельное окно в углу: вид сверху на место падения + расстояние и время. */
    static void window(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p, long now) {
        if (!M.enabled || !WINDOW.bool || CUR.isEmpty() || now - computedAt > 300) return;
        ClientWorld w = mc.world;
        if (w == null) return;
        Pred pr = CUR.get(0);
        int zoom = (int) ZOOM.num;
        int ww = 136, mapW = ww - 4;
        int cols = mapW / zoom, rows = Math.max(8, 72 / zoom);
        int mapH = rows * zoom, wh = mapH + 16;
        int sw = ctx.getScaledWindowWidth();
        int x = sw - ww - 6 - (int) OFF_X.num, y = 6 + (int) OFF_Y.num;
        int rgb = pr.mine ? 0x55E0FF : 0xFF4444;

        Fx.fill(ctx, x - 1, y - 1, x + ww + 1, y + wh + 1, rgb, 0.9f);
        Fx.fill(ctx, x, y, x + ww, y + wh, 0x101018, 0.92f);

        String title = pr.id.equals("ender_pearl") ? "Pearl Cam" : pr.id.replace('_', ' ') + " cam";
        ctx.drawText(mc.textRenderer, title, x + 3, y + 3, 0xFFFFFFFF, true);
        String right = Math.round(Math.sqrt(p.squaredDistanceTo(pr.land.x, pr.land.y, pr.land.z))) + " m  "
                + String.format("%.1f s", pr.ticks / 20f);
        ctx.drawText(mc.textRenderer, right, x + ww - 3 - mc.textRenderer.getWidth(right), y + 3, 0xFF000000 | rgb, true);

        BlockPos lp = BlockPos.ofFloored(pr.land);
        if (colors.length != cols * rows || !lp.equals(colPos) || now - colAt > 400) {
            colors = new int[cols * rows];
            colPos = lp;
            colAt = now;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int bx = lp.getX() + c - cols / 2, bz = lp.getZ() + r - rows / 2;
                    int col = 0xFF202028;
                    for (int by = lp.getY() + 8; by >= lp.getY() - 10; by--) {
                        BlockPos bp = new BlockPos(bx, by, bz);
                        BlockState st = w.getBlockState(bp);
                        if (st.isAir()) continue;
                        int mcol = st.getMapColor(w, bp).color;
                        if (mcol == 0) continue;
                        col = shade(mcol, MathHelper.clamp(1f + (by - lp.getY()) * 0.045f, 0.55f, 1.3f));
                        break;
                    }
                    colors[r * cols + c] = col;
                }
            }
        }
        int mx = x + 2, my = y + 14;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                ctx.fill(mx + c * zoom, my + r * zoom, mx + (c + 1) * zoom, my + (r + 1) * zoom, colors[r * cols + c]);
            }
        }
        double x0 = lp.getX() - cols / 2, z0 = lp.getZ() - rows / 2;
        // путь
        for (int i = 0; i < pr.path.size(); i += 2) {
            Vec3d q = pr.path.get(i);
            int sx = mx + (int) ((q.x - x0) * zoom), sy = my + (int) ((q.z - z0) * zoom);
            if (sx >= mx && sx < mx + mapW && sy >= my && sy < my + mapH) Fx.fill(ctx, sx, sy, sx + 2, sy + 2, 0xFFFFFF, 0.8f);
        }
        // игрок
        int psx = mx + (int) ((p.getX() - x0) * zoom), psy = my + (int) ((p.getZ() - z0) * zoom);
        if (psx >= mx && psx < mx + mapW && psy >= my && psy < my + mapH) Fx.fill(ctx, psx - 1, psy - 1, psx + 2, psy + 2, 0xFFFFFF, 1f);
        // точка падения
        int lx = mx + (int) ((pr.land.x - x0) * zoom), ly = my + (int) ((pr.land.z - z0) * zoom);
        Fx.thinLine(ctx, lx - 6, ly, lx + 6, ly, rgb, 1f);
        Fx.thinLine(ctx, lx, ly - 6, lx, ly + 6, rgb, 1f);
        Fx.glowDot(ctx, lx, ly, rgb, 1f, 1);
    }
}
