package ch.njol.unofficialmonumentamod.misc.managers;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CooldownManager {
    //TODO detect whether it was actually triggered or not
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Map<Item, Entry> entries = Maps.newHashMap();
    private static int tick;

    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("^Cooldown : (?<cooldown>[0-9]{0,3})m$");

    public static boolean shouldRender() {
        return UnofficialMonumentaModClient.options.renderItemCooldowns;
    }

    public static void addCooldownToItem(ItemStack itemStack) {
        addCooldownToItem(itemStack, getCooldownFromItem(itemStack));
    }
    public static void addCooldownToItem(ItemStack itemStack, int cooldown) {
        if (mc.player == null) return;
        if (!shouldRender()) {
            cooldown = 0;
        }
        if (cooldown != 0) {
            set(itemStack.getItem(), cooldown);
        } else {
            remove(itemStack.getItem());
        }
    }


    public static int getCooldownFromItem(ItemStack itemStack) {
        List<Text> tooltip = itemStack.getTooltip(mc.player, TooltipContext.Default.NORMAL);

        for (Text text: tooltip) {
            String line = text.getString();
            Matcher matcher = COOLDOWN_PATTERN.matcher(line);
            if (matcher.matches()) {
                return (Integer.parseInt(matcher.group("cooldown")) * 60) * 20;
            }
        }
        return 0;
    }

    public static boolean isCoolingDown(Item item) {
        return getCooldownProgress(item, 0.0F) > 0.0F;
    }

    public static float getCooldownProgress(Item item, float partialTicks) {
        Entry entry = (Entry)entries.get(item);
        if (entry != null) {
            float f = (float)(entry.endTick - entry.startTick);
            float g = (float)entry.endTick - ((float)tick + partialTicks);
            return MathHelper.clamp(g / f, 0.0F, 1.0F);
        } else {
            return 0.0F;
        }
    }

    public static void update() {
        ++tick;
        if (!entries.isEmpty()) {
            Iterator iterator = entries.entrySet().iterator();

            while(iterator.hasNext()) {
                java.util.Map.Entry<Item, Entry> entry = (java.util.Map.Entry)iterator.next();
                if (((Entry)entry.getValue()).endTick <= tick) {
                    iterator.remove();
                    onCooldownUpdate((Item)entry.getKey());
                }
            }
        }

    }

    public static void set(Item item, int duration) {
        entries.put(item, new Entry(tick, tick + duration));
        onCooldownUpdate(item, duration);
    }

    @Environment(EnvType.CLIENT)
    public static void remove(Item item) {
        entries.remove(item);
        onCooldownUpdate(item);
    }

    protected static void onCooldownUpdate(Item item, int duration) {
    }

    protected static void onCooldownUpdate(Item item) {
    }

    static class Entry {
        private final int startTick;
        private final int endTick;

        private Entry(int startTick, int endTick) {
            this.startTick = startTick;
            this.endTick = endTick;
        }
    }
}
