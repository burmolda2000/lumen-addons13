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
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Sphere swap: по клавише берёт подходящую сферу из инвентаря во ВТОРУЮ руку (меняет местами со слотом левой руки);
 * повторное нажатие возвращает всё назад.
 */
public final class SphereSwap {
    public static final Module M = new Module("Sphere swap",
            "Put a sphere in your off hand with a key", Category.UTILITY, true, true);
    static final Setting SCAN   = M.add(Setting.mode("scan", "Scan", 0, "Inventory", "Hotbar"));
    static final Setting EFFECT = M.add(Setting.mode("effect", "Sphere effect", 0, ItemSwap.EFFECT_NAMES));
    static final Setting BACK   = M.add(Setting.bool("swap_back", "Press again to swap back", true));

    public static KeyBinding KEY;
    private static int backSlot = -1;

    private SphereSwap() { }

    static void init() {
        KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lumenx.sphere_swap", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, LumenAddons.CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (KEY.wasPressed()) trigger(mc);
        });
    }

    private static boolean isSphere(ItemStack st) {
        if (st.isEmpty()) return false;
        String text = ItemSwap.textOf(st);
        if (!ItemSwap.any(text, "сфер", "sphere", "orb")) return false;
        int e = Math.max(0, Math.min(ItemSwap.EFFECTS.length - 1, EFFECT.mode));
        if (e == 0) return true;
        return ItemSwap.any(text, java.util.Arrays.copyOfRange(ItemSwap.EFFECTS[e], 1, ItemSwap.EFFECTS[e].length));
    }

    private static void trigger(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || !M.enabled || mc.currentScreen != null) return;

        if (isSphere(p.getOffHandStack())) {
            if (!BACK.bool || backSlot < 0) { say(p, "already in off hand"); return; }
            swap(mc, p, backSlot);
            backSlot = -1;
            return;
        }
        int from = find(p.getInventory());
        if (from < 0) { say(p, "sphere not found"); return; }
        swap(mc, p, from);
        backSlot = from;
    }

    /** Слот в PlayerScreenHandler: хотбар 36-44, инвентарь 9-35. */
    private static int find(PlayerInventory inv) {
        for (int i = 0; i < 9; i++) if (isSphere(inv.getStack(i))) return 36 + i;
        if (SCAN.mode == 0) {
            for (int i = 9; i < 36; i++) if (isSphere(inv.getStack(i))) return i;
        }
        return -1;
    }

    /** Меняет слот местами со второй рукой (кнопка 40 в действии SWAP). */
    private static void swap(MinecraftClient mc, ClientPlayerEntity p, int slot) {
        mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, p);
    }

    private static void say(ClientPlayerEntity p, String msg) {
        p.sendMessage(Text.literal("Sphere swap: " + msg), true);
    }
}
