package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Modules;
import dev.lumen.core.Setting;
import dev.lumen.util.Lang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayList;
import java.util.List;

/** Keybinds: список клавиш модулей на экране — включённые подсвечены, нажатая клавиша вспыхивает. */
public final class Keybinds {
    public static final Module M = new Module("Keybinds",
            "List of module keybinds", Category.VISUALS, false, true);
    static final Setting SHOW   = M.add(Setting.mode("show", "Show", 0, "All bound", "Only enabled"));
    static final Setting ADDON  = M.add(Setting.bool("addon", "Include addon keys", true));
    static final Setting TITLE  = M.add(Setting.bool("title", "Show title", true));
    static final Setting MAX    = M.add(Setting.num("max", "Max entries", 12, 4, 24, 1));
    static final Setting CORNER = M.add(Setting.mode("corner", "Corner", 3, "Top left", "Top right", "Bottom left", "Bottom right"));
    static final Setting OFF_X  = M.add(Setting.num("off_x", "Offset X", 4, 0, 400, 1));
    static final Setting OFF_Y  = M.add(Setting.num("off_y", "Offset Y", 4, 0, 400, 1));
    static final Setting ALPHA  = M.add(Setting.num("opacity", "Opacity (%)", 70, 20, 100, 5));

    private record Row(String name, String key, boolean on, boolean pressed) { }

    private Keybinds() { }

    private static String keyText(KeyBinding k) {
        return k.getBoundKeyLocalizedText().getString();
    }

    private static void collect(List<Row> out) {
        for (Module m : Modules.ALL) {
            if (m == M || m.key == null || m.key.isUnbound()) continue;
            if (SHOW.mode == 1 && !m.enabled) continue;
            out.add(new Row(Lang.t(m.name), keyText(m.key), m.enabled, m.key.isPressed()));
        }
        if (ADDON.bool) {
            if (ItemSwap.KEY != null && !ItemSwap.KEY.isUnbound()) {
                out.add(new Row(Lang.t("Item swap"), keyText(ItemSwap.KEY), true, ItemSwap.KEY.isPressed()));
            }
            if (LockSlot.KEY != null && !LockSlot.KEY.isUnbound()) {
                out.add(new Row(Lang.t("Lock slot"), keyText(LockSlot.KEY), true, LockSlot.KEY.isPressed()));
            }
        }
    }

    static void hud(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p) {
        if (!M.enabled) return;
        List<Row> rows = new ArrayList<>();
        collect(rows);
        if (rows.isEmpty()) return;
        // сначала включённые, затем по алфавиту
        rows.sort((a, b) -> a.on() != b.on() ? (a.on() ? -1 : 1) : a.name().compareToIgnoreCase(b.name()));
        int n = Math.min(rows.size(), (int) MAX.num);

        int nameW = 0, keyW = 0;
        for (int i = 0; i < n; i++) {
            nameW = Math.max(nameW, mc.textRenderer.getWidth(rows.get(i).name()));
            keyW = Math.max(keyW, mc.textRenderer.getWidth("[" + rows.get(i).key() + "]"));
        }
        String title = Lang.t("Keybinds");
        int w = Math.max(nameW + keyW + 16, mc.textRenderer.getWidth(title) + 10);
        int rowH = 11, h = n * rowH + (TITLE.bool ? 13 : 3) + 3;

        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        boolean left = CORNER.mode == 0 || CORNER.mode == 2, top = CORNER.mode <= 1;
        int x = left ? (int) OFF_X.num : sw - w - (int) OFF_X.num;
        int y = top ? (int) OFF_Y.num : sh - h - (int) OFF_Y.num;
        float a = (float) ALPHA.num / 100f;

        Fx.fill(ctx, x, y, x + w, y + h, 0x101018, a);
        Fx.fill(ctx, x, y, x + w, y + 1, 0xB07CE0, 1f);
        int cy = y + 3;
        if (TITLE.bool) {
            ctx.drawText(mc.textRenderer, title, x + 5, cy, 0xFFB07CE0, true);
            cy += 11;
        }
        for (int i = 0; i < n; i++) {
            Row r = rows.get(i);
            if (r.pressed()) Fx.fill(ctx, x + 1, cy - 1, x + w - 1, cy + rowH - 1, 0xB07CE0, 0.45f);
            Fx.fill(ctx, x + 2, cy, x + 4, cy + rowH - 2, r.on() ? 0x55FF7A : 0x666677, 1f);
            ctx.drawText(mc.textRenderer, r.name(), x + 7, cy + 1, r.on() ? 0xFFFFFFFF : 0xFF9A9AAA, true);
            String k = "[" + r.key() + "]";
            ctx.drawText(mc.textRenderer, k, x + w - 5 - mc.textRenderer.getWidth(k), cy + 1,
                    r.on() ? 0xFFFFD700 : 0xFF9A9AAA, true);
            cy += rowH;
        }
    }
}
