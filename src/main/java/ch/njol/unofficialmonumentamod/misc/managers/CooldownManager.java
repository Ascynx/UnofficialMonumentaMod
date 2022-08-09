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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CooldownManager {
    //TODO detect whether it was actually triggered or not
    //TODO it seems cooldown is slightly unaccurate (it actually goes off cooldown slightly after the server sends the message)
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Map<Item, CooldownEntry> entries = Maps.newHashMap();
    private static int tick;

    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("^Cooldown : (?<cooldown>[0-9]{0,3})m$");
    private static final Pattern CHARGES_PATTERN = Pattern.compile("^Charges : (?<charges>[0-9]{0,3})");

    public static boolean shouldRender() {
        return UnofficialMonumentaModClient.options.renderItemCooldowns;
    }

    public static void addCooldownToItem(ItemStack itemStack) {
        addCooldownToItem(itemStack, getCooldownFromItem(itemStack));
    }
    public static void addCooldownToItem(ItemStack itemStack, MonumentaCooldownEntry entry) {
        if (!shouldRender()) {
            entry = null;
        }
        if (entry != null) {
            set(itemStack.getItem(), entry);
        } else {
            remove(itemStack.getItem());
        }
    }


    public static MonumentaCooldownEntry getCooldownFromItem(ItemStack itemStack) {
        List<Text> tooltip = itemStack.getTooltip(mc.player, TooltipContext.Default.NORMAL);
        int charges = 1;
        int cooldown = 0;

        for (Text text: tooltip) {
            String line = text.getString();

            Matcher cooldown_matcher = COOLDOWN_PATTERN.matcher(line);
            Matcher charges_matcher = CHARGES_PATTERN.matcher(line);

            if (cooldown_matcher.matches()) {
                cooldown = (Integer.parseInt(cooldown_matcher.group("cooldown")) * 60) * 20;
            } else if (charges_matcher.matches()) {
                charges = Integer.parseInt(charges_matcher.group("charges"));
            }
        }
        if (cooldown > 0) {
            return new MonumentaCooldownEntry(cooldown, charges);
        }

        return null;
    }

    public static boolean isCoolingDown(Item item) {
        return getCooldownProgress(item, 0.0F) > 0.0F;
    }

    public static float getCooldownProgress(Item item, float partialTicks) {
        CooldownEntry entry = entries.get(item);
        if (entry != null && entry.entries.size() > 0) {
            float f = (float)(entry.entries.get(0).endTick - entry.entries.get(0).startTick);
            float g = (float)entry.entries.get(0).endTick - ((float)tick + partialTicks);
            return MathHelper.clamp(g / f, 0.0F, 1.0F);
        } else {
            return 0.0F;
        }
    }

    public static int getItemCharges(Item item) {
        CooldownEntry entry = entries.get(item);
        if (entry != null) {
            return entry.entries.size();
        } else return 0;
    }

    public static int getMaxItemCharges(Item item) {
        CooldownEntry entry = entries.get(item);
        if (entry != null) {
            return entry.max_charges;
        } else return 0;
    }

    public static void update() {
        ++tick;
        if (!entries.isEmpty()) {
            Iterator<Map.Entry<Item, CooldownEntry>> iterator = entries.entrySet().iterator();
            while(iterator.hasNext()) {
                java.util.Map.Entry<Item, CooldownEntry> entry = iterator.next();
                if (entry.getValue().entries.size() > 1 && entry.getValue().entries.get(0).endTick <= tick ) {
                    entry.getValue().entries.remove(0);
                } else if (entry.getValue().entries.get(0).endTick <= tick && entry.getValue().entries.size() == 1) {
                    iterator.remove();
                    onCooldownUpdate(entry.getKey());
                }
            }
        }

    }

    public static void set(Item item, MonumentaCooldownEntry entry) {
        if (entries.containsKey(item) && entries.get(item).max_charges > entries.get(item).entries.size()) {
                entries.get(item).entries.add(new Entry(tick, tick + entry.cooldown));
                System.out.println(entries.get(item).entries);
        } else if (!entries.containsKey(item)) entries.put(item, new CooldownEntry(tick, tick + entry.cooldown, entry.charges));
        onCooldownUpdate(item, entry.cooldown);
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

    static class CooldownEntry {
        final ArrayList<Entry> entries;
        final int max_charges;


        private CooldownEntry(int startTick, int endTick, int charges) {
            this.max_charges = charges;
            this.entries = new ArrayList<>();
            entries.add(new Entry(startTick, endTick));
        }
    }

    public static class MonumentaCooldownEntry {
        private final int cooldown;
        private final int charges;

        private MonumentaCooldownEntry(int cooldown, int charges) {
            this.charges = charges;
            this.cooldown = cooldown;
        }
    }
}
