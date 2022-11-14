package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.features.locations.Locations;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I"))
	private int getTeamColorValueRedirect(Entity instance) {
		return UnofficialMonumentaModClient.options.highlightMaskedAssassins && Objects.equals(Locations.getShortShard(), "ruin") && instance.getDisplayName().getString().equalsIgnoreCase("masked assassin") ? Formatting.RED.getColorValue() : instance.getTeamColorValue();
	}
}
