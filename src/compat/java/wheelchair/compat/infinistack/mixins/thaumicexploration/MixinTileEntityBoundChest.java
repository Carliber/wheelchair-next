package wheelchair.compat.mixins.thaumicexploration;

import flaxbeard.thaumicexploration.tile.TileEntityBoundChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityBoundChest.class)
@Pseudo
public abstract class MixinTileEntityBoundChest {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
