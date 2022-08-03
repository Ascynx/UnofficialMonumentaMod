package ch.njol.unofficialmonumentamod.misc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.mixins.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Objects;

public class Calculator {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final CalculatorRender renderer = new CalculatorRender();
    private static int units;
    private static int price;

    private static String output;

    private static boolean hasShownError = false;

    public synchronized static String logic() {
        int HyperValue = (int) Math.floor((units * price) / 64);
        int CompressedValue = (units * price) % 64;
        if (!hasShownError && (HyperValue - 2147483647) > 0) {
            hasShownError = true;
            Notifier.addCustomToast(new NotificationToast(Text.of("Calculator"), Text.of("A value is higher than the 32bit integer limit, Expect glitches."), Notifier.getMillisHideTime()).setToastRender(NotificationToast.RenderType.SYSTEM));
        }

        return "" + HyperValue + "H* " + CompressedValue + "C*";
    }

    public static void tick() {
        if (units == 0 && price == 0) return;
        output = logic();
    }

    public static class CalculatorRender extends DrawableHelper {
        private int x;
        private int y;

        private final Identifier background = new Identifier(UnofficialMonumentaModClient.MOD_IDENTIFIER, "textures/gui/calc_background.png");

        public TextFieldWidget price;
        public TextFieldWidget units;

        CalculatorRender() {}

        public boolean shouldRender() {
            return UnofficialMonumentaModClient.options.showCalculatorInPlots && (!Objects.equals(Locations.getShortShard(), "plots") && (mc.currentScreen != null && mc.currentScreen.getClass().equals(GenericContainerScreen.class)));
        }

        private void resetPosition() {
            assert mc.currentScreen != null;
            this.x = ((HandledScreenAccessor)mc.currentScreen).getX() + ((HandledScreenAccessor)mc.currentScreen).getBackGroundWidth();
            this.y = ((HandledScreenAccessor)mc.currentScreen).getY();

            if (this.price != null) {
                this.price.x = this.x + 10;
                this.price.y = this.y + 30;
            }
            if (this.units != null) {
                this.units.x = this.x + 10;
                this.units.y = this.y + 75;
            }
        }

        public void init() {
            if (!shouldRender()) return;
            resetPosition();

            this.price = new TextFieldWidget(mc.textRenderer, this.x+10, this.y+30, (int) Math.floor(mc.textRenderer.getWidth("Enter price per unit (in C*)") / 1.5), 10, Text.of("Enter price per unit (in C*)"));
            this.units = new TextFieldWidget(mc.textRenderer, this.x+10, this.y+75, (int) Math.floor(mc.textRenderer.getWidth("Enter number of units") / 1.5), 10, Text.of("Enter number of units"));

            price.setEditableColor(16777215);
            units.setEditableColor(16777215);

            units.setChangedListener((I) -> {
                try {
                    Calculator.units = Integer.parseInt(I);
                } catch (Exception ignored) {
                    Calculator.units = 0;
                }
            });

            price.setChangedListener((I) -> {
                try {
                    Calculator.price = Integer.parseInt(I);
                } catch (Exception ignored) {
                    Calculator.price = 0;
                }
            });

        }

        public void onClose() {
            if (units != null) {
                units.setText("");
            }
            if (price != null) {
                price.setText("");
            }

            Calculator.hasShownError = false;
            Calculator.output = null;
        }

        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (!shouldRender() || price == null || units == null) return;
            resetPosition();

            mc.getTextureManager().bindTexture(background);
            super.drawTexture(matrices, x, y, 0, 0, 256, 256);

            mc.textRenderer.draw(matrices, "Enter price per unit (in C*)", x+10, y+15, 4210752);
            price.render(matrices, mouseX, mouseY, delta);

            mc.textRenderer.draw(matrices, "Enter number of units", x+10, y+60, 4210752);
            units.render(matrices, mouseX, mouseY, delta);

            if (output != null) {
                mc.textRenderer.draw(matrices, output, x+10, y+105,  4210752);
            } else mc.textRenderer.draw(matrices, "0H* 0C*", x+10, y+105,  4210752);
        }

    }
}
