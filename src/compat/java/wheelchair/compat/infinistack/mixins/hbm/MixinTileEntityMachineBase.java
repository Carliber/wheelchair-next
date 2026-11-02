package wheelchair.compat.mixins.hbm;

import com.hbm.tileentity.TileEntityMachineBase; // hbm\tileentity\TileEntityMachineBase.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityMachineBase.class)
@Pseudo
public abstract class MixinTileEntityMachineBase {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}