package wheelchair.mixin;

import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player-inventory slot limit: vanilla HARD-CODES 64 in getInventoryStackLimit
 * (InventoryPlayer:668) — a separate chain from Item.maxStackSize, so the
 * reflective intmax patch never lifts it. This gate caps every vanilla path
 * through the player inventory: Slot.getSlotStackLimit() ->
 * Container.mergeItemStack (shift-click moves), pickup merging
 * (InventoryPlayer.addItem / storePartialItemStack), drop pickup and the
 * creative screen's inventory rows.
 *
 * Implemented as @Inject(HEAD, cancellable) instead of @Overwrite: member
 * remapping for @Overwrite needs a refmap entry format our AP chain does not
 * emit, while @Inject method references map through the bare-name entries we
 * patch into the refmap at build time.
 */
@Mixin(InventoryPlayer.class)
public abstract class MixinInventoryPlayer {

    @Inject(method = "getInventoryStackLimit", at = @At("HEAD"), cancellable = true)
    private void wheelchair$bigLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Integer.MAX_VALUE);
    }
}
