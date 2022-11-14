package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.features.locations.Locations;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	
	public LivingEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}
	
	@Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
	private void glowOverride(CallbackInfoReturnable<Boolean> cir) {
		if (UnofficialMonumentaModClient.options.highlightMaskedAssassins && Objects.equals(Locations.getShortShard(), "ruin") && this.getDisplayName().getString().equalsIgnoreCase("masked assassin")) {
			cir.setReturnValue(true);
		}
	}
}
