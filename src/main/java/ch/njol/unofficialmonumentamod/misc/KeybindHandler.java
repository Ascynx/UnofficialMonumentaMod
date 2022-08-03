package ch.njol.unofficialmonumentamod.misc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.options.Options;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.TranslatableText;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class KeybindHandler {
    private static ArrayList<Integer> oldTick = new ArrayList<>();
    private static final Options options = UnofficialMonumentaModClient.options;

    public static void tick() {
        ArrayList<Integer> newTick = new ArrayList<>();

        if (isPressed(options.QuickAction.getKeycode())) {
                QuickUse.onMainQuickActionPress();
        } else if (wasPressed(options.QuickAction.getKeycode()) && !isPressed(options.QuickAction.getKeycode())) {
            QuickUse.onMainQuickActionReleased();
        }

        for (Field field: Options.class.getDeclaredFields()) {
            if (!field.getType().equals(keybind.class)) continue;
            try {
                    if (((keybind) field.get(options)).isPressed()) {
                        newTick.add(((keybind) field.get(options)).getKeycode());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        for (int keycode: oldTick) {
            if (isPressed(keycode) && !newTick.contains(keycode)) {
                newTick.add(keycode);
            }
        }
        oldTick = newTick;
    }

    private static boolean wasPressed(int keycode) {
        return oldTick.contains(keycode);
    }
    private static boolean isPressed(int keycode) {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), keycode);
    }

    public static class keybind {
        private final int keycode;

        public keybind(int keycode) {
            this.keycode = keycode;
        }

        public int getKeycode() {
            return keycode;
        }
        public String getKeyName() {
            Text text = new TranslatableText(InputUtil.fromKeyCode(keycode, -1).getTranslationKey());
            if (text.getString().matches(".*\\..*")) {
                return text.getString().split("\\.")[2].toUpperCase();
            } else return text.getString();
        }
        public boolean isPressed() {
            return KeybindHandler.isPressed(keycode);
        }
        public boolean wasPressed() {
            return KeybindHandler.wasPressed(keycode);
        }
    }
}
