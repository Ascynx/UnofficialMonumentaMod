package ch.njol.unofficialmonumentamod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class Utils {

    private Utils() {
    }

    /**
     * Gets the plain display name of an items. This is used by Monumenta to distinguish items.
     *
     * @param itemStack An item stack
     * @return The plain display name of the item, i.e. the value of NBT node plain.display.Name.
     */
    public static String getPlainDisplayName(ItemStack itemStack) {
        return itemStack.getTag() == null ? null : itemStack.getTag().getCompound("plain").getCompound("display").getString("Name");
    }

    public static boolean isChestSortDisabledForInventory(ScreenHandler screenHandler, int slotId) {
        if (screenHandler.getSlot(slotId).inventory instanceof PlayerInventory)
            return UnofficialMonumentaModClient.options.chestsortDisabledForInventory;
        if (MinecraftClient.getInstance().currentScreen instanceof GenericContainerScreen
                && !(screenHandler.getSlot(slotId).inventory instanceof PlayerInventory)
                && ("Ender Chest".equals(MinecraftClient.getInstance().currentScreen.getTitle().getString()) // fake Ender Chest inventory (opened via Remnant)
                || MinecraftClient.getInstance().currentScreen.getTitle() instanceof TranslatableText
                && "container.enderchest".equals(((TranslatableText) MinecraftClient.getInstance().currentScreen.getTitle()).getKey()))) {
            return UnofficialMonumentaModClient.options.chestsortDisabledForEnderchest;
        }
        return UnofficialMonumentaModClient.options.chestsortDisabledEverywhereElse;
    }

    public static float smoothStep(float f) {
        if (f <= 0) return 0;
        if (f >= 1) return 1;
        return f * f * (3 - 2 * f);
    }

    public static class TextWithOffset {
        private final int XOffset;
        private final int YOffset;
        private final String message;

        public TextWithOffset(String message,int X,int Y) {
            this.message = message;
            this.XOffset = X;
            this.YOffset = Y;
        }
        public TextWithOffset(String message) {
            this(message, 0, 0);
        }

        public int getXOffset() {
            return XOffset;
        }
        public int getYOffset() {
            return YOffset;
        }
        public String getMessage() {
            return message;
        }
    }

    public static String getUrl(@NotNull URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        InputStreamReader streamReader;

        if (connection.getResponseCode() > 299) {
            streamReader = new InputStreamReader(connection.getErrorStream());
        } else {
            streamReader = new InputStreamReader(connection.getInputStream());
        }

        BufferedReader in = new BufferedReader(
                streamReader);
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();

        return content.toString();
    }

}
