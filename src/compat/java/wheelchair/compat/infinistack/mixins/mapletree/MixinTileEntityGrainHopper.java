package wheelchair.compat.mixins.mapletree;

import ecru.MapleTree.tile.ecru_TileEntityGrainHopper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(ecru_TileEntityGrainHopper.class)
@Pseudo
public abstract class MixinTileEntityGrainHopper {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
