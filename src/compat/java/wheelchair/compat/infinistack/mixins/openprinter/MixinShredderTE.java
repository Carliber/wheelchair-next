package wheelchair.compat.mixins.openprinter;

import pcl.openprinter.tileentity.ShredderTE;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(ShredderTE.class)
@Pseudo
public abstract class MixinShredderTE {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
