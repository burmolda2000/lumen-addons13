package dev.lumenx;

import dev.lumen.core.Config;
import dev.lumen.core.Module;
import dev.lumen.core.Modules;
import dev.lumen.util.Lang;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

/** Подключает новые модули к Lumen Visuals через Modules.ALL. */
public class LumenAddons implements ClientModInitializer {
    public static final String ID = "lumenx";
    public static KeyBinding.Category CATEGORY;

    @Override
    public void onInitializeClient() {
        CATEGORY = KeyBinding.Category.create(Identifier.of(ID, "addons"));

        ItemSwap.init();
        SphereSwap.init();
        LockSlot.init();
        Trajectory.init();
        ShulkerPreview.init();
        WeaponTrails.init();
        SaturationHud.init();
        HitRange.init();
        InventoryHud.init();
        Visuals.init();
        ConfigShare.init();

        Module[] mine = { ItemSwap.M, Trajectory.M, ShulkerPreview.M, WeaponTrails.M,
                CustomHand.M, LowFire.M, SaturationHud.M, HitRange.M, InventoryHud.M,
                HandEditor.M, HitColor.M, HitFx.HIT, HitFx.CRIT, Hitbox.M, KillEffect.M, BlockOverlay.M,
                ChinaHat.M, BodyGlow.M, MoveTrails.M, WorldParticles.M, ArrowNametag.M, MaceHelper.M,
                TotemTracker.M, AspectRatio.M,
                CustomEnchant.M, ArmorDurability.M, LockSlot.M, TargetActions.M, Predictions.M, StructureFinder.M, TargetAnim.M, Minimap.M, Keybinds.M, HealthIndicator.M, SphereSwap.M };
        for (Module m : mine) {
            if (!Modules.ALL.contains(m)) Modules.ALL.add(m);
            // бинд для включения/выключения (по умолчанию не назначен)
            m.key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.lumenx.mod." + slug(m.name), InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
        }

        addRussian();

        // Перечитать lumen.json, чтобы сохранённые настройки применились и к новым модулям
        try { Config.load(); } catch (Throwable ignored) { }
    }

    static String slug(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }

    /** Русские названия в меню Lumen. Если структура Lang другая — молча пропускаем. */
    @SuppressWarnings("unchecked")
    private static void addRussian() {
        try {
            Field f = Lang.class.getDeclaredField("RU");
            f.setAccessible(true);
            Map<String, String> ru = (Map<String, String>) f.get(null);
            ru.putIfAbsent("Item swap", "Замена предметов");
            ru.putIfAbsent("Swap items from inventory or hotbar with a key", "Быстрая замена предмета по клавише");
            ru.putIfAbsent("Scan", "Поиск");
            ru.putIfAbsent("Inventory", "Весь инвентарь");
            ru.putIfAbsent("Hotbar", "Хотбар");
            ru.putIfAbsent("Swap into", "Куда класть");
            ru.putIfAbsent("Offhand", "Вторая рука");
            ru.putIfAbsent("Main hand", "Основная рука");
            ru.putIfAbsent("Item", "Предмет");
            ru.putIfAbsent("Press again to swap back", "Повторно — вернуть назад");
            ru.putIfAbsent("Totem of Undying", "Тотем бессмертия");
            ru.putIfAbsent("Golden apple", "Золотое яблоко");
            ru.putIfAbsent("Enchanted golden apple", "Зачарованное золотое яблоко");
            ru.putIfAbsent("Ender pearl", "Эндер-жемчуг");
            ru.putIfAbsent("Firework rocket", "Фейерверк");
            ru.putIfAbsent("End crystal", "Кристалл Энда");
            ru.putIfAbsent("Experience bottle", "Пузырёк опыта");
            ru.putIfAbsent("Shield", "Щит");
            ru.putIfAbsent("Water bucket", "Ведро воды");

            ru.putIfAbsent("Trajectory", "Траектория");
            ru.putIfAbsent("Predicts where thrown items and arrows will land", "Показывает, куда прилетит бросок или стрела");
            ru.putIfAbsent("Path line", "Линия пути");
            ru.putIfAbsent("Landing marker", "Метка падения");
            ru.putIfAbsent("Highlight entity hit", "Подсветка попадания в сущность");
            ru.putIfAbsent("Max ticks", "Макс. тиков");
            ru.putIfAbsent("Throwables (pearl, snowball...)", "Броски (жемчуг, снежок...)");
            ru.putIfAbsent("Bow and crossbow", "Лук и арбалет");
            ru.putIfAbsent("Trident", "Трезубец");
            ru.putIfAbsent("Potions and XP bottles", "Зелья и пузырьки опыта");

            ru.putIfAbsent("Shulker preview", "Предпросмотр шалкера");
            ru.putIfAbsent("See shulker contents without opening it", "Содержимое шалкера без открытия");
            ru.putIfAbsent("Hold Shift only", "Только с Shift");
            ru.putIfAbsent("Show empty slots", "Пустые слоты");
            ru.putIfAbsent("Show counts", "Количество предметов");

            ru.putIfAbsent("Weapon trails", "Следы оружия");
            ru.putIfAbsent("Glowing trail: gold on normal hit, red on critical", "Светящийся след: золотой — обычный удар, красный — критический");
            ru.putIfAbsent("Length (sec)", "Длина (сек)");
            ru.putIfAbsent("Width", "Толщина");
            ru.putIfAbsent("Trail on miss", "След при промахе");
            ru.putIfAbsent("Any item", "Любой предмет");

            // --- Custom hand ---
            ru.putIfAbsent("Custom hand", "Своя рука");
            ru.putIfAbsent("Move, rotate and scale your hand, pick a swing style", "Положение, поворот и размер руки, стиль удара");
            ru.putIfAbsent("Hand", "Рука");
            ru.putIfAbsent("Both", "Обе");
            ru.putIfAbsent("Offset X", "Смещение X");
            ru.putIfAbsent("Offset Y", "Смещение Y");
            ru.putIfAbsent("Offset Z", "Смещение Z");
            ru.putIfAbsent("Rotate X", "Поворот X");
            ru.putIfAbsent("Rotate Y", "Поворот Y");
            ru.putIfAbsent("Rotate Z", "Поворот Z");
            ru.putIfAbsent("Scale", "Размер");
            ru.putIfAbsent("Swing style", "Стиль удара");
            ru.putIfAbsent("Swing strength", "Сила взмаха");
            ru.putIfAbsent("Off", "Выкл");
            ru.putIfAbsent("Swing speed", "Скорость");
            ru.putIfAbsent("Main: Offset X", "Основная: смещение X");
            ru.putIfAbsent("Main: Offset Y", "Основная: смещение Y");
            ru.putIfAbsent("Main: Offset Z", "Основная: смещение Z");
            ru.putIfAbsent("Main: Rotate X", "Основная: поворот X");
            ru.putIfAbsent("Main: Rotate Y", "Основная: поворот Y");
            ru.putIfAbsent("Main: Rotate Z", "Основная: поворот Z");
            ru.putIfAbsent("Main: Scale", "Основная: размер");
            ru.putIfAbsent("Off: Offset X", "Вторая: смещение X");
            ru.putIfAbsent("Off: Offset Y", "Вторая: смещение Y");
            ru.putIfAbsent("Off: Offset Z", "Вторая: смещение Z");
            ru.putIfAbsent("Off: Rotate X", "Вторая: поворот X");
            ru.putIfAbsent("Off: Rotate Y", "Вторая: поворот Y");
            ru.putIfAbsent("Off: Rotate Z", "Вторая: поворот Z");
            ru.putIfAbsent("Off: Scale", "Вторая: размер");
            ru.putIfAbsent("No hit dip", "Без просадки после удара");
            ru.putIfAbsent("Smooth", "Плавный");
            ru.putIfAbsent("Slide", "Скольжение");
            ru.putIfAbsent("Block", "Блок");
            ru.putIfAbsent("Spin", "Вращение");
            ru.putIfAbsent("Poke", "Тычок");
            ru.putIfAbsent("Whip", "Хлыст");
            ru.putIfAbsent("Tap", "Касание");
            ru.putIfAbsent("Hammer", "Молот");
            ru.putIfAbsent("Wave", "Волна");
            ru.putIfAbsent("Dash", "Рывок");
            ru.putIfAbsent("Stab", "Выпад");
            ru.putIfAbsent("Drop", "Падение");
            ru.putIfAbsent("Punch", "Удар кулаком");
            ru.putIfAbsent("Wiggle", "Тряска");
            ru.putIfAbsent("Spiral", "Спираль");
            ru.putIfAbsent("Chop", "Рубящий");
            ru.putIfAbsent("Uppercut", "Апперкот");
            ru.putIfAbsent("Pendulum", "Маятник");
            ru.putIfAbsent("Flip", "Переворот");
            ru.putIfAbsent("Corkscrew", "Штопор");
            ru.putIfAbsent("No swing", "Без анимации");
            // --- Low fire ---
            ru.putIfAbsent("Low fire", "Меньше огня");
            ru.putIfAbsent("Reduces the fire overlay while you burn", "Уменьшает огонь на экране, когда вы горите");
            ru.putIfAbsent("Reduction (%)", "Уменьшение (%)");
            // --- Saturation HUD ---
            ru.putIfAbsent("Saturation HUD", "Насыщение");
            ru.putIfAbsent("Shows your saturation level", "Показывает уровень насыщения");
            ru.putIfAbsent("Show number", "Показывать число");
            ru.putIfAbsent("Show food level", "Показывать голод");
            // --- Hit range ---
            ru.putIfAbsent("Hit range", "Радиус удара");
            ru.putIfAbsent("Attack radius circle under players: green = target in range, red = none", "Круг радиуса атаки под игроками: зелёный — цель рядом, красный — цели нет");
            ru.putIfAbsent("Radius", "Радиус");
            ru.putIfAbsent("Show for yourself", "Для себя");
            ru.putIfAbsent("Show for other players", "Для других игроков");
            ru.putIfAbsent("Targets", "Цели");
            ru.putIfAbsent("Players", "Игроки");
            ru.putIfAbsent("All living", "Все существа");
            ru.putIfAbsent("Max distance", "Макс. дистанция");
            // --- Inventory HUD ---
            ru.putIfAbsent("Inventory HUD", "Инвентарь на экране");
            ru.putIfAbsent("Shows your inventory on the game screen", "Показывает инвентарь прямо на игровом экране");
            ru.putIfAbsent("Position", "Позиция");
            ru.putIfAbsent("Bottom left", "Снизу слева");
            ru.putIfAbsent("Bottom right", "Снизу справа");
            ru.putIfAbsent("Top left", "Сверху слева");
            ru.putIfAbsent("Top right", "Сверху справа");
            ru.putIfAbsent("Show hotbar row", "Показывать хотбар");
            ru.putIfAbsent("Show counts", "Количество предметов");
            ru.putIfAbsent("Background", "Фон");
            ru.putIfAbsent("Hand editor", "Редактор рук");
            ru.putIfAbsent("Fine-tune hand position (overrides poses)", "Точная настройка рук (заменяет позы)");
            ru.putIfAbsent("Hit color", "Цвет попадания");
            ru.putIfAbsent("Tints a player after you hit them", "Окрашивает игрока после вашего попадания");
            ru.putIfAbsent("Hit particles", "Частицы удара");
            ru.putIfAbsent("Glowing sparks when you hit someone", "Светящиеся искры при ударе");
            ru.putIfAbsent("Crit particles", "Частицы крита");
            ru.putIfAbsent("Star burst on critical hits", "Звёздный взрыв при критическом ударе");
            ru.putIfAbsent("Hitbox customizer", "Хитбоксы");
            ru.putIfAbsent("Custom hitbox outlines, corners and fill", "Свои контуры, углы и заливка хитбоксов");
            ru.putIfAbsent("Kill effect", "Эффект убийства");
            ru.putIfAbsent("Effect when you kill a target", "Эффект после убийства цели");
            ru.putIfAbsent("Block overlay", "Подсветка блока");
            ru.putIfAbsent("Highlights the block you look at", "Подсвечивает блок под прицелом");
            ru.putIfAbsent("China hat", "Китайская шляпа");
            ru.putIfAbsent("Decorative hat above the head", "Декоративная шляпа над головой");
            ru.putIfAbsent("Body glow", "Свечение тела");
            ru.putIfAbsent("Glow around player models", "Свечение вокруг моделей игроков");
            ru.putIfAbsent("Trails", "Следы");
            ru.putIfAbsent("Trails behind moving players", "Следы за движущимися игроками");
            ru.putIfAbsent("World particles", "Частицы мира");
            ru.putIfAbsent("Decorative particles around you", "Декоративные частицы вокруг вас");
            ru.putIfAbsent("Arrow nametag", "Имена стрел");
            ru.putIfAbsent("Shows who shot the arrow in flight", "Показывает, кто выпустил стрелу");
            ru.putIfAbsent("Mace helper", "Помощник булавы");
            ru.putIfAbsent("Mace charge and highlight", "Зарядка булавы и подсветка");
            ru.putIfAbsent("Totem tracker", "Трекер тотемов");
            ru.putIfAbsent("Counts totem pops", "Считает использованные тотемы");
            ru.putIfAbsent("Hand poses, sizes and swing animations", "Позы рук, размеры и анимации удара");
            ru.putIfAbsent("Animation", "Анимация");
            ru.putIfAbsent("Main hand size", "Размер основной руки");
            ru.putIfAbsent("Off hand size", "Размер второй руки");
            ru.putIfAbsent("Main pose", "Поза основной руки");
            ru.putIfAbsent("Off pose", "Поза второй руки");
            ru.putIfAbsent("Vanilla", "Ваниль");
            ru.putIfAbsent("Flat low", "Плашмя внизу");
            ru.putIfAbsent("Tilted", "Наклон");
            ru.putIfAbsent("Raised", "Поднята");
            ru.putIfAbsent("Violet", "Фиолетовый");
            ru.putIfAbsent("Gold", "Золотой");
            ru.putIfAbsent("Red", "Красный");
            ru.putIfAbsent("Cyan", "Голубой");
            ru.putIfAbsent("Green", "Зелёный");
            ru.putIfAbsent("White", "Белый");
            ru.putIfAbsent("Rainbow", "Радуга");
            ru.putIfAbsent("Fire", "Огонь");
            ru.putIfAbsent("Ocean", "Океан");
            ru.putIfAbsent("Candy", "Конфетный");
            ru.putIfAbsent("Color", "Цвет");
            ru.putIfAbsent("Crit color", "Цвет крита");
            ru.putIfAbsent("Style", "Стиль");
            ru.putIfAbsent("Tint", "Заливка");
            ru.putIfAbsent("Outline", "Контур");
            ru.putIfAbsent("Both", "Оба");
            ru.putIfAbsent("Duration (sec)", "Длительность (сек)");
            ru.putIfAbsent("Strength (%)", "Сила (%)");
            ru.putIfAbsent("Amount", "Количество");
            ru.putIfAbsent("Size", "Размер");
            ru.putIfAbsent("Sparks", "Искры");
            ru.putIfAbsent("Dots", "Точки");
            ru.putIfAbsent("Stars", "Звёзды");
            ru.putIfAbsent("Ring", "Кольцо");
            ru.putIfAbsent("Corners", "Углы");
            ru.putIfAbsent("Filled", "Заливка");
            ru.putIfAbsent("Outline+Fill", "Контур+заливка");
            ru.putIfAbsent("Target color", "Цвет цели");
            ru.putIfAbsent("Color by health", "Цвет по здоровью");
            ru.putIfAbsent("Eye line", "Линия глаз");
            ru.putIfAbsent("Look line", "Линия взгляда");
            ru.putIfAbsent("Show yourself", "Показывать себя");
            ru.putIfAbsent("Fill strength (%)", "Сила заливки (%)");
            ru.putIfAbsent("Effect", "Эффект");
            ru.putIfAbsent("Star Glow", "Звёздное свечение");
            ru.putIfAbsent("Rings", "Кольца");
            ru.putIfAbsent("Soul", "Души");
            ru.putIfAbsent("Fireworks", "Фейерверк");
            ru.putIfAbsent("Animation ", "Анимация");
            ru.putIfAbsent("Solid", "Цельный");
            ru.putIfAbsent("Pulse", "Пульс");
            ru.putIfAbsent("Wave", "Волна");
            ru.putIfAbsent("Fill", "Заливка");
            ru.putIfAbsent("Glow", "Свечение");
            ru.putIfAbsent("Radius", "Радиус");
            ru.putIfAbsent("Height", "Высота");
            ru.putIfAbsent("Show in first person", "От первого лица");
            ru.putIfAbsent("Other players", "Другие игроки");
            ru.putIfAbsent("Mode", "Режим");
            ru.putIfAbsent("Line", "Линия");
            ru.putIfAbsent("Ribbon", "Лента");
            ru.putIfAbsent("Length (sec)", "Длина (сек)");
            ru.putIfAbsent("Ribbon height", "Высота ленты");
            ru.putIfAbsent("Comets", "Кометы");
            ru.putIfAbsent("Spiral", "Спираль");
            ru.putIfAbsent("Fireflies", "Светлячки");
            ru.putIfAbsent("Show distance", "Показывать расстояние");
            ru.putIfAbsent("Full charge (blocks)", "Полная зарядка (блоков)");
            ru.putIfAbsent("Show text", "Показывать текст");
            ru.putIfAbsent("Highlight mace", "Подсвечивать булаву");
            ru.putIfAbsent("Show list", "Показывать список");
            ru.putIfAbsent("Chat message", "Сообщение в чат");
            ru.putIfAbsent("Max entries", "Макс. записей");
            ru.putIfAbsent("Aspect ratio", "Соотношение сторон");
            ru.putIfAbsent("Stretch the 3D view to another aspect ratio", "Растягивает картинку под другое соотношение сторон");
            ru.putIfAbsent("Preset", "Пресет");
            ru.putIfAbsent("Custom", "Своё");
            ru.putIfAbsent("Custom ratio", "Своё соотношение");
            ru.putIfAbsent("Neon", "Неон");
            ru.putIfAbsent("Glass", "Стекло");
            ru.putIfAbsent("Classic", "Классика");
            ru.putIfAbsent("Fill all faces", "Заливать все грани");
            ru.putIfAbsent("Inner frame", "Внутренняя рамка");
            ru.putIfAbsent("Corner marks", "Метки углов");
            ru.putIfAbsent("Scan line", "Сканирующая линия");
            ru.putIfAbsent("Exact shape", "Точная форма блока");
            ru.putIfAbsent("Default", "Обычная");
            ru.putIfAbsent("Tilt", "Наклон");
            ru.putIfAbsent("Hand height", "Высота рук");
            ru.putIfAbsent("Custom enchant", "Свои зачарования");
            ru.putIfAbsent("Custom display of item enchantments", "Свой вид зачарований предмета");
            ru.putIfAbsent("Armor durability", "Прочность брони");
            ru.putIfAbsent("Armor condition by colour: green, yellow, red", "Состояние брони цветом: зелёный, жёлтый, красный");
            ru.putIfAbsent("Lock slot", "Блокировка слота");
            ru.putIfAbsent("Blocks dropping a hotbar slot with Q", "Запрещает выбрасывать слот хотбара на Q");
            ru.putIfAbsent("Target actions", "Действия цели");
            ru.putIfAbsent("Shows what your target is doing", "Показывает, что делает цель");
            ru.putIfAbsent("Predictions", "Предсказания");
            ru.putIfAbsent("Shows where flying projectiles will land", "Показывает, куда прилетит летящий предмет");
            ru.putIfAbsent("Structure finder", "Поиск структур");
            ru.putIfAbsent("Finds spawners and structures in loaded chunks", "Ищет спавнеры и структуры в загруженных чанках");
            ru.putIfAbsent("Layout", "Расположение");
            ru.putIfAbsent("Vertical", "Вертикально");
            ru.putIfAbsent("Horizontal", "Горизонтально");
            ru.putIfAbsent("Anchor", "Привязка");
            ru.putIfAbsent("Left", "Слева");
            ru.putIfAbsent("Right", "Справа");
            ru.putIfAbsent("Show percent", "Показывать проценты");
            ru.putIfAbsent("Show bar", "Показывать полоску");
            ru.putIfAbsent("Show icons", "Показывать иконки");
            ru.putIfAbsent("Show on", "Показывать для");
            ru.putIfAbsent("Held item", "Предмет в руке");
            ru.putIfAbsent("Armor", "Броня");
            ru.putIfAbsent("Format", "Формат");
            ru.putIfAbsent("Short", "Коротко");
            ru.putIfAbsent("Full", "Полностью");
            ru.putIfAbsent("Level only", "Только уровень");
            ru.putIfAbsent("Language", "Язык");
            ru.putIfAbsent("Russian", "Русский");
            ru.putIfAbsent("English", "English");
            ru.putIfAbsent("Coloring", "Окраска");
            ru.putIfAbsent("By level", "По уровню");
            ru.putIfAbsent("Single", "Один цвет");
            ru.putIfAbsent("Single color", "Цвет");
            ru.putIfAbsent("Mark max level", "Отмечать максимум");
            ru.putIfAbsent("Show item icon", "Иконка предмета");
            ru.putIfAbsent("Show lock marker", "Показывать замок");
            ru.putIfAbsent("Show icon", "Иконка");
            ru.putIfAbsent("Show timer", "Таймер");
            ru.putIfAbsent("Show attacks", "Показывать удары");
            ru.putIfAbsent("Show movement", "Показывать движение");
            ru.putIfAbsent("Track", "Отслеживать");
            ru.putIfAbsent("Ender pearls", "Эндер-жемчуг");
            ru.putIfAbsent("All throwables", "Все броски");
            ru.putIfAbsent("Enemy projectiles", "Чужие снаряды");
            ru.putIfAbsent("Window", "Окно");
            ru.putIfAbsent("World marker", "Метка в мире");
            ru.putIfAbsent("Window zoom", "Масштаб окна");
            ru.putIfAbsent("Spawners", "Спавнеры");
            ru.putIfAbsent("Stronghold portal", "Портал крепости");
            ru.putIfAbsent("Ancient city", "Древний город");
            ru.putIfAbsent("Trial chamber", "Испытательная камера");
            ru.putIfAbsent("Fortress / bastion", "Крепость / бастион");
            ru.putIfAbsent("Ocean monument", "Океанский монумент");
            ru.putIfAbsent("End city", "Город Энда");
            ru.putIfAbsent("Village", "Деревня");
            ru.putIfAbsent("Temples (trap)", "Храмы (ловушки)");
            ru.putIfAbsent("Ruined portal", "Разрушенный портал");
            ru.putIfAbsent("Scan radius (chunks)", "Радиус поиска (чанки)");
            ru.putIfAbsent("Boxes", "Боксы");
            ru.putIfAbsent("Tracers", "Линии");
            ru.putIfAbsent("Chat notify", "Уведомления в чат");
            ru.putIfAbsent("Target animations", "Анимации цели");
            ru.putIfAbsent("More Target ESP animations (capture, rings, orbit...)", "Больше анимаций Target ESP (захват, кольца, орбита...)");
            ru.putIfAbsent("Capture", "Захват");
            ru.putIfAbsent("Brackets", "Скобки");
            ru.putIfAbsent("Orbit", "Орбита");
            ru.putIfAbsent("Reticle", "Прицел");
            ru.putIfAbsent("Diamond", "Ромб");
            ru.putIfAbsent("Speed", "Скорость");
            ru.putIfAbsent("Use aimed entity", "Брать цель по прицелу");
            ru.putIfAbsent("Minimap", "Мини-карта");
            ru.putIfAbsent("Map of the area around you", "Карта окрестностей вокруг вас");
            ru.putIfAbsent("Range", "Дальность");
            ru.putIfAbsent("Scale", "Масштаб");
            ru.putIfAbsent("Shape", "Форма");
            ru.putIfAbsent("Square", "Квадрат");
            ru.putIfAbsent("Circle", "Круг");
            ru.putIfAbsent("Rotate with player", "Вращать по взгляду");
            ru.putIfAbsent("Corner", "Угол");
            ru.putIfAbsent("Opacity (%)", "Прозрачность (%)");
            ru.putIfAbsent("Show players", "Игроки");
            ru.putIfAbsent("Show hostile", "Враждебные");
            ru.putIfAbsent("Show passive", "Мирные");
            ru.putIfAbsent("Show compass", "Стороны света");
            ru.putIfAbsent("Show coordinates", "Координаты");
            ru.putIfAbsent("Sphere (by name)", "Сфера (по названию)");
            ru.putIfAbsent("Talisman (by name)", "Талисман (по названию)");
            ru.putIfAbsent("Sphere effect", "Эффект сферы");
            ru.putIfAbsent("Any", "Любой");
            ru.putIfAbsent("Haste", "Спешка");
            ru.putIfAbsent("Strength", "Сила");
            ru.putIfAbsent("Jump", "Прыгучесть");
            ru.putIfAbsent("Regeneration", "Регенерация");
            ru.putIfAbsent("Resistance", "Сопротивление");
            ru.putIfAbsent("Fire resistance", "Огнестойкость");
            ru.putIfAbsent("Night vision", "Ночное зрение");
            ru.putIfAbsent("Invisibility", "Невидимость");
            ru.putIfAbsent("Saturation", "Насыщение");
            ru.putIfAbsent("Keybinds", "Бинды");
            ru.putIfAbsent("List of module keybinds", "Список клавиш модулей");
            ru.putIfAbsent("All bound", "Все назначенные");
            ru.putIfAbsent("Only enabled", "Только включённые");
            ru.putIfAbsent("Include addon keys", "Клавиши аддона");
            ru.putIfAbsent("Show title", "Заголовок");
            ru.putIfAbsent("Show", "Показывать");
            ru.putIfAbsent("Invisible only", "Только невидимые");
            ru.putIfAbsent("All players", "Все игроки");
            ru.putIfAbsent("Health indicator", "Индикатор здоровья");
            ru.putIfAbsent("Hearts of visible players", "Сердечки видимых игроков");
            ru.putIfAbsent("Hearts", "Сердечки");
            ru.putIfAbsent("Number", "Число");
            ru.putIfAbsent("Show absorption", "Золотые сердца");
            ru.putIfAbsent("Mark invisible", "Помечать невидимых");
            ru.putIfAbsent("Sphere swap", "Сфера в другую руку");
            ru.putIfAbsent("Put a sphere in your off hand with a key", "Берёт сферу в другую руку по клавише");
            ru.putIfAbsent("Press again to swap back", "Повторно — вернуть назад");
            ru.putIfAbsent("Ellipse", "Эллипс");
            ru.putIfAbsent("Model outline", "Контур модели");
            ru.putIfAbsent("Line of sight only", "Только в зоне видимости");
            ru.putIfAbsent("Outlines a player's model after you hit them", "Обводит модель игрока контуром после вашего попадания");
        } catch (Throwable ignored) { }
    }
}
