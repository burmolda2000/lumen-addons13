package dev.lumenx;

import dev.lumen.core.Category;
import dev.lumen.core.Module;
import dev.lumen.core.Setting;
import net.minecraft.entity.LivingEntity;

/**
 * Hit color: после вашего попадания цель обводится цветным контуром по силуэту модели
 * (а не прямоугольником). Невидимые игроки не подсвечиваются.
 */
public final class HitColor {
    public static final Module M = new Module("Hit color",
            "Outlines a player's model after you hit them", Category.VISUALS, false, true);
    static final Setting COLOR      = M.add(Setting.mode("color", "Color", 2, Fx.PAL));
    static final Setting CRIT_COLOR = M.add(Setting.mode("crit_color", "Crit color", 1, Fx.PAL));
    static final Setting TIME       = M.add(Setting.num("time", "Duration (sec)", 0.6, 0.1, 3, 0.1));

    private static LivingEntity target;
    private static long t0;
    private static boolean crit;

    private HitColor() { }

    static void onHit(LivingEntity e, boolean c, long now) {
        if (!M.enabled) return;
        target = e; t0 = now; crit = c;
    }

    /** Каждый кадр до Glow.commit(): заявляет цель, пока идёт подсветка. */
    static void update(long now) {
        if (!M.enabled || target == null) return;
        float age = (now - t0) / (float) (TIME.num * 1000.0);
        if (age >= 1f || !target.isAlive()) { target = null; return; }
        Glow.want(target, Fx.color(crit ? CRIT_COLOR.mode : COLOR.mode, now / 1500f));
    }
}
