package ch.njol.unofficialmonumentamod.misc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.options.Options;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.apache.commons.lang3.ArrayUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtil;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static ch.njol.unofficialmonumentamod.Utils.isChestSortDisabledForInventory;

public class QuickUse {
    private static boolean RenderQuickActionMenu = false;

    private static final Options options = UnofficialMonumentaModClient.options;

    private static boolean DraggingMenu = false;
    /*
     *  Idea for pos modification mode for quickAction menu
     *  add an option in the settings to enter gui move mode, (will set renderQuickActionMenu to be always active when chat is on)
     * (when on, you can then drag and drop the gui's position) -> while it is in drag and drop show a rectangle instead of the normal texture
     */

    public static final int width = 101;
    public static final int height = 21;


    private static ArrayList<Integer> quickPotions = new ArrayList<>();
    private static final ArrayList<Integer> badEffects = new ArrayList<Integer>(ImmutableList.of(2, 4, 7, 9, 15, 17, 18, 19, 20, 27, 31));

    private static Integer quickSellSlot;
    private static final String[] quickSellItems = new String[]{"Perfect Crystallizer", "Crystallizer (U)", "Crystallizer", "Experiencinator (U)", "Experiencinator"};

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void onMainQuickActionPress() {
        if (mc.player == null) return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof GenericContainerScreen)) return;
        RenderQuickActionMenu = true;

        findItems();

        if (options.QuickSell.wasReleased()) {
            onQuickSellReleased();
            return;
        }
        if (options.QuickSort.wasReleased()) {
            onQuickSortingReleased();
            return;
        }
        /*
        IDEAS:
            quickPotions -> onQuickPotionPressed(); -> might be an issue due to cooldown.
            quickTesseract? -> would require a way to switch between found tesseracts (up key, down key);
         */
    }

    public static void onMainQuickActionReleased() {
        RenderQuickActionMenu = false;
    }
    public static boolean isRendered() {
        if (options.editGuiPosMode) {
            return true;
        } else return RenderQuickActionMenu;
    }

    public static boolean isDraggingMenu() {
        return DraggingMenu;
    }
    public static void setDraggingMenu(boolean bool) {
        DraggingMenu = bool;
    }

    public static void onQuickSellReleased() {
        if (quickSellSlot != null) {
            assert mc.player != null;
            if (mc.player.inventory.getCursorStack().isEmpty() && !mc.player.inventory.getStack(quickSellSlot).isEmpty() && ArrayUtils.contains(quickSellItems, mc.player.inventory.getStack(quickSellSlot).getName().asString())) {
                pickupPlayerInventorySlot(quickSellSlot);
            }
        } else {
            Notifier.addCustomToast(new NotificationToast(Text.of("quickSell"), Text.of("Tried to trigger but couldn't find necessary item."), Notifier.getMillisHideTime()).setToastRender(NotificationToast.RenderType.SYSTEM));
        }
    }
    public static void onQuickSortingReleased() {
        assert mc.player != null;
        ScreenHandler currentScreenHandler = mc.player.currentScreenHandler;
        if (isChestSortDisabledForInventory(currentScreenHandler, 0) || mc.currentScreen == null) {
            return;
        }

        Slot actionSlot = null;
        for (Slot slot: ((GenericContainerScreen) mc.currentScreen).getScreenHandler().slots) {
            if (slot.getStack().isEmpty()) {
                actionSlot = slot;
                break;
            }
        }

        if (actionSlot == null) {
            Notifier.addCustomToast(new NotificationToast(Text.of("quickSort"), Text.of("Tried to trigger but no empty slot was found in container."), Notifier.getMillisHideTime()).setToastRender(NotificationToast.RenderType.SYSTEM));
            return;
        }

        pickupContainerInventorySlot(actionSlot.id);
        pickupContainerInventorySlot(actionSlot.id);
    }
    public synchronized static void onQuickPotionReleased() {
        if (quickPotions.size() > 0) {
            for (Integer quickPotionSlot: quickPotions) {
                assert mc.player != null;
                if (mc.player.inventory.getCursorStack().isEmpty() && !mc.player.inventory.getStack(quickPotionSlot).isEmpty()) {
                    //get effects, check if the player has the effects or no
                    ArrayList<Boolean> hasEffects = new ArrayList<>();

                    for (StatusEffectInstance effect: mc.player.getActiveStatusEffects().values()) {
                        if (getPotionEffects(quickPotionSlot).contains(effect)) {
                            hasEffects.add(true);
                        }
                    }

                    if (hasEffects.size() < getPotionEffects(quickPotionSlot).size() && getPotionEffects(quickPotionSlot).stream().noneMatch((e) -> StatusEffect.getRawId(e.getEffectType()) == 6) && hasNegativeEffects(getPotionEffects(quickPotionSlot))) {
                        //player doesn't have all the effects and the potion doesn't have instant health
                        pickupPlayerInventorySlot(quickPotionSlot);
                        quickPotions.remove(quickPotionSlot);//remove from the list
                    } else if (getPotionEffects(quickPotionSlot).contains(new StatusEffectInstance(StatusEffect.byRawId(6)))) {//is an instant health potion
                        if (mc.player.getMaxHealth() > mc.player.getHealth()) {
                            //isn't full health
                            pickupPlayerInventorySlot(quickPotionSlot);
                            quickPotions.remove(quickPotionSlot);
                        }
                    }
                }
            }
        } else {
            Notifier.addCustomToast(new NotificationToast(Text.of("quickPotions"), Text.of("Tried to trigger but no potions found"), Notifier.getMillisHideTime()).setToastRender(NotificationToast.RenderType.SYSTEM));
        }
    }

    private static void pickupContainerInventorySlot(int slot) {
        if (mc.currentScreen == null || mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(((GenericContainerScreen) mc.currentScreen).getScreenHandler().syncId, slot, 1, SlotActionType.PICKUP, mc.player);
    }
    private static void pickupPlayerInventorySlot(int slot) {
        if (mc.player == null || mc.interactionManager == null) return;
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, slot, 1, SlotActionType.PICKUP, mc.player);
    }

    private static List<StatusEffectInstance> getPotionEffects(int slot) {
        if (mc.player == null) return ImmutableList.of();
        return PotionUtil.getPotionEffects(mc.player.inventory.getStack(slot));
    }
    private static Boolean hasNegativeEffects(List<StatusEffectInstance> effects) {
        for (StatusEffectInstance effect: effects) {
            if (badEffects.contains(StatusEffect.getRawId(effect.getEffectType()))) {
                return true;
            }
        }
        
        return false;
    }

    private static void findItems() {
        //reset
        quickPotions.clear();

        //create new list of candidates
        ArrayList<ItemCandidate> quickSellCandidates = new ArrayList<>();
        ArrayList<ItemCandidate> quickPotionCandidates = new ArrayList<>();

        assert mc.player != null;
        for (ItemStack item: mc.player.inventory.main) {
            String name = item.getName().asString();
            if (ArrayUtils.contains(quickSellItems, name)) {
                quickSellCandidates.add(new ItemCandidate(item, mc.player.inventory.getSlotWithStack(item)));
            }
            if (PotionUtil.getPotionEffects(item).size() > 0) {
                quickPotionCandidates.add(new ItemCandidate(item, mc.player.inventory.getSlotWithStack(item)));
            }
        }

        Integer[] goodEffects = new Integer[]{1, 3, 5, 6, 8, 10, 11, 12, 13, 14, 16, 21, 22, 23, 24, 26, 28, 29, 30, 32};

        ArrayList<ItemCandidate>[] Potions = new ArrayList[33];

        for (ItemCandidate candidate: quickPotionCandidates) {
            for (StatusEffectInstance i: PotionUtil.getPotionEffects(candidate.item)) {
                if (Potions[StatusEffect.getRawId(i.getEffectType())] == null) Potions[StatusEffect.getRawId(i.getEffectType())] = new ArrayList<>();
                Potions[StatusEffect.getRawId(i.getEffectType())].add(candidate);
            }
        }


        for (int i = 0; i < Potions.length; i++) {
            int finalI = i;
            if (Potions[i] == null || Arrays.stream(goodEffects).noneMatch((e) -> finalI == e)) continue;
            ArrayList<ItemCandidate> A = Potions[i];

            A.sort((e, v) -> {
                List<StatusEffectInstance> ec = PotionUtil.getPotionEffects(e.item);
                ec.sort(((a, b) -> StatusEffect.getRawId(a.getEffectType()) == finalI ? finalI : 0));

                List<StatusEffectInstance> vc = PotionUtil.getPotionEffects(v.item);
                vc.sort(((a, b) -> StatusEffect.getRawId(a.getEffectType()) == finalI ? finalI : 0));
                return vc.get(0).getAmplifier() != ec.get(0).getAmplifier() ? vc.get(0).getAmplifier() - ec.get(0).getAmplifier() : vc.get(0).getDuration() - ec.get(0).getDuration();
            });

            if (!quickPotions.contains(A.get(0).slot)) quickPotions.add(A.get(0).slot);
        }

        quickSellCandidates.sort(Comparator.comparingInt(i -> ArrayUtils.indexOf(quickSellItems, i.item.getName().asString())));

        if (quickSellCandidates.size() > 0) quickSellSlot = quickSellCandidates.get(0).slot;
    }

    public static boolean isMouseInBounds(double mouseX, double mouseY) {
        return mouseX >= (double)options.QuickActionMenuX && mouseX < (double)(options.QuickActionMenuX + width) && mouseY >= (double)options.QuickActionMenuY && mouseY < (double)(options.QuickActionMenuY + height);
    }

    public static class ItemCandidate {
     public Integer slot;
     public ItemStack item;

     public ItemCandidate(ItemStack item, Integer slot) {
         this.item = item;
         this.slot = slot;
     }
    }
}
