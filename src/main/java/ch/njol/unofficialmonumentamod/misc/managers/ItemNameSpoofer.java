package ch.njol.unofficialmonumentamod.misc.managers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import ch.njol.unofficialmonumentamod.Utils;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import static ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient.writeJsonFile;

public class ItemNameSpoofer {
    private static ArrayList<Spoof> spoofedNames = new ArrayList<>();
    private static final String CACHE_PATH = "monumenta/spoofed-item-names.json";

    //Will not change the actual item name -> will only change when the item name is called (tooltip and when the player switch to a slot that has that item)
    public static int commandNameSpoofer(CommandContext<FabricClientCommandSource> context) {
        final MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return 0;
        String spoofName = StringArgumentType.getString(context, "name");
        spoofName = spoofName.replaceAll("&&", "§");
        if (getUuid(mc.player.getMainHandStack()) != null) {
            ItemNameSpoofer.addSpoof(new Spoof(getUuid(mc.player.getMainHandStack()), spoofName));
            mc.inGameHud.getChatHud().addMessage(Text.of("Successfully spoofed item name to " + spoofName));
        } else mc.inGameHud.getChatHud().addMessage(Text.of("Item doesn't have a UUID (couldn't add spoofed name)"));
        return 1;
    }

    public static void save() {
        writeJsonFile(spoofedNames, CACHE_PATH);
    }

    private static void loadJson(String jsonString) {
        JsonParser jsonParser = new JsonParser();

        JsonArray json = jsonParser.parse(jsonString).getAsJsonArray();
        Gson gson = new Gson();
        Type type = new TypeToken<Spoof>(){}.getType();

        spoofedNames.clear();
        for (JsonElement element: json) {
            addSpoof(gson.fromJson(element, type));
        }
    }

    public static void load() {
        try {
            String cache = Utils.readFile(CACHE_PATH);
            loadJson(cache);

        } catch (FileNotFoundException | NoSuchFileException e) {
            writeJsonFile(spoofedNames, CACHE_PATH);//create with "empty" values

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addSpoof(Spoof spoofed) {
        boolean exists = false;
        for (Spoof spoof: spoofedNames) {
            if (!spoofed.getUuid().equals(spoof.getUuid())) continue;
            spoof.setName(spoofed.getName().getString());
            exists = true;
            break;
        }
        if (!exists) spoofedNames.add(spoofed);
        save();
    }

    public static MutableText getSpoofedName(ItemStack itemStack) {
        try {
            if (itemStack.getTag() == null) return new LiteralText("").append(itemStack.getName());
            if (itemStack.getTag().get("AttributeModifiers") != null && itemStack.getTag().getList("AttributeModifiers", 9) != null) {
                UUID uuid = ((ListTag) itemStack.getTag().get("AttributeModifiers")).getCompound(0).getUuid("UUID");
                for (Spoof spoofed: spoofedNames) {
                    if (spoofed.getUuid().equals(uuid)) {
                        return spoofed.getName();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new LiteralText("").append(itemStack.getName());
    }

    public static void remove(UUID uuid) {
        for (Spoof spoof: spoofedNames) {
            if (spoof.getUuid().equals(uuid)) {
                spoofedNames.remove(spoof);
                break;
            }
        }
    }

    @Nullable
    public static UUID getUuid(ItemStack itemStack) {
        if (itemStack.getTag().get("AttributeModifiers") != null && itemStack.getTag().getList("AttributeModifiers", 9) != null) {
            UUID uuid = ((ListTag) itemStack.getTag().get("AttributeModifiers")).getCompound(0).getUuid("UUID");
            return uuid;
        }
        return null;
    }

    public static void EditOrDeleteExistingSpoof(UUID itemUuid, @Nullable Text text) {
        if (text == null || Objects.equals(text.getString(), "")) {
            for (Spoof spoofed: spoofedNames) {
                if (spoofed.getUuid() == itemUuid) {
                    spoofedNames.remove(spoofed);
                    return;
                }
            }
        } else {
            for (Spoof spoofed: spoofedNames) {
                if (spoofed.getUuid() == itemUuid) {
                    spoofed.setName(text.getString());
                    return;
                }
            }
        }
    }

    public static class Spoof {
        private final UUID uuid;
            private String name;

        public Spoof(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public UUID getUuid() {
            return uuid;
        }

        public MutableText getName() {
            MutableText literal = new LiteralText(name);
            return literal;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
