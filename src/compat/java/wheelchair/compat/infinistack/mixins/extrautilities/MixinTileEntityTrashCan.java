package wheelchair.compat.mixins.extrautilities;

import com.rwtema.extrautils.tileentity.TileEntityTrashCan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityTrashCan.class)
@Pseudo
public abstract class MixinTileEntityTrashCan {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
