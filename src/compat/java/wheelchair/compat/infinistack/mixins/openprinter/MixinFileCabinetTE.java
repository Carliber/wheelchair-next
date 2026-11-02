package wheelchair.compat.mixins.openprinter;

import pcl.openprinter.tileentity.FileCabinetTE;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(FileCabinetTE.class)
@Pseudo
public abstract class MixinFileCabinetTE {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
