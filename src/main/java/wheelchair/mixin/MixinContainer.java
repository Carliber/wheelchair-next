package wheelchair.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Big-stack overflow guard in Container.slotClick: vanilla's right-click
 * half-take computes (stackSize + 1) / 2 — at stackSize == Integer.MAX_VALUE
 * the +1 wraps the int to MIN_VALUE and the negative count corrupts BOTH the
 * cursor stack and the slot (each then renders as a numberless single item).
 * Redirect every Slot.decrStackSize call in slotClick and clamp negative
 * counts back to a safe half. (Checked: InfiniStack 0.1.0 does NOT guard this.)
 */
@Mixin(Container.class)
public abstract class MixinContainer {

    @Redirect(method = "slotClick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/inventory/Slot;decrStackSize(I)Lnet/minecraft/item/ItemStack;"))
    private static ItemStack wheelchair$overflowGuard(Slot slot, int count) {
        if (count < 0 && slot.getStack() != null) {
            count = slot.getStack().stackSize / 2; // (N+1)/2 overflowed — safe floor-half
        }
        return slot.decrStackSize(count);
    }

    /**
     * Shift-click transfer loop (InfiniStack law): vanilla retries via RECURSION
     * (retrySlotClick) which overflows the stack with int-max materials —
     * verified crash in this very project. Loop in place, bounded, instead.
     */
    @Redirect(method = "slotClick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/inventory/Container;retrySlotClick(IIZLnet/minecraft/entity/player/EntityPlayer;)V"))
    private static void wheelchair$boundedRetry(Container self, int slotId, int clickedButton, boolean flag, EntityPlayer player) {
        if (slotId < 0) return;
        Slot slot = (Slot) self.inventorySlots.get(slotId);
        if (slot == null) return;
        for (int i = 0; i < 64; i++) {
            if (!slot.canTakeStack(player)) break;
            ItemStack transferred = self.transferStackInSlot(player, slotId);
            if (transferred == null) break;
            ItemStack current = slot.getStack();
            if (current == null || !transferred.isItemEqual(current)) break;
        }
    }

    /**
     * NOTE: drag-split (vanilla func_94525_a) — InfiniStack overwrites it via
     * the RAW SRG name, which does not exist in dev; revisit for production.
     */
}
