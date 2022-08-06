package ch.njol.unofficialmonumentamod.misc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CooldownManager {//TODO detect whether it was actually triggered or not
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final ItemCooldownManager manager = mc.player.getItemCooldownManager();

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
            manager.set(itemStack.getItem(), cooldown);
        } else {
            manager.remove(itemStack.getItem());
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
}
