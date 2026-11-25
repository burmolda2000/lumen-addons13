package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;

/** Lock slot: блокирует слот хотбара, чтобы его нельзя было выбросить клавишей Q (и Ctrl+Q). */
public final class LockSlot {
    public static final Module M = new Module("Lock slot",
            "Blocks dropping a hotbar slot with Q", Category.UTILITY, true, true);
    static final Setting MARK = M.add(Setting.bool("mark", "Show lock marker", true));

    public static KeyBinding KEY;
    private static int mask;

    private LockSlot() { }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("lumen-addons-lockslot.txt");
    }

    static void init() {
        try {
            Path f = file();
            if (Files.exists(f)) mask = Integer.parseInt(Files.readString(f).trim()) & 0x1FF;
        } catch (Exception ignored) { }

        KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lumenx.lock_slot", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, LumenAddons.CATEGORY));

        // START-тик идёт раньше обработки клавиш игрой, поэтому нажатия Q можно «съесть» до того, как предмет выбросится
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            while (KEY.wasPressed()) toggle(p);
            if (M.enabled && mc.currentScreen == null && isLocked(p.getInventory().getSelectedSlot())) {
                while (mc.options.dropKey.wasPressed()) { /* заблокировано */ }
            }
        });
    }

    static boolean isLocked(int slot) {
        return slot >= 0 && slot < 9 && (mask & (1 << slot)) != 0;
    }

    private static void toggle(ClientPlayerEntity p) {
        int s = p.getInventory().getSelectedSlot();
        if (s < 0 || s > 8) return;
        mask ^= (1 << s);
        try { Files.writeString(file(), Integer.toString(mask)); } catch (Exception ignored) { }
        p.sendMessage(Text.literal("Lock slot: " + (s + 1) + (isLocked(s) ? " locked" : " unlocked")), true);
    }

    static void hud(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p) {
        if (!M.enabled || !MARK.bool || mask == 0) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        for (int i = 0; i < 9; i++) {
            if (!isLocked(i)) continue;
            int sx = sw / 2 - 91 + i * 20, sy = sh - 22;
            int ix = sx + 13, iy = sy + 1;
            // маленький навесной замок
            Fx.fill(ctx, ix + 1, iy, ix + 6, iy + 1, 0xFFD700, 1f);
            Fx.fill(ctx, ix + 1, iy + 1, ix + 2, iy + 3, 0xFFD700, 1f);
            Fx.fill(ctx, ix + 5, iy + 1, ix + 6, iy + 3, 0xFFD700, 1f);
            Fx.fill(ctx, ix, iy + 3, ix + 7, iy + 8, 0xFF4444, 1f);
            Fx.fill(ctx, ix + 3, iy + 5, ix + 4, iy + 6, 0x101018, 1f);
        }
    }
}
