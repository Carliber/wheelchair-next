package wheelchair.mixin;

import net.minecraft.inventory.InventoryBasic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Same 64 hard-code as MixinInventoryPlayer, for the generic InventoryBasic
 * (creative-screen backing inventory and most simple vanilla inventories).
 * @Inject(HEAD, cancellable) — see MixinInventoryPlayer for the why.
 */
@Mixin(InventoryBasic.class)
public abstract class MixinInventoryBasic {

    @Inject(method = "getInventoryStackLimit", at = @At("HEAD"), cancellable = true)
    private void wheelchair$bigLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Integer.MAX_VALUE);
    }
}
