package wheelchair.compat.mixins.manametal;

import wheelchair.compat.helper.Configurables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(targets = "project.studio.manametalmod.produce.brewing.SlotPotion")
@Pseudo
public abstract class MixinSlotPotion {

    @Shadow(remap = false)
    int type;

    @Inject(
            method = {"func_75219_a", "getSlotStackLimit", "getSlotStackLimit"},
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void injectGetSlotStackLimit(CallbackInfoReturnable<Integer> cir){
        cir.setReturnValue(this.type == 0 ? 1 : Configurables.maxStackSize);
    }

}
