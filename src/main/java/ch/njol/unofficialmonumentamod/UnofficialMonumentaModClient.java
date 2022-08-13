package ch.njol.unofficialmonumentamod;

import ch.njol.unofficialmonumentamod.discordrpc.DiscordRPC;
import ch.njol.unofficialmonumentamod.misc.Calculator;
import ch.njol.unofficialmonumentamod.misc.managers.CooldownManager;
import ch.njol.unofficialmonumentamod.misc.Locations;
import ch.njol.unofficialmonumentamod.misc.managers.Notifier;
import ch.njol.unofficialmonumentamod.misc.notifications.LocationNotifier;
import ch.njol.unofficialmonumentamod.options.Options;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Objects;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class UnofficialMonumentaModClient implements ClientModInitializer {

	// TODO:
	// sage's insight has no ClassAbility, but has stacks
	// spellshock however has a ClassAbility, but doesn't really need to be displayed...
	// build calculator with a custom gui ?

	public static final String MOD_IDENTIFIER = "unofficial-monumenta-mod";

	public static final String OPTIONS_FILE_NAME = "unofficial-monumenta-mod.json";

	public static final Logger LOGGER = LogManager.getLogger(MOD_IDENTIFIER);

	public static Options options = new Options();

	public static DiscordRPC discordPresence = new DiscordRPC();

	public static Locations locations = new Locations();

	public static final AbilityHandler abilityHandler = new AbilityHandler();

	private static Boolean onMonumenta;

	// This is a hacky way to pass data around...
	public static boolean isReorderingAbilities = false;

	@Override
	public void onInitializeClient() {

		FabricModelPredicateProviderRegistry.register(new Identifier("on_head"),
			(itemStack, clientWorld, livingEntity) -> livingEntity != null && itemStack == livingEntity.getEquippedStack(EquipmentSlot.HEAD) ? 1 : 0);

		try {
			options = readJsonFile(Options.class, OPTIONS_FILE_NAME);
		} catch (FileNotFoundException e) {
			// Config file doesn't exist, so use default config (and write config file).
			writeJsonFile(options, OPTIONS_FILE_NAME);
		} catch (IOException | JsonParseException e) {
			// Any issue with the config file silently reverts to the default config
			e.printStackTrace();
		}

		if (options.discordEnabled) discordPresence.init();

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			abilityHandler.tick();
			Calculator.tick();
			CooldownManager.update();
		});

		ClientTickEvents.END_WORLD_TICK.register(world -> {
			Notifier.tick();
		});

		ClientPlayNetworking.registerGlobalReceiver(ChannelHandler.CHANNEL_ID, new ChannelHandler());

	}

	public static boolean isOnMonumenta() {
		if (onMonumenta != null) return onMonumenta;
		Boolean onMM = null;

		MinecraftClient mc = MinecraftClient.getInstance();
		if (Locations.getShard() != null) onMM = true;
		if (onMM == null) onMM = !mc.isInSingleplayer() && Objects.requireNonNull(mc.getCurrentServerEntry()).address.toLowerCase().endsWith(".playmonumenta.com");

		onMonumenta = onMM;
		return onMM;
	}

	public static void onDisconnect() {
		abilityHandler.onDisconnect();
		Notifier.onDisonnect();
		LocationNotifier.onDisconnect();
		onMonumenta = null;
	}

	public static <T> T readJsonFile(Class<T> c, String filePath) throws IOException, JsonParseException {
		try (FileReader reader = new FileReader(FabricLoader.getInstance().getConfigDir().resolve(filePath).toFile())) {
			return new GsonBuilder().create().fromJson(reader, c);
		}
	}

	public static void writeJsonFile(Object o, String filePath) {
		try (FileWriter writer = new FileWriter((FabricLoader.getInstance().getConfigDir().resolve(filePath).toFile()))) {
			writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(o));
		} catch (NoSuchFileException | FileNotFoundException e) {
			if ((FabricLoader.getInstance().getConfigDir().resolve(filePath).toFile()).getParentFile().mkdirs()) {
				writeJsonFile(o, filePath);
			}
		} catch (IOException e) {
			// Silently ignore save errors
			e.printStackTrace();
		}
	}

	public static void saveConfig() {
		MinecraftClient.getInstance().execute(() -> {
			writeJsonFile(options, OPTIONS_FILE_NAME);
		});
	}

	public static boolean isAbilityVisible(AbilityHandler.AbilityInfo abilityInfo, boolean forSpaceCalculation) {
		// Passive abilities are visible iff passives are enabled in the options
		if (abilityInfo.initialCooldown == 0 && abilityInfo.maxCharges == 0) {
			return options.abilitiesDisplay_showPassiveAbilities;
		}

		// Active abilities take up space even if hidden unless condenseOnlyOnCooldown is enabled
		if (forSpaceCalculation && !UnofficialMonumentaModClient.options.abilitiesDisplay_condenseOnlyOnCooldown) {
			return true;
		}

		// Active abilities are visible with showOnlyOnCooldown iff they are on cooldown or don't have a cooldown (and should have stacks instead)
		return !options.abilitiesDisplay_showOnlyOnCooldown
			       || isReorderingAbilities
			       || abilityInfo.remainingCooldown > 0
			       || abilityInfo.maxCharges > 0 && (abilityInfo.initialCooldown <= 0 || options.abilitiesDisplay_alwaysShowAbilitiesWithCharges);
	}

}
