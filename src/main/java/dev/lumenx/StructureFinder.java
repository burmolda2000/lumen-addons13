package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Structure finder: ищет признаки структур в уже загруженных чанках (спавнеры, порталы крепости,
 * древний город, испытательные камеры, бастионы/крепости Незера, монумент, город Энда, деревни...).
 * Работает по данным, которые сервер уже прислал клиенту; дальние чанки и скрытые античитом блоки не видны.
 */
public final class StructureFinder {
    public static final Module M = new Module("Structure finder",
            "Finds spawners and structures in loaded chunks", Category.VISUALS, false, true);
    static final Setting LANG     = M.add(Setting.mode("lang", "Language", 0, "Russian", "English"));
    static final Setting SPAWNER  = M.add(Setting.bool("spawner", "Spawners", true));
    static final Setting STRONG   = M.add(Setting.bool("stronghold", "Stronghold portal", true));
    static final Setting ANCIENT  = M.add(Setting.bool("ancient", "Ancient city", true));
    static final Setting TRIAL    = M.add(Setting.bool("trial", "Trial chamber", true));
    static final Setting NETHER   = M.add(Setting.bool("nether", "Fortress / bastion", true));
    static final Setting MONUMENT = M.add(Setting.bool("monument", "Ocean monument", true));
    static final Setting ENDCITY  = M.add(Setting.bool("endcity", "End city", true));
    static final Setting VILLAGE  = M.add(Setting.bool("village", "Village", true));
    static final Setting TEMPLE   = M.add(Setting.bool("temple", "Temples (trap)", false));
    static final Setting RUINED   = M.add(Setting.bool("ruined", "Ruined portal", false));
    static final Setting RADIUS   = M.add(Setting.num("radius", "Scan radius (chunks)", 8, 2, 16, 1));
    static final Setting MAXD     = M.add(Setting.num("max_dist", "Max distance", 160, 32, 400, 16));
    static final Setting BOXES    = M.add(Setting.bool("boxes", "Boxes", true));
    static final Setting TRACERS  = M.add(Setting.bool("tracers", "Tracers", true));
    static final Setting LIST     = M.add(Setting.bool("list", "Show list", true));
    static final Setting CHAT     = M.add(Setting.bool("chat", "Chat notify", true));

    // тип: {английское имя, русское имя, цвет, радиус склейки в блоках}
    private static final String[][] NAMES = {
            {"Spawner", "Спавнер", "FF4444", "0"},
            {"Stronghold portal", "Портал крепости", "55FF55", "40"},
            {"Ancient city", "Древний город", "55E0FF", "48"},
            {"Trial chamber", "Испытательная камера", "FF8A00", "0"},
            {"Nether fortress/bastion", "Крепость/бастион Незера", "FF6A3B", "48"},
            {"Ocean monument", "Океанский монумент", "55E0FF", "48"},
            {"End city", "Город Энда", "B07CE0", "48"},
            {"Village", "Деревня", "FFFF55", "48"},
            {"Temple trap", "Храм (ловушка)", "FFD700", "24"},
            {"Ruined portal", "Разрушенный портал", "FF7AD9", "24"}
    };
    private static final Map<Block, Integer> TYPE = new HashMap<>();
    static {
        TYPE.put(Blocks.SPAWNER, 0);
        TYPE.put(Blocks.END_PORTAL_FRAME, 1);
        TYPE.put(Blocks.END_PORTAL, 1);
        TYPE.put(Blocks.REINFORCED_DEEPSLATE, 2);
        TYPE.put(Blocks.TRIAL_SPAWNER, 3);
        TYPE.put(Blocks.VAULT, 3);
        TYPE.put(Blocks.NETHER_BRICK_FENCE, 4);
        TYPE.put(Blocks.GILDED_BLACKSTONE, 4);
        TYPE.put(Blocks.SEA_LANTERN, 5);
        TYPE.put(Blocks.PURPUR_PILLAR, 6);
        TYPE.put(Blocks.BELL, 7);
        TYPE.put(Blocks.TNT, 8);
        TYPE.put(Blocks.TRIPWIRE_HOOK, 8);
        TYPE.put(Blocks.CRYING_OBSIDIAN, 9);
    }

    private record Hit(int x, int y, int z, int type) { }
    private static final class Cluster {
        int type, n; double sx, sy, sz;
        double x() { return sx / n; }
        double y() { return sy / n; }
        double z() { return sz / n; }
    }

    private static final Map<Long, List<Hit>> HITS = new HashMap<>();
    private static final List<Cluster> CLUSTERS = new ArrayList<>();
    private static final Set<String> NOTIFIED = new HashSet<>();
    private static ClientWorld world;
    private static int scanIdx;
    private static long lastCluster;
    private static final float[] T = new float[3];

    private StructureFinder() { }

    private static boolean enabled(int type) {
        return switch (type) {
            case 0 -> SPAWNER.bool;
            case 1 -> STRONG.bool;
            case 2 -> ANCIENT.bool;
            case 3 -> TRIAL.bool;
            case 4 -> NETHER.bool;
            case 5 -> MONUMENT.bool;
            case 6 -> ENDCITY.bool;
            case 7 -> VILLAGE.bool;
            case 8 -> TEMPLE.bool;
            default -> RUINED.bool;
        };
    }

    private static boolean isIndicator(BlockState st) {
        return TYPE.containsKey(st.getBlock());
    }

    /** Сканирование чанков по кругу: несколько чанков за тик. */
    static void update(ClientWorld w, ClientPlayerEntity p, long now) {
        if (!M.enabled) {
            if (!HITS.isEmpty()) { HITS.clear(); CLUSTERS.clear(); NOTIFIED.clear(); }
            return;
        }
        if (w != world) { world = w; HITS.clear(); CLUSTERS.clear(); NOTIFIED.clear(); scanIdx = 0; }

        int r = (int) RADIUS.num;
        ChunkPos cp = p.getChunkPos();
        int side = r * 2 + 1, total = side * side;
        for (int k = 0; k < 14; k++) {
            int idx = scanIdx++ % total;
            int cx = cp.x - r + idx % side, cz = cp.z - r + idx / side;
            scanChunk(w, cx, cz);
        }
        // убрать чанки далеко от игрока
        HITS.keySet().removeIf(key -> {
            int hx = (int) (key >> 32), hz = (int) (long) key;
            return Math.abs(hx - cp.x) > r + 2 || Math.abs(hz - cp.z) > r + 2;
        });

        if (now - lastCluster > 700) { lastCluster = now; rebuild(p); }
    }

    private static void scanChunk(ClientWorld w, int cx, int cz) {
        long key = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
        WorldChunk ch = w.getChunkManager().getWorldChunk(cx, cz);
        if (ch == null) { HITS.remove(key); return; }
        ChunkSection[] sections = ch.getSectionArray();
        List<Hit> out = null;
        for (int i = 0; i < sections.length; i++) {
            ChunkSection s = sections[i];
            if (s == null || s.isEmpty() || !s.hasAny(StructureFinder::isIndicator)) continue;
            int baseY = ch.getBottomY() + i * 16;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        Integer t = TYPE.get(s.getBlockState(x, y, z).getBlock());
                        if (t == null) continue;
                        if (out == null) out = new ArrayList<>();
                        out.add(new Hit(cx * 16 + x, baseY + y, cz * 16 + z, t));
                    }
                }
            }
        }
        if (out == null) HITS.remove(key); else HITS.put(key, out);
    }

    /** Склеивает близкие находки одного типа в один маркер. */
    private static void rebuild(ClientPlayerEntity p) {
        CLUSTERS.clear();
        for (List<Hit> list : HITS.values()) {
            for (Hit h : list) {
                if (!enabled(h.type())) continue;
                int merge = Integer.parseInt(NAMES[h.type()][3]);
                Cluster found = null;
                if (merge > 0) {
                    for (Cluster c : CLUSTERS) {
                        if (c.type != h.type()) continue;
                        double dx = c.x() - h.x(), dy = c.y() - h.y(), dz = c.z() - h.z();
                        if (dx * dx + dy * dy + dz * dz <= (double) merge * merge) { found = c; break; }
                    }
                }
                if (found == null) { found = new Cluster(); found.type = h.type(); CLUSTERS.add(found); }
                found.n++;
                found.sx += h.x() + 0.5; found.sy += h.y() + 0.5; found.sz += h.z() + 0.5;
            }
        }
        CLUSTERS.sort((a, b) -> Double.compare(dist2(p, a), dist2(p, b)));

        if (CHAT.bool) {
            boolean ru = LANG.mode == 0;
            for (Cluster c : CLUSTERS) {
                double d = Math.sqrt(dist2(p, c));
                if (d > MAXD.num) continue;
                String key = c.type + ":" + Math.round(c.x() / 24) + ":" + Math.round(c.y() / 24) + ":" + Math.round(c.z() / 24);
                if (NOTIFIED.add(key)) {
                    p.sendMessage(Text.literal("[Structures] " + NAMES[c.type][ru ? 1 : 0] + " — "
                            + Math.round(c.x()) + " " + Math.round(c.y()) + " " + Math.round(c.z()) + " (" + Math.round(d) + "m)"), false);
                }
            }
        }
    }

    private static double dist2(ClientPlayerEntity p, Cluster c) {
        double dx = c.x() - p.getX(), dy = c.y() - p.getY(), dz = c.z() - p.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /** Боксы, подписи и линии к находкам (нужна проекция). */
    static void draw(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p, long now) {
        if (!M.enabled || CLUSTERS.isEmpty()) return;
        boolean ru = LANG.mode == 0;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        double md2 = MAXD.num * MAXD.num;
        int shown = 0;
        for (Cluster c : CLUSTERS) {
            if (shown >= 40) break;
            double d2 = dist2(p, c);
            if (d2 > md2) continue;
            shown++;
            int rgb = Integer.parseInt(NAMES[c.type][2], 16);
            double r = c.n > 1 ? 1.5 : 0.5;
            Box b = new Box(c.x() - r, c.y() - r, c.z() - r, c.x() + r, c.y() + r, c.z() + r);
            if (BOXES.bool && Fx.projectBox(b, Fx.PTS)) Fx.boxEdges(ctx, Fx.PTS, rgb, 0.9f);
            if (Fx.to(c.x(), c.y() + r + 0.4, c.z(), T)) {
                String label = NAMES[c.type][ru ? 1 : 0] + (c.n > 1 ? " x" + c.n : "") + "  " + Math.round(Math.sqrt(d2)) + "m";
                int tw = mc.textRenderer.getWidth(label);
                ctx.drawText(mc.textRenderer, label, Math.round(T[0] - tw / 2f), Math.round(T[1]) - 4, 0xFF000000 | rgb, true);
                if (TRACERS.bool) Fx.thinLine(ctx, sw / 2f, sh / 2f, T[0], T[1], rgb, 0.45f);
            }
        }
    }

    /** Список ближайших находок со списком координат. */
    static void hud(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p) {
        if (!M.enabled || !LIST.bool || CLUSTERS.isEmpty()) return;
        boolean ru = LANG.mode == 0;
        double md2 = MAXD.num * MAXD.num;
        int y = 120, rows = 0;
        for (Cluster c : CLUSTERS) {
            if (rows >= 6) break;
            double d2 = dist2(p, c);
            if (d2 > md2) continue;
            int rgb = Integer.parseInt(NAMES[c.type][2], 16);
            String t = NAMES[c.type][ru ? 1 : 0] + (c.n > 1 ? " x" + c.n : "") + "  "
                    + Math.round(c.x()) + " " + Math.round(c.y()) + " " + Math.round(c.z()) + "  " + Math.round(Math.sqrt(d2)) + "m";
            ctx.drawText(mc.textRenderer, t, 4, y + rows * 10, 0xFF000000 | rgb, true);
            rows++;
        }
    }
}
