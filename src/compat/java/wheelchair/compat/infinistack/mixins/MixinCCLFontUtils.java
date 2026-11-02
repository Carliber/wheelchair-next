package wheelchair.compat.mixins;

import codechicken.lib.render.FontUtils;
import wheelchair.compat.helper.Configurables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(FontUtils.class)
@Pseudo
public abstract class MixinCCLFontUtils {

    //Maybe later, I will add a config option to let it use my own display logic.
    //Right now, just let it calculate stack number based on maxStackSize.

    @ModifyConstant(
            method = "drawItemQuantity",
            constant = @Constant(intValue = 64),
            remap = false
    )
    private static int modifyMaxStackSize(int original){
        return Configurables.maxStackSize;
    }

}
