package wheelchair.compat.mixins;

import net.minecraft.item.ItemPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wheelchair.compat.helper.Configurables;

@Mixin(ItemPotion.class)
public abstract class MixinItemPotion {

    //Well, potions should be stackable so I make this.

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(CallbackInfo ci) {
        ((ItemPotion)(Object)this).setMaxStackSize(Configurables.maxStackSize);
    }
}