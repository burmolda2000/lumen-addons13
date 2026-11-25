package dev.lumenx;

import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Общий менеджер «свечения» Minecraft: контур идёт строго по силуэту модели.
 * Модули каждый кадр заявляют цели (want), затем commit() включает/выключает свечение и возвращает прежнее состояние.
 * Невидимые существа не подсвечиваются.
 */
public final class Glow {
    private static final Map<Entity, Boolean> PREV = new HashMap<>();
    private static final Map<Entity, Integer> WANT = new HashMap<>();
    private static Map<Entity, Integer> colors = new HashMap<>();

    private Glow() { }

    static void begin() {
        WANT.clear();
    }

    static void want(Entity e, int rgb) {
        if (e == null || e.isInvisible() || !e.isAlive()) return;
        WANT.put(e, rgb & 0xFFFFFF);
    }

    static void commit() {
        for (Entity e : WANT.keySet()) {
            if (!PREV.containsKey(e)) PREV.put(e, e.isGlowing());
            if (!e.isGlowing()) e.setGlowing(true);        // сервер может перезаписать флаги — включаем снова
        }
        Iterator<Map.Entry<Entity, Boolean>> it = PREV.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Entity, Boolean> en = it.next();
            if (!WANT.containsKey(en.getKey())) { en.getKey().setGlowing(en.getValue()); it.remove(); }
        }
        colors = new HashMap<>(WANT);
    }

    /** Цвет контура (RGB) или null — вызывается из EntityMixin. */
    public static Integer colorFor(Entity e) {
        return colors.get(e);
    }
}
