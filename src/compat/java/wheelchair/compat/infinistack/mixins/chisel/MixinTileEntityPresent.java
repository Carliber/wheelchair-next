package wheelchair.compat.mixins.chisel;

import team.chisel.block.tileentity.TileEntityPresent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityPresent.class)
@Pseudo
public abstract class MixinTileEntityPresent {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
