package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Modules;
import dev.lumen.core.Setting;
import dev.lumen.hud.HudRenderer;
import dev.lumen.hud.HudWidget;
import dev.lumen.hud.TargetTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

/** Target actions: что делает цель (ест, пьёт зелье, натягивает лук...) — строка под Target HUD. */
public final class TargetActions {
    public static final Module M = new Module("Target actions",
            "Shows what your target is doing", Category.VISUALS, false, true);
    static final Setting LANG  = M.add(Setting.mode("lang", "Language", 0, "Russian", "English"));
    static final Setting ICON  = M.add(Setting.bool("icon", "Show icon", true));
    static final Setting TIMER = M.add(Setting.bool("timer", "Show timer", true));
    static final Setting SWING = M.add(Setting.bool("swing", "Show attacks", true));
    static final Setting MOVES = M.add(Setting.bool("moves", "Show movement", false));
    static final Setting OFF_X = M.add(Setting.num("off_x", "Offset X", 0, -200, 200, 1));
    static final Setting OFF_Y = M.add(Setting.num("off_y", "Offset Y", 0, -100, 100, 1));

    private record Act(String text, int rgb, ItemStack icon, String extra) { }

    private TargetActions() { }

    /** Полёт на элитрах: сверяем позу по имени — метод и константа переименовывались между версиями. */
    private static boolean isGliding(LivingEntity t) {
        String pose = t.getPose().name();
        return pose.equals("GLIDING") || pose.equals("FALL_FLYING");
    }

    private static Act action(LivingEntity t) {
        boolean ru = LANG.mode == 0;
        if (t.isUsingItem()) {
            ItemStack st = t.getActiveItem();
            int used = t.getItemUseTime();
            String time = TIMER.bool ? String.format("%.1fs", Math.max(0f, t.getItemUseTimeLeft() / 20f)) : "";
            if (st.isOf(Items.BOW)) {
                float f = used / 20f;
                f = MathHelper.clamp((f * f + f * 2f) / 3f, 0f, 1f);
                return new Act(ru ? "Натягивает лук" : "Drawing bow", 0xFF5555, st, Math.round(f * 100f) + "%");
            }
            if (st.isOf(Items.CROSSBOW)) return new Act(ru ? "Заряжает арбалет" : "Charging crossbow", 0xFF5555, st, time);
            if (st.isOf(Items.TRIDENT)) return new Act(ru ? "Замахивается трезубцем" : "Charging trident", 0xFF5555, st, "");
            if (st.isOf(Items.SHIELD)) return new Act(ru ? "Блокирует щитом" : "Blocking", 0xFFD700, st, "");
            if (st.isOf(Items.POTION)) return new Act(ru ? "Пьёт зелье" : "Drinking potion", 0x55AAFF, st, time);
            if (st.isOf(Items.GOLDEN_APPLE) || st.isOf(Items.ENCHANTED_GOLDEN_APPLE)) {
                return new Act(ru ? "Ест золотое яблоко" : "Eating golden apple", 0x55FF55, st, time);
            }
            if (st.isOf(Items.CHORUS_FRUIT)) return new Act(ru ? "Ест хорус" : "Eating chorus fruit", 0xB07CE0, st, time);
            if (st.isOf(Items.MILK_BUCKET)) return new Act(ru ? "Пьёт молоко" : "Drinking milk", 0xFFFFFF, st, time);
            if (st.contains(DataComponentTypes.FOOD)) {
                return new Act((ru ? "Ест: " : "Eating: ") + st.getName().getString(), 0x55FF55, st, time);
            }
            return new Act((ru ? "Использует: " : "Using: ") + st.getName().getString(), 0xFFAA00, st, time);
        }
        if (t.isBlocking()) return new Act(ru ? "Блокирует щитом" : "Blocking", 0xFFD700, t.getOffHandStack(), "");
        if (SWING.bool && t.getHandSwingProgress(1f) > 0.02f) {
            return new Act(ru ? "Атакует" : "Attacking", 0xFFAA00, t.getMainHandStack(), "");
        }
        if (MOVES.bool) {
            if (isGliding(t)) return new Act(ru ? "Летит на элитрах" : "Gliding", 0x55E0FF, ItemStack.EMPTY, "");
            if (t.isSprinting()) return new Act(ru ? "Бежит" : "Sprinting", 0xFFFFFF, ItemStack.EMPTY, "");
            if (t.isSneaking()) return new Act(ru ? "Крадётся" : "Sneaking", 0xAAAAAA, ItemStack.EMPTY, "");
        }
        return null;
    }

    static void hud(DrawContext ctx, MinecraftClient mc, ClientPlayerEntity p) {
        if (!M.enabled) return;
        LivingEntity t = TargetTracker.entity;
        if (t == null || !t.isAlive()) return;
        Act a = action(t);
        if (a == null) return;

        // под виджетом Target HUD, где бы игрок его ни поставил
        float x = ctx.getScaledWindowWidth() / 2f - 60f, y = 56f;
        for (HudWidget w : HudRenderer.widgets()) {
            if (w.module == Modules.TARGET) { x = w.px; y = w.py + w.bh() + 3f; break; }
        }
        int ix = Math.round(x) + (int) OFF_X.num, iy = Math.round(y) + (int) OFF_Y.num;

        String text = a.extra().isEmpty() ? a.text() : a.text() + "  " + a.extra();
        boolean icon = ICON.bool && !a.icon().isEmpty();
        int w = mc.textRenderer.getWidth(text) + (icon ? 22 : 8);
        Fx.fill(ctx, ix, iy, ix + w, iy + 18, 0x101018, 0.75f);
        Fx.fill(ctx, ix, iy, ix + 2, iy + 18, a.rgb(), 1f);
        if (icon) ctx.drawItem(a.icon(), ix + 4, iy + 1);
        ctx.drawText(mc.textRenderer, text, ix + (icon ? 22 : 6), iy + 5, 0xFF000000 | a.rgb(), true);
    }
}
