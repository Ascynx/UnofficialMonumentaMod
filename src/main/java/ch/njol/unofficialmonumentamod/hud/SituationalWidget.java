package ch.njol.unofficialmonumentamod.hud;

import ch.njol.minecraft.uiframework.ElementPosition;
import ch.njol.minecraft.uiframework.hud.HudElement;
import ch.njol.unofficialmonumentamod.Atlases;
import ch.njol.unofficialmonumentamod.ChannelHandler;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.utils.GuiUtils;
import ch.njol.unofficialmonumentamod.utils.Utils;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class SituationalWidget extends HudElement {
    public static final SituationalWidget INSTANCE = new SituationalWidget();
    public static Map<String, Identifier> identifiers = new HashMap<>();

    public static final int MIN_WIDTH = 24;
    public static final int HEIGHT = 24;
    public static final int ELEMENT_SIZE = 24;//24x24
    public static final int SPRITE_SIZE = 16;//16x16

    public static final int BACKGROUND_COLOR = 0xFF212121;//same color as the original texture for effects in inventory.

    private SituationalWidget() {}

    public final Map<String, SituationalData> equippedSituationals = new HashMap<>();

    public void onInitSituationalPacket(ChannelHandler.InitialEnchantmentPacket packet) {
        equippedSituationals.clear();
        if (packet.enchantments == null) {
            return;
        }

        for (ChannelHandler.InitialEnchantmentPacket.Enchantment enchantment: packet.enchantments) {
            SituationalData data = new SituationalData();
            data.name = enchantment.name;

            equippedSituationals.put(data.name, data);
        }
    }

    public void onSituationalUpdatePacket(ChannelHandler.SituationalStateUpdatePacket packet) {
        if (equippedSituationals.containsKey(packet.name)) {
            equippedSituationals.get(packet.name).setState(packet.state);
        }
    }

    @Override
    protected boolean isEnabled() {
        return UnofficialMonumentaModClient.options.enableSituationalsOverlay;
    }

    @Override
    protected boolean isVisible() {
        return !equippedSituationals.isEmpty();
    }

    @Override
    protected int getWidth() {
        return UnofficialMonumentaModClient.options.situationals_horizontal ? Math.max(MIN_WIDTH, ELEMENT_SIZE * (!isInEditMode() ? equippedSituationals.size() : 4)) : MIN_WIDTH;
    }

    @Override
    protected int getHeight() {
        return !UnofficialMonumentaModClient.options.situationals_horizontal ? Math.max(HEIGHT, ELEMENT_SIZE * (!isInEditMode() ? equippedSituationals.size() : 4)) : HEIGHT;
    }

    @Override
    protected ElementPosition getPosition() {
        return UnofficialMonumentaModClient.options.situationalsPosition;
    }

    @Override
    protected int getZOffset() {
        return 0;
    }

    private int currCycleTick = 0;
    public void tick() {
        if (currCycleTick >= 10) {
            currCycleTick = 1;
        } else {
            currCycleTick++;
        }
    }

    @Override
    protected void render(MatrixStack matrices, float tickDelta) {
        float scale = UnofficialMonumentaModClient.options.situationals_scalefactor;
        matrices.push();
        matrices.scale(scale, scale, 0);
        Rectangle pos = getDimension();
        boolean horizontal = UnofficialMonumentaModClient.options.situationals_horizontal;
        int x = 0;
        int y = 0;
        Rectangle editModeDimension = new Rectangle(0, 0, pos.width, pos.height);

        if (UnofficialMonumentaModClient.options.situationals_renderbg) {
            DrawableHelper.fill(matrices, editModeDimension.x, editModeDimension.y, (int) editModeDimension.getMaxX(), (int) editModeDimension.getMaxY(), BACKGROUND_COLOR);
            GuiUtils.renderOutline(matrices, editModeDimension);
        }

        //render from max to min position.
        if (isInEditMode()) {
            //ACTIVE/INACTIVE SPRITES
            final Sprite cloakedActive = getSprite(Utils.sanitizeForIdentifier("icons/cloakedactive"));
            final Sprite shieldingInactive = getSprite(Utils.sanitizeForIdentifier("icons/shieldinginactive"));
            //ANIMATED SPRITES
            final Sprite bubble = getSprite(Utils.sanitizeForIdentifier("fx/bubble/bubble1"));
            final Sprite steadfast = getSprite(Utils.sanitizeForIdentifier("icons/steadfast"));

            drawSpriteOrEmpty(matrices, cloakedActive, x+4, y+4, SPRITE_SIZE, SPRITE_SIZE);

            if (horizontal) {x+=ELEMENT_SIZE;} else {y+=ELEMENT_SIZE;}
            drawSpriteOrEmpty(matrices, shieldingInactive, x+4, y+4, SPRITE_SIZE, SPRITE_SIZE);

            if (horizontal) {x+=ELEMENT_SIZE;} else {y+=ELEMENT_SIZE;}
            drawSpriteOrEmpty(matrices, bubble, x+4, y+4, SPRITE_SIZE, SPRITE_SIZE);

            if (horizontal) {x+=ELEMENT_SIZE;} else {y+=ELEMENT_SIZE;}
            drawSpriteOrEmpty(matrices, steadfast, x+4, y+4, SPRITE_SIZE, SPRITE_SIZE);
            matrices.pop();
            return;
        }

        for (SituationalData data: equippedSituationals.values()) {
            Sprite situationalSprite = getSprite(Utils.sanitizeForIdentifier("icons/"+ data.name));
            drawSpriteOrEmpty(matrices, situationalSprite, x+4, y+4, SPRITE_SIZE, SPRITE_SIZE);
            if (horizontal) {x+=ELEMENT_SIZE;} else {y+=ELEMENT_SIZE;}
        }
        matrices.pop();
    }

    public Identifier getIdentifier(String id) {
        if (identifiers.containsKey(id)) {
            return identifiers.get(id);
        }
        return null;
    }

    public Sprite getSprite(String id) {
        Identifier identifier = getIdentifier(id);
        String path = identifier.getPath();

        boolean isAnimatedSprite = false;
        Identifier animationSpritePath = null;
        if (Character.isDigit(path.charAt(path.length()-1))) {
            String newPath = path.replaceAll("\\d+$", "");
            //is an animated sprite

            isAnimatedSprite = true;
            animationSpritePath = getIdentifier(path.substring(0, newPath.length()) + currCycleTick);
        }
        if (getIdentifier(path+"1") != null) {
            //is most likely an animated sprite
            isAnimatedSprite = true;
            animationSpritePath = getIdentifier(path + currCycleTick);
        }

        if (isAnimatedSprite) {
            if (animationSpritePath == null) {
                return null;
            }
            Sprite tickSprite = Atlases.SITUATIONAL_ATLAS.getSprite(animationSpritePath);

            if (!tickSprite.getContents().getId().equals(MissingSprite.getMissingSpriteId())) {
                return tickSprite;
            } else {
                return null;//empty tick
            }
        }

        //just return what it asked for.
        return Atlases.SITUATIONAL_ATLAS.getSprite(identifier);
    }

    public void drawSpriteOrEmpty(MatrixStack matrices, @Nullable Sprite sprite, int x, int y, int width, int height) {
        if (sprite == null) {
            return;
        }
        drawSprite(matrices, sprite, x, y, width, height);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            super.mouseReleased(mouseX, mouseY, button);
            //I'd move this in mouseDragged, but I can't access private stuff, so it'll be moved in the UI library later on.
            float overlayScale = UnofficialMonumentaModClient.options.situationals_scalefactor;

            int scaledWidth = client.getWindow().getScaledWidth();
            int scaledHeight = client.getWindow().getScaledHeight();

            //stop it from reaching going offscreen.
            Rectangle dimension = getDimension();
            int x = dimension.x;
            int y = dimension.y;
            //stop left overflow and full right overflow.
            int newX = Math.max(0, (Math.min(x, scaledWidth)));
            //stop top overflow and full bottom overflow.
            int newY = Math.max(0, (Math.min(y, scaledHeight)));

            //stop right partial overflow.
            if (newX + dimension.width * overlayScale > scaledWidth) {
                int overflow = (int) (newX + (dimension.width * overlayScale) - scaledWidth);
                newX -= overflow;
            }
            //stop left partial overflow.
            if (newY + dimension.height * overlayScale > scaledHeight) {
                int overflow = (int) (newY + (dimension.height * overlayScale) - scaledHeight);
                newY -= overflow;
            }

            ElementPosition position = getPosition();

            // Offsets to the sides and centers of the screen. The smallest offset of each direction will be used as anchor point.
            double horizontalMiddle = Math.abs(newX + dimension.width / 2.0 - scaledWidth / 2.0);
            double right = scaledWidth - (newX + dimension.width);
            double verticalMiddle = Math.abs(newY + dimension.height / 2.0 - scaledHeight / 2.0);
            double bottom = scaledHeight - (newY + dimension.height);

            position.offsetXRelative = (double) newX < horizontalMiddle && (double) newX < right ? 0 : horizontalMiddle < right ? 0.5f : 1;
            position.offsetYRelative = (double) newY < verticalMiddle && (double) newY < bottom ? 0 : verticalMiddle < bottom ? 0.5f : 1;
            position.alignX = position.offsetXRelative;
            position.alignY = position.offsetYRelative;
            position.offsetXAbsolute = Math.round(newX - scaledWidth * position.offsetXRelative + position.alignX * dimension.width);
            position.offsetYAbsolute = Math.round(newY - scaledHeight * position.offsetYRelative + position.alignY * dimension.height);

            // snap to center
            if (!Screen.hasAltDown()) {
                if (position.offsetXRelative == 0.5f && Math.abs(position.offsetXAbsolute) < 10) {
                    position.offsetXAbsolute = 0;
                }
                if (position.offsetYRelative == 0.5f && Math.abs(position.offsetYAbsolute) < 10) {
                    position.offsetYAbsolute = 0;
                }
            }

            UnofficialMonumentaModClient.saveConfig();
        }
        return false;
    }

    public static class SituationalData {
        String name;
        SituationalState state = SituationalState.INACTIVE;

        public void setState(SituationalState state) {
            this.state = state;
        }
    }

    public enum SituationalState {
        INACTIVE(),
        ACTIVE(),
        BROKEN()
    }
}
