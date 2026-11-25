package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Custom enchant: свой формат и цвета зачарований предмета в руке и брони (на экране). */
public final class CustomEnchant {
    public static final Module M = new Module("Custom enchant",
            "Custom display of item enchantments", Category.VISUALS, false, true);
    static final Setting SHOW     = M.add(Setting.mode("show", "Show on", 2, "Held item", "Armor", "Both"));
    static final Setting FORMAT   = M.add(Setting.mode("format", "Format", 0, "Short", "Full", "Level only"));
    static final Setting LANG     = M.add(Setting.mode("lang", "Language", 0, "Russian", "English"));
    static final Setting COLORING = M.add(Setting.mode("coloring", "Coloring", 0, "By level", "Single"));
    static final Setting SINGLE   = M.add(Setting.mode("single", "Single color", 5, Fx.PAL));
    static final Setting MAXMARK  = M.add(Setting.bool("maxmark", "Mark max level", true));
    static final Setting ICONS    = M.add(Setting.bool("icons", "Show item icon", true));
    static final Setting BG       = M.add(Setting.bool("bg", "Background", true));
    static final Setting ANCHOR   = M.add(Setting.mode("anchor", "Anchor", 0, "Right", "Left"));
    static final Setting OFF_X    = M.add(Setting.num("off_x", "Offset X", 4, 0, 400, 1));
    static final Setting OFF_Y    = M.add(Setting.num("off_y", "Offset Y", 0, -200, 200, 1));

    private static final Map<String, String[]> SHORT = new HashMap<>();
    static {
        String[][] d = {
                {"sharpness", "Sharp", "Остр"}, {"smite", "Smite", "Небес"}, {"bane_of_arthropods", "Bane", "Членист"},
                {"knockback", "KB", "Отбр"}, {"fire_aspect", "Fire", "Поджог"}, {"looting", "Loot", "Добыча"},
                {"sweeping_edge", "Sweep", "Размах"}, {"efficiency", "Eff", "Эфф"}, {"silk_touch", "Silk", "Шёлк"},
                {"unbreaking", "Unb", "Проч"}, {"fortune", "Fort", "Удача"}, {"mending", "Mend", "Починка"},
                {"protection", "Prot", "Защита"}, {"fire_protection", "FireP", "Огонь"}, {"blast_protection", "Blast", "Взрыв"},
                {"projectile_protection", "Proj", "Снаряд"}, {"feather_falling", "Feather", "Падение"}, {"thorns", "Thorns", "Шипы"},
                {"respiration", "Resp", "Дыхан"}, {"aqua_affinity", "Aqua", "Сродство"}, {"depth_strider", "Depth", "Подвод"},
                {"frost_walker", "Frost", "Лёд"}, {"soul_speed", "SoulSp", "Души"}, {"swift_sneak", "Sneak", "Крадущ"},
                {"power", "Power", "Сила"}, {"punch", "Punch", "Отбой"}, {"flame", "Flame", "Горение"},
                {"infinity", "Inf", "Бескон"}, {"quick_charge", "QCharge", "Быстр"}, {"multishot", "Multi", "Мульти"},
                {"piercing", "Pierce", "Пробив"}, {"loyalty", "Loyal", "Верн"}, {"riptide", "Rip", "Тяга"},
                {"channeling", "Chan", "Молния"}, {"impaling", "Imp", "Пронз"}, {"density", "Dens", "Плотн"},
                {"breach", "Breach", "Брешь"}, {"wind_burst", "Wind", "Порыв"}, {"lure", "Lure", "Приман"},
                {"luck_of_the_sea", "LotS", "УдачаМоря"}, {"binding_curse", "Bind", "Привяз"}, {"vanishing_curse", "Van", "Исчез"}
        };
        for (String[] r : d) SHORT.put(r[0], new String[] { r[1], r[2] });
    }

    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    private record Line(String text, int rgb) { }

    private CustomEnchant() { }

    private static String roman(int n) {
        if (n <= 0 || n > 20) return Integer.toString(n);
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
                "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};
        return r[n];
    }

    private static List<Line> lines(ItemStack st) {
        List<Line> out = new ArrayList<>();
        ItemEnchantmentsComponent comp = st.get(DataComponentTypes.ENCHANTMENTS);
        if (comp == null || comp.isEmpty()) comp = st.get(DataComponentTypes.STORED_ENCHANTMENTS);
        if (comp == null || comp.isEmpty()) return out;
        boolean ru = LANG.mode == 0;

        for (RegistryEntry<Enchantment> e : comp.getEnchantments()) {
            int lvl = comp.getLevel(e);
            String path = e.getKey().map(k -> k.getValue().getPath()).orElse("?");
            int max = e.value().getMaxLevel();

            String name;
            if (FORMAT.mode == 2) name = "";
            else if (FORMAT.mode == 1) name = e.value().description().getString();
            else {
                String[] sh = SHORT.get(path);
                if (sh != null) name = ru ? sh[1] : sh[0];
                else {
                    String p = path.replace('_', ' ');
                    name = p.isEmpty() ? "?" : Character.toUpperCase(p.charAt(0)) + p.substring(1, Math.min(p.length(), 7));
                }
            }
            String text = (name.isEmpty() ? "" : name + " ") + roman(lvl);
            boolean isMax = lvl >= max;
            if (MAXMARK.bool && isMax) text += " *";

            int rgb;
            if (COLORING.mode == 1) rgb = Fx.color(SINGLE.mode, 0f);
            else if (path.endsWith("_curse")) rgb = 0xFF5555;
            else if (isMax) rgb = 0x55FF55;
            else if (lvl >= Math.ceil(max * 0.6)) rgb = 0xFFFF55;
            else rgb = 0xFFFFFF;
            out.add(new Line(text, rgb));
        }
        return out;
    }

    static void hud(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p) {
        if (!M.enabled) return;
        List<ItemStack> items = new ArrayList<>();
        if (SHOW.mode != 1) items.add(p.getMainHandStack());
        if (SHOW.mode != 0) for (EquipmentSlot s : ARMOR) items.add(p.getEquippedStack(s));

        List<ItemStack> stacks = new ArrayList<>();
        List<List<Line>> blocks = new ArrayList<>();
        int rows = 0, maxW = 0;
        for (ItemStack st : items) {
            if (st.isEmpty()) continue;
            List<Line> ls = lines(st);
            if (ls.isEmpty()) continue;
            stacks.add(st);
            blocks.add(ls);
            rows += Math.max(ls.size(), ICONS.bool ? 2 : ls.size()) + 0;
            for (Line l : ls) maxW = Math.max(maxW, mc.textRenderer.getWidth(l.text()));
        }
        if (blocks.isEmpty()) return;

        int iconW = ICONS.bool ? 20 : 0;
        int w = maxW + iconW + 6;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int x = ANCHOR.mode == 0 ? sw - w - (int) OFF_X.num : (int) OFF_X.num;
        int totalH = rows * 10 + blocks.size() * 3;
        int y = sh / 2 - totalH / 2 + (int) OFF_Y.num;

        if (BG.bool) Fx.fill(ctx, x - 2, y - 2, x + w, y + totalH, 0x101018, 0.55f);
        int cy = y;
        for (int b = 0; b < blocks.size(); b++) {
            List<Line> ls = blocks.get(b);
            if (ICONS.bool) ctx.drawItem(stacks.get(b), x, cy);
            for (int i = 0; i < ls.size(); i++) {
                ctx.drawText(mc.textRenderer, ls.get(i).text(), x + iconW, cy + i * 10, 0xFF000000 | ls.get(i).rgb(), true);
            }
            cy += Math.max(ls.size(), ICONS.bool ? 2 : ls.size()) * 10 + 3;
        }
    }
}
