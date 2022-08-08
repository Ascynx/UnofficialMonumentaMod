package ch.njol.unofficialmonumentamod.mixins;

import ch.njol.unofficialmonumentamod.misc.managers.CooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenHandler.class)
public class ScreenHandlerMixin {
    @Inject(at=@At("HEAD"), method="method_30010")
    private void onPlayerInventoryInteraction(int i, int j, SlotActionType slotActionType, PlayerEntity playerEntity, CallbackInfoReturnable<ItemStack> cir) {
        if ((CooldownManager.shouldRender() && i < playerEntity.currentScreenHandler.slots.size()) && (i > 0) && CooldownManager.getCooldownFromItem(playerEntity.currentScreenHandler.getSlot(i).getStack()) != 0 && slotActionType == SlotActionType.PICKUP && j == 1) {
            CooldownManager.addCooldownToItem(playerEntity.currentScreenHandler.getSlot(i).getStack());
        } else if (!CooldownManager.shouldRender()) {
            CooldownManager.addCooldownToItem(playerEntity.currentScreenHandler.getSlot(i).getStack());
        }
    }
}
