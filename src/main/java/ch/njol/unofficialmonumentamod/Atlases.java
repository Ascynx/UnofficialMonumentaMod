package ch.njol.unofficialmonumentamod;

import ch.njol.minecraft.uiframework.ModSpriteAtlasHolder;
import ch.njol.unofficialmonumentamod.features.misc.SlotLocking;
import ch.njol.unofficialmonumentamod.hud.AbilitiesHud;
import ch.njol.unofficialmonumentamod.hud.SituationalWidget;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class Atlases {
    @MonotonicNonNull
    public static ModSpriteAtlasHolder SITUATIONAL_ATLAS = null;
    @MonotonicNonNull
    public static ModSpriteAtlasHolder ABILITIES_ATLAS = null;
    @MonotonicNonNull
    public static ModSpriteAtlasHolder GUI_ATLAS = null;

    private static void registerSituationalsAtlas() {
        if (SITUATIONAL_ATLAS == null) {
            SITUATIONAL_ATLAS = ModSpriteAtlasHolder.createAtlas(UnofficialMonumentaModClient.MOD_IDENTIFIER, "situationals");
        } else {
            SITUATIONAL_ATLAS.clearSprites();
        }
        SituationalWidget.identifiers.clear();

        List<Identifier> foundSprites = MinecraftClient.getInstance().getResourceManager().findResources("textures/situationals", path -> true)
                .keySet().stream()
                .filter(id -> id.getNamespace().equals(UnofficialMonumentaModClient.MOD_IDENTIFIER)).toList();
        for (Identifier foundSprite: foundSprites) {
            String key = foundSprite.getPath().substring("textures/situationals/".length(), foundSprite.getPath().length() - ".png".length());
            SituationalWidget.identifiers.put(key, SITUATIONAL_ATLAS.registerSprite(key));
        }
    }

    private static void registerAbilitiesAtlas() {
        if (ABILITIES_ATLAS == null) {
            ABILITIES_ATLAS = ModSpriteAtlasHolder.createAtlas(UnofficialMonumentaModClient.MOD_IDENTIFIER, "abilities");
        } else {
            ABILITIES_ATLAS.clearSprites();
        }
        AbilitiesHud.COOLDOWN_OVERLAY = ABILITIES_ATLAS.registerSprite("cooldown_overlay");
        AbilitiesHud.COOLDOWN_FLASH = ABILITIES_ATLAS.registerSprite("off_cooldown");
        AbilitiesHud.UNKNOWN_ABILITY_ICON = ABILITIES_ATLAS.registerSprite("unknown_ability");
        AbilitiesHud.UNKNOWN_CLASS_BORDER = ABILITIES_ATLAS.registerSprite("unknown_border");
        List<Identifier> foundIcons = MinecraftClient.getInstance().getResourceManager().findResources("textures/abilities", path -> true)
                .keySet().stream()
                .filter(id -> id.getNamespace().equals(UnofficialMonumentaModClient.MOD_IDENTIFIER)).toList();
        for (Identifier foundIcon : foundIcons) {
            if (foundIcon == AbilitiesHud.COOLDOWN_OVERLAY || foundIcon == AbilitiesHud.COOLDOWN_FLASH || foundIcon == AbilitiesHud.UNKNOWN_ABILITY_ICON || foundIcon == AbilitiesHud.UNKNOWN_CLASS_BORDER) {
                continue;
            }
            ABILITIES_ATLAS.registerSprite(foundIcon.getPath().substring("textures/abilities/".length(), foundIcon.getPath().length() - ".png".length()));
        }
    }

    private static void registerGuiAtlas() {
        if (GUI_ATLAS == null) {
            GUI_ATLAS = ModSpriteAtlasHolder.createAtlas(UnofficialMonumentaModClient.MOD_IDENTIFIER, "gui");
        } else {
            GUI_ATLAS.clearSprites();
        }
        SlotLocking.LOCK = GUI_ATLAS.registerSprite("locks/locked");
        SlotLocking.LEFT_CLICK_LOCK = GUI_ATLAS.registerSprite("locks/left-click");
        SlotLocking.RIGHT_CLICK_LOCK = GUI_ATLAS.registerSprite("locks/right-click");
        SlotLocking.DROP_LOCK = GUI_ATLAS.registerSprite("locks/drop");
        SlotLocking.BASE_LOCK  = GUI_ATLAS.registerSprite("locks/base-lock");
    }

    public static void registerSprites() {
        registerSituationalsAtlas();
        registerAbilitiesAtlas();
        registerGuiAtlas();
    }
}
