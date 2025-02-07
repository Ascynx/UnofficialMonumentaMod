package ch.njol.unofficialmonumentamod.features.misc.dev;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.Utils;
import ch.njol.unofficialmonumentamod.core.gui.InventoryWidget;
import ch.njol.unofficialmonumentamod.mixins.screen.HandledScreenAccessor;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;

public class ItemDataOverlay extends InventoryWidget {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static ItemDataOverlay lastInitializedWidget = null;

    private final ItemStack itemStack;
    private List<String> cachedLines = null;

    private double pos = 0;

    public ItemDataOverlay(Screen parent, ItemStack stack) {
        super(parent);
        itemStack = stack.copy();
        if (UnofficialMonumentaModClient.options.itemdataoverlay_mode == ItemDataOverlayMode.NBT) {
            if (itemStack.hasNbt() && itemStack.getNbt() != null) {
                cachedLines = splitNbt(itemStack.getNbt());
            }
        }

        if (cachedLines == null) {
            cachedLines = new ArrayList<>();
        }
    }

    private final int TITLE_HEIGHT = 20;
    private final float TITLE_SCALE_FACTOR = 1.5f;

    @Override public Rectangle getDimension() {
        int x = ((HandledScreenAccessor) parentScreen).getX() + ((HandledScreenAccessor) parentScreen).getBackGroundWidth();
        int y = ((HandledScreenAccessor) parentScreen).getY() - TITLE_HEIGHT;

        int TITLE_MARGIN = 4;
        int width = TooltipComponent.of(itemStack.getName().asOrderedText()).getWidth(client.textRenderer) + (TITLE_MARGIN * 2);
        width = Math.max(width, 160);

        int height = 180;

        return new Rectangle(x, y, (int) (width * TITLE_SCALE_FACTOR), height);
    }

    @Override public void renderBackground(DrawContext ctx, Rectangle dimension) {
        final int backgroundColor = client.options.getTextBackgroundColor(0.3f);
        ctx.fill(dimension.x, dimension.y, (int) dimension.getMaxX(), (int) dimension.getMaxY(), backgroundColor);
    }

    @Override public void render(DrawContext ctx, Rectangle dimension, int mouseX, int mouseY, float delta) {
        renderTitle(ctx, mouseX, mouseY, dimension);

        int yPos = dimension.y + TITLE_HEIGHT;
        for (int i = (int) pos; i < cachedLines.size(); i++) {
            if (yPos + (client.textRenderer.fontHeight + 1) > dimension.getMaxY()) {
                //add 1 to relativeIndex to keep a short margin, stops it from going overboard.
                break;
            }
            String line = cachedLines.get(i);
            if (client.textRenderer.getWidth(line) >= dimension.getWidth()) {
                //limit size to avoid going over the container limit + remove trailing as " ..." doesn't look as good.
                line = client.textRenderer
                        .trimToWidth(line, (int) dimension.getWidth() - (client.textRenderer.getWidth("...") * 2))
                        .stripTrailing();
                line += "...";
            }

            ctx.drawTextWithShadow(client.textRenderer, line, dimension.x + 10, yPos, 0xffffff);
            yPos += (client.textRenderer.fontHeight + 1);
        }

        renderScrollbar(ctx, mouseX, mouseY, dimension, (int) pos, (int) renderableLines(), cachedLines.size());
    }

    private void renderTitle(DrawContext ctx, double mouseX, double mouseY, Rectangle dimension) {
        ctx.getMatrices().push();
        ctx.getMatrices().scale(TITLE_SCALE_FACTOR, TITLE_SCALE_FACTOR, TITLE_SCALE_FACTOR);
        TooltipComponent component = TooltipComponent.of(itemStack.getName().asOrderedText());
        int titleY = (dimension.y) + ((TITLE_HEIGHT - client.textRenderer.fontHeight) / 2);
        double titleX = dimension.x + ((dimension.getWidth()) / TITLE_SCALE_FACTOR - component.getWidth(client.textRenderer)) / 2;

        component.drawText(client.textRenderer, (int) (titleX / TITLE_SCALE_FACTOR), (int) (titleY / TITLE_SCALE_FACTOR), ctx.getMatrices().peek().getPositionMatrix(), ctx.getVertexConsumers());
        ctx.getMatrices().pop();
    }

    private void renderScrollbar(DrawContext ctx, double mouseX, double mouseY, Rectangle dimension, int firstPos, int renderedSize, int size) {
        float factor = renderedSize / (float) size;
        double height = dimension.getHeight() - TITLE_HEIGHT;

        int basePos = (int) ((double) firstPos / size * height);
        double barHeight = Math.min(factor * height, height);

        Rectangle barDimensions = getBaseBarDimension();
        boolean mouseOverBar = barDimensions.contains(mouseX, mouseY) || clickedInBar;

        barDimensions.y += basePos;
        barDimensions.height = (int) barHeight;
        if (mouseOverBar) {
            barDimensions.y -= 1;
            barDimensions.x -= 1;
            barDimensions.width += 2;
            barDimensions.height += 2;
        }

        ctx.fill(barDimensions.x, barDimensions.y, (int) barDimensions.getMaxX(), (int) barDimensions.getMaxY(), 0xFFFFFFFF);
    }

    private boolean clickedInBar = false;
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (getBaseBarDimension().contains(mouseX, mouseY) && button == 0) {
            clickedInBar = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        Rectangle barDimensions = getBaseBarDimension();
        if (barDimensions.contains(mouseX, mouseY) && button == 0) {
            double relativeYPos = mouseY - barDimensions.y;
            int size = cachedLines.size();

            pos = (relativeYPos / (getDimension().getHeight() - TITLE_HEIGHT)) * size;
            clampPosition();

            return true;
        }

        clickedInBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        //invert movement so it lines up and clamp the result to the size of the list.
        pos += (int) -verticalAmount;
        clampPosition();
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (clickedInBar) {
            int size = cachedLines.size();
            double relativeDrag = deltaY / (getDimension().getHeight() - TITLE_HEIGHT);
            double relativeMove = relativeDrag * size;

            pos = pos + relativeMove;
            clampPosition();

            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override public void appendNarrations(NarrationMessageBuilder builder) {
        //nuh uh
    }

    private void clampPosition() {
        pos = Utils.clamp(0, (float) pos, (float) (cachedLines.size() - (1 + renderableLines())));
    }

    private double renderableLines() {
        //remove 1 from the total, to keep the margin into account.
        return (getBaseBarDimension().getHeight() / ((client.textRenderer.fontHeight + 1))) - 1;
    }

    private Rectangle getBaseBarDimension() {
        Rectangle dim = getDimension();
        int baseY = dim.y + TITLE_HEIGHT;

        int barWidth = 2;
        double barHeight = dim.getHeight() - TITLE_HEIGHT;
        return new Rectangle(
                dim.x,
                baseY,
                barWidth,
                (int) barHeight
        );
    }

    private static List<String> splitNbt(NbtCompound tag) {
        List<String> splits = new ArrayList<>();

        String dirtyNbt = tag.toString();
        StringBuilder builder = new StringBuilder();

        boolean skipNext = false;
        boolean inQuotation = false;
        int depth = 0;
        for (int i = 0; i < dirtyNbt.length(); i++) {
            char c = dirtyNbt.charAt(i);
            if (!skipNext && (c == '\'' || c == '\"')) {
                inQuotation = !inQuotation;
                builder.append(c);
                continue;
            }

            if (c == '\\') {
                skipNext = !skipNext;
                builder.append(c);
                continue;
            }

            if (!skipNext) {
                //put the character on the next line.
                if ( c == ']' || c == '}') {
                    splits.add(builder.toString().indent(depth).stripTrailing());
                    builder.setLength(0);
                    depth--;
                }
            }

            builder.append(c);
            if (!skipNext) {
                //put the character on this line, then new line.
                if (c == ',' || c == '[' || c == '{') {
                    if (c == '[' || c == '{') {
                        depth++;
                    }
                    splits.add(builder.toString().indent(depth).stripTrailing());
                    builder.setLength(0);
                }
            }
            skipNext = false;
        }
        if (!builder.isEmpty()) {
            splits.add(builder.toString());
        }

        return splits;
    }

    private static boolean mightRender() {
        return client.currentScreen instanceof HandledScreen<?>;
    }

    private static ItemStack getFocusedStack(HandledScreen<?> handled) {
        Slot focused = ((HandledScreenAccessor) handled).getFocusedSlot();
        if (focused == null || ((HandledScreenAccessor)handled).isMouseOverSlot(focused, client.mouse.getX(), client.mouse.getY()) || !focused.hasStack()) {
            return null;
        }
        return focused.getStack();
    }

    public static boolean keyTyped(int keyCode, int scanCode, int modifiers) {
        if (lastInitializedWidget != null && client.currentScreen != null) {
            if (InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.GLFW_KEY_UP)) {
                lastInitializedWidget.pos--;
                lastInitializedWidget.clampPosition();
            } else if (InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.GLFW_KEY_DOWN)) {
                lastInitializedWidget.pos++;
                lastInitializedWidget.clampPosition();
            }
        }

        if (mightRender() && UnofficialMonumentaModClient.nbtDevOverlayKeyBinding.matchesKey(keyCode, scanCode)
                && modifiers == 0) {
            if (!(client.currentScreen instanceof HandledScreen<?> handled)) {
                return false;
            }
            ItemStack stack = getFocusedStack(handled);
            ItemStack oldStack = null;

            if (lastInitializedWidget != null) {
                oldStack = lastInitializedWidget.itemStack;
                Utils.removeWidget(handled, lastInitializedWidget);
                lastInitializedWidget = null;
            }

            //do not re-open if it seems like it is the same item.
            if (stack != null && !(oldStack != null && stack.toString().equals(oldStack.toString()))) {
                ItemDataOverlay overlay = new ItemDataOverlay(handled, stack);
                Utils.addWidget(handled, overlay);
                lastInitializedWidget = overlay;
            }
            return true;
        }
        return false;
    }

    public static void onGUIClosed() {
        if (lastInitializedWidget != null) {
            lastInitializedWidget = null;
        }
    }

    public static void onGUIResized(Screen screen) {
        if (lastInitializedWidget != null) {
            ItemDataOverlay overlay = new ItemDataOverlay(screen, lastInitializedWidget.itemStack);
            Utils.addWidget(screen, overlay);
            lastInitializedWidget = overlay;
        }
    }

    public enum ItemDataOverlayMode {
        NBT
    }
}
