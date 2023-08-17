package ch.njol.unofficialmonumentamod.core.commands;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.hud.strike.ChestCountOverlay;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MainCommand {
    public static final Style TITLE_STYLE = Style.EMPTY.withColor(Formatting.AQUA);
    public static final Style KEY_STYLE = Style.EMPTY.withColor(Formatting.DARK_GRAY);
    public static final Style VALUE_STYLE = Style.EMPTY.withColor(Formatting.DARK_AQUA);
    public static final Style UNIMPORTANT_STYLE = Style.EMPTY.withColor(Formatting.GRAY);
    public static final Style MOD_STYLE = Style.EMPTY.withColor(Formatting.DARK_GREEN);

    public LiteralArgumentBuilder<FabricClientCommandSource> register() {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = LiteralArgumentBuilder.literal("umm");

        builder.then(ClientCommandManager.literal("disableChestCountError").executes((ctx) -> runExecuteDisableChestCountError()));

        builder.then(ClientCommandManager.literal("debug").then(ClientCommandManager.literal("addCount").then(ClientCommandManager.argument("count", IntegerArgumentType.integer(0)).executes((MainCommand::runAddCount)))));

        builder.then(ClientCommandManager.literal("info").executes(ctx -> runSelfInfo()));
        builder.then(ClientCommandManager.literal("info")
                .then(ClientCommandManager.literal("modlist")
                        .then(ClientCommandManager.literal("clip").executes(ctx -> runCopyInfo()))
                        .executes(ctx -> runModList())));

        return builder;
    }

    public String getName() {
        return MainCommand.class.getSimpleName();
    }

    private static MutableText getSelfInfo() {
        MutableText text = Text.literal("[Mod Info]").setStyle(TITLE_STYLE);
        if (FabricLoader.getInstance().isModLoaded(UnofficialMonumentaModClient.MOD_IDENTIFIER)) {
            ModMetadata thisMetadata = FabricLoader.getInstance().getModContainer(UnofficialMonumentaModClient.MOD_IDENTIFIER).get().getMetadata();
            Version version = thisMetadata.getVersion();
            String name = thisMetadata.getName();
            text.append(Text.literal("\nName: ").setStyle(KEY_STYLE))
                    .append(Text.literal(name).setStyle(VALUE_STYLE));
            text.append(Text.literal("\nVersion: ").setStyle(KEY_STYLE))
                    .append(Text.literal(version.getFriendlyString()).setStyle(VALUE_STYLE));
        }

        text.append(Text.literal("\nMinecraft: ").setStyle(KEY_STYLE))
                .append(Text.literal((MinecraftClient.getInstance().getGameVersion()) + "-" + SharedConstants.getGameVersion().getName()).setStyle(VALUE_STYLE));
        text.append(Text.literal("\nIn Development environment: ").setStyle(KEY_STYLE))
                .append(Text.literal(FabricLoader.getInstance().isDevelopmentEnvironment() ? "Yes" : "No").setStyle(VALUE_STYLE));
        return text;
    }

    private static MutableText getModList() {
        MutableText text = Text.literal("[Mod List]").setStyle(TITLE_STYLE);

        Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
        StringBuilder data = new StringBuilder();

        for (ModContainer mod: mods) {
            ModMetadata metadata = mod.getMetadata();
            if (metadata.getId().startsWith("fabric-") || metadata.getId().equals("minecraft") || metadata.getId().equals("java")) {
                continue;//Skip fabric apis, Minecraft and Java.
            }
            text.append(Text.literal("\n"+  metadata.getName()).setStyle(MOD_STYLE))
                    .append(Text.literal(" ("+ metadata.getId() + ") ").setStyle(UNIMPORTANT_STYLE))
                    .append(metadata.getVersion().getFriendlyString()).setStyle(MOD_STYLE);
            if (mod.getContainingMod().isPresent()) {
                text.append(Text.literal(" via " + mod.getContainingMod().get().getMetadata().getId()).setStyle(UNIMPORTANT_STYLE));
            }
        }

        return text;
    }

    private static int runCopyInfo() {
        MinecraftClient.getInstance().keyboard.setClipboard(getSelfInfo().append("\n").toString().concat(getModList().toString()));
        MutableText text = Text.literal("Copied info to clipboard").setStyle(Style.EMPTY.withColor(Formatting.DARK_GRAY));

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
        return 0;
    }

    private static int runSelfInfo() {
        if (FabricLoader.getInstance().getModContainer(UnofficialMonumentaModClient.MOD_IDENTIFIER).isEmpty()) {
            return 1;
        }
        MutableText text = getSelfInfo();

        //other "pages"
        text.append(Text.literal("\n[Press Here to show modlist]").setStyle(UNIMPORTANT_STYLE.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/umm info modlist"))));
        text.append(Text.literal("\n[Press Here to show current shard]").setStyle(UNIMPORTANT_STYLE.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ummShard debug loaded"))));

        //copy all info to clipboard
        text.append(Text.literal("\n[Press Here to copy to clipboard]").setStyle(UNIMPORTANT_STYLE.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/umm info clip"))));

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }

    private static int runModList() {
        MutableText text = getModList();

        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }

    public static int runExecuteDisableChestCountError() {
        if (!UnofficialMonumentaModClient.options.enableChestCountMaxError) {
            return 1;
        }
        UnofficialMonumentaModClient.options.enableChestCountMaxError = false;
        //wouldn't want to mitigate the effect of the command.
        UnofficialMonumentaModClient.saveConfig();
        MutableText text = Text.literal("[UMM] Successfully disabled warning message").setStyle(TITLE_STYLE);
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }

    public static int runAddCount(CommandContext<FabricClientCommandSource> commandContext) {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            MutableText text = Text.literal("[UMM] nuh uh, not happening.").setStyle(TITLE_STYLE);
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
            return 1;
        }
        int count = IntegerArgumentType.getInteger(commandContext, "count");
        ChestCountOverlay.INSTANCE.addCount(count);

        MutableText text = Text.literal("[UMM] added " + count + " to chestCountOverlay").setStyle(TITLE_STYLE);
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);

        return 0;
    }
}