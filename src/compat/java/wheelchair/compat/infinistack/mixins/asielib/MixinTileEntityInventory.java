package wheelchair.compat.mixins.asielib;

import pl.asie.lib.block.TileEntityInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityInventory.class)
@Pseudo
public abstract class MixinTileEntityInventory {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
