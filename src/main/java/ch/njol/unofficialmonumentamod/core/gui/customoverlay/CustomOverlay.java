package ch.njol.unofficialmonumentamod.core.gui.customoverlay;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.gui.editor.OverlayEditor;
import ch.njol.unofficialmonumentamod.utils.Utils;
import java.awt.Rectangle;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CustomOverlay extends HudElement {
    private OverlayData data = new OverlayData();

    //Non persistent data
    //{count} pattern
    public int count = 0;
    //{max} pattern | if equal to -1 max is not initialized.
    public int max = -1;

    @Override
    protected void render(MatrixStack matrices, float tickDelta) {
        final TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        DrawableHelper.fill(matrices, 0, 0, getWidth(), getHeight(), MinecraftClient.getInstance().options.getTextBackgroundColor(UnofficialMonumentaModClient.options.overlay_opacity));

        Rectangle dimension = getDimension();
        client.getItemRenderer().renderGuiItemIcon(matrices, getItemStack(), dimension.x + 4, dimension.y + (getHeight() - 16) / 2);

        Text text;
        if (isInEditMode()) {
            text = Text.of("UNKNOWN");
        } else {
            text = Text.of(String.valueOf(max > 0 ? count + "/" + max : count));
        }

        int color = max > 0 && count >= max ? 0xFF1FD655 : 0xFFFCCD12;

        //center text
        int x = 20 + (getWidth() - 20) / 2 - tr.getWidth(text) / 2;
        int y = getHeight() / 2 - tr.fontHeight / 2;

        tr.draw(matrices, text, x , y ,color);
    }

    @Override
    protected boolean isEnabled() {
        return data.enabled;
    }

    @Override
    protected boolean isVisible() {
        return count > 0 || MinecraftClient.getInstance().currentScreen instanceof OverlayEditor;
    }

    public OverlayData getData() {
        return data;
    }

    @Override
    protected int getWidth() {
        return data.width;
    }

    public void setWidth(int width) {
        data.width = width;
    }

    @Override
    protected int getHeight() {
        return data.height;
    }

    public void setHeight(int height) {
        data.height = height;
    }

    @Override
    protected ElementPosition getPosition() {
        return data.position;
    }

    @Override
    protected int getZOffset() {
        return 0;
    }

    public ItemStack getItemStack() {
        return Registries.ITEM.get(new Identifier(data.itemData)).getDefaultStack();
    }

    public void setItemStack(ItemStack stack) {
            data.itemData = Registries.ITEM.getId(stack.getItem()).toString();
    }

    public String getName() {
        return data.name;
    }

    public void setName(String name) {
        data.name = name;
    }
    public void setGroup(String group) {
        data.group = group;
    }

    public void startDrag(double mouseX, double mouseY) {
            super.startDragging(mouseX, mouseY);
    }

    //Events.
    public boolean onBossbar(Text message) throws ParseException {
        if (data.listenerData.type != ListenerType.BOSSBAR) {
            return false;
        }
        List<Utils.Formatter> matches = Utils.getFormatters(message.getString());
        return false;
    }

    public boolean onActionbar(Text message) throws ParseException {
        if (data.listenerData.type != ListenerType.ACTIONBAR) {
            return false;
        }
        List<Utils.Formatter> matches = Utils.getFormatters(message.getString());
        return false;
    }

    public void onShardChange(String shardName) {
        count = 0;
    }

    public static CustomOverlay fromData(OverlayData data) {
        CustomOverlay newOverlay = new CustomOverlay();
        newOverlay.data = data;
        return newOverlay;
    }

    public static class OverlayData {
        public int width = 64;
        public int height = 24;

        public boolean enabled = true;

        public String name = "UNKNOWN";
        public String itemData;//itemIdentifier

        @Nullable
        public String group;

        ListenerData listenerData = new ListenerData();

        ElementPosition position = new ElementPosition(0.5f, 0, 0.5f, 0, 0.5f, 0.5f);

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof OverlayData other)) {
                return false;
            }

            return width == other.width &&
                    height == other.height &&
                    enabled == other.enabled &&
                    Objects.equals(name, other.name) &&
                    Objects.equals(itemData, other.itemData) &&
                    Objects.equals(group, other.group) &&
                    Objects.equals(listenerData, other.listenerData) &&
                    isPosEqual(position, other.position);
        }

        public static boolean isPosEqual(ElementPosition a, ElementPosition b) {
            return (a == null && b == null) ||
                    (a != null && a.alignX == b.alignX && a.alignY == b.alignY &&
                            a.offsetXAbsolute == b.offsetXAbsolute && a.offsetYAbsolute == b.offsetYAbsolute &&
                            a.offsetXRelative == b.offsetXRelative && a.offsetYRelative == b.offsetYRelative);
        }
    }

    public static class ListenerData {
        public ListenerType type;
        public ArrayList<String> patterns = new ArrayList<>();//used to check whether an action bar or bossbar update corresponds to this overlay.

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ListenerData other)) {
                return false;
            }

            return Objects.equals(type, other.type) && patterns.equals(other.patterns);
        }
    }

    public enum ListenerType {
        BOSSBAR(),
        ACTIONBAR();
    }
}
