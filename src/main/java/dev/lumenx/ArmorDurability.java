package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

/** Armor durability: прочность брони цветом — целая зелёная, половина жёлтая, почти сломанная красная. */
public final class ArmorDurability {
    public static final Module M = new Module("Armor durability",
            "Armor condition by colour: green, yellow, red", Category.VISUALS, false, true);
    static final Setting LAYOUT  = M.add(Setting.mode("layout", "Layout", 0, "Vertical", "Horizontal"));
    static final Setting ANCHOR  = M.add(Setting.mode("anchor", "Anchor", 0, "Left", "Right"));
    static final Setting PERCENT = M.add(Setting.bool("percent", "Show percent", true));
    static final Setting BAR     = M.add(Setting.bool("bar", "Show bar", true));
    static final Setting ICONS   = M.add(Setting.bool("icons", "Show icons", true));
    static final Setting OFF_X   = M.add(Setting.num("off_x", "Offset X", 6, 0, 400, 1));
    static final Setting OFF_Y   = M.add(Setting.num("off_y", "Offset Y", 0, -200, 200, 1));

    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    private ArmorDurability() { }

    /** 100% — зелёный, 50% — жёлтый, 0% — красный. */
    static int colorFor(float pct) {
        return Fx.hsv(MathHelper.clamp(pct, 0f, 1f) / 3f, 0.85f, 1f);
    }

    static void hud(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p) {
        if (!M.enabled) return;
        boolean vertical = LAYOUT.mode == 0;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int cellW = 40, cellH = 20;
        int count = 0;
        for (EquipmentSlot s : SLOTS) if (!p.getEquippedStack(s).isEmpty()) count++;
        if (count == 0) return;

        int totalW = vertical ? cellW : cellW * count;
        int totalH = vertical ? cellH * count : cellH;
        int x0 = ANCHOR.mode == 0 ? (int) OFF_X.num : sw - totalW - (int) OFF_X.num;
        int y0 = (vertical ? sh / 2 - totalH / 2 : sh - 60) + (int) OFF_Y.num;

        int i = 0;
        for (EquipmentSlot s : SLOTS) {
            ItemStack st = p.getEquippedStack(s);
            if (st.isEmpty()) continue;
            int x = vertical ? x0 : x0 + i * cellW;
            int y = vertical ? y0 + i * cellH : y0;
            i++;

            boolean dmg = st.isDamageable();
            float pct = dmg ? 1f - st.getDamage() / (float) Math.max(1, st.getMaxDamage()) : 1f;
            int rgb = dmg ? colorFor(pct) : 0x55E0FF;

            Fx.fill(ctx, x - 1, y - 1, x + cellW - 3, y + cellH - 3, 0x101018, 0.55f);
            int tx = x + 1;
            if (ICONS.bool) { ctx.drawItem(st, x, y); tx = x + 18; }
            if (PERCENT.bool) {
                String t = dmg ? Math.round(pct * 100f) + "%" : "inf";
                ctx.drawText(mc.textRenderer, t, tx, y + 4, 0xFF000000 | rgb, true);
            }
            if (BAR.bool) {
                int bw = cellW - 5;
                Fx.fill(ctx, x, y + cellH - 6, x + bw, y + cellH - 4, 0x000000, 0.7f);
                Fx.fill(ctx, x, y + cellH - 6, x + Math.round(bw * pct), y + cellH - 4, rgb, 0.95f);
            }
        }
    }
}
