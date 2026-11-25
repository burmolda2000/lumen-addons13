package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Item swap: по клавише берёт выбранный предмет из ВСЕГО инвентаря (или только хотбара)
 * и меняет его местами с рукой. Повторное нажатие возвращает всё назад.
 */
public final class ItemSwap {
    static final String[] NAMES = {
            "Totem of Undying", "Golden apple", "Enchanted golden apple", "Ender pearl",
            "Firework rocket", "End crystal", "Experience bottle", "Shield", "Water bucket",
            "Sphere (by name)", "Talisman (by name)"
    };
    static final Item[] ITEMS = {
            Items.TOTEM_OF_UNDYING, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.ENDER_PEARL,
            Items.FIREWORK_ROCKET, Items.END_CRYSTAL, Items.EXPERIENCE_BOTTLE, Items.SHIELD, Items.WATER_BUCKET,
            null, null                      // сфера и талисман ищутся по названию и описанию
    };
    static final int SPHERE = 9, TALISMAN = 10;

    /** Эффект сферы: первое значение — название в меню, остальные — ключевые слова (RU/EN) в названии или описании. */
    static final String[][] EFFECTS = {
            { "Any" },
            { "Haste", "спешк", "haste" }, { "Strength", "сила", "силы", "strength" }, { "Speed", "скорост", "speed" },
            { "Jump", "прыг", "jump" }, { "Regeneration", "регенер", "regen" }, { "Resistance", "сопротив", "resist" },
            { "Fire resistance", "огнестойк", "fire res" }, { "Night vision", "ночн", "night" },
            { "Invisibility", "невидим", "invis" }, { "Saturation", "насыщ", "satur" }
    };
    static final String[] EFFECT_NAMES = new String[EFFECTS.length];
    static { for (int i = 0; i < EFFECTS.length; i++) EFFECT_NAMES[i] = EFFECTS[i][0]; }

    public static final Module M = new Module("Item swap",
            "Swap items from inventory or hotbar with a key", Category.UTILITY, true, true);
    static final Setting SCAN   = M.add(Setting.mode("scan", "Scan", 0, "Inventory", "Hotbar"));
    static final Setting TARGET = M.add(Setting.mode("target", "Swap into", 0, "Offhand", "Main hand"));
    static final Setting ITEM   = M.add(Setting.mode("item", "Item", 0, NAMES));
    static final Setting EFFECT = M.add(Setting.mode("effect", "Sphere effect", 0, EFFECT_NAMES));
    static final Setting BACK   = M.add(Setting.bool("swap_back", "Press again to swap back", true));

    public static KeyBinding KEY;

    /** Контейнерный слот, откуда взяли предмет (для возврата), либо -1. */
    private static int backSlot = -1;

    static void init() {
        KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lumenx.item_swap", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,
                LumenAddons.CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (KEY.wasPressed()) trigger(mc);
        });
    }

    private static void trigger(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || !M.enabled) return;
        if (mc.currentScreen != null) return;

        PlayerInventory inv = p.getInventory();
        int sel = inv.getSelectedSlot();
        boolean offhand = TARGET.mode == 0;
        int want = Math.max(0, Math.min(NAMES.length - 1, ITEM.mode));
        ItemStack held = offhand ? p.getOffHandStack() : p.getMainHandStack();
        int button = offhand ? 40 : sel;           // 40 = слот второй руки для SWAP
        boolean alreadyThere = matches(held, want);

        int from;
        if (alreadyThere) {
            if (!BACK.bool || backSlot < 0) { say(p, "already in hand"); return; }
            from = backSlot;
        } else {
            from = find(inv, want, offhand ? -1 : sel);
            if (from < 0) { say(p, "not found"); return; }
        }

        mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, from, button,
                SlotActionType.SWAP, p);
        backSlot = alreadyThere ? -1 : from;
    }

    /** Возвращает номер слота в PlayerScreenHandler: хотбар 36-44, инвентарь 9-35. */
    private static int find(PlayerInventory inv, int want, int skipHotbar) {
        for (int i = 0; i < 9; i++) {
            if (i == skipHotbar) continue;
            if (matches(inv.getStack(i), want)) return 36 + i;
        }
        if (SCAN.mode == 0) {
            for (int i = 9; i < 36; i++) {
                if (matches(inv.getStack(i), want)) return i;
            }
        }
        return -1;
    }


    private static String norm(String s) {
        return s.toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    /** Название предмета и все строки описания (lore) одним текстом. */
    static String textOf(ItemStack st) {
        StringBuilder sb = new StringBuilder(st.getName().getString());
        LoreComponent lore = st.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) sb.append(' ').append(line.getString());
        }
        return norm(sb.toString());
    }

    static boolean any(String text, String... keys) {
        for (String k : keys) if (text.contains(k)) return true;
        return false;
    }

    /** Подходит ли предмет под выбранный пункт: по типу или (сфера/талисман) по названию и описанию. */
    private static boolean matches(ItemStack st, int idx) {
        if (st.isEmpty()) return false;
        Item vanilla = ITEMS[idx];
        if (vanilla != null) return st.isOf(vanilla);

        String text = textOf(st);
        if (idx == SPHERE) {
            if (!any(text, "сфер", "sphere", "orb")) return false;
            int e = Math.max(0, Math.min(EFFECTS.length - 1, EFFECT.mode));
            if (e == 0) return true;
            return any(text, java.util.Arrays.copyOfRange(EFFECTS[e], 1, EFFECTS[e].length));
        }
        return any(text, "талисман", "talisman");
    }

    private static void say(ClientPlayerEntity p, String msg) {
        p.sendMessage(Text.literal("Item swap: " + msg), true);
    }
}
