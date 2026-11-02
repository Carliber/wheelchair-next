package wheelchair.compat.mixins.hbm;

import com.hbm.tileentity.machine.storage.TileEntityCrateBase; // hbm\tileentity\machine\storage\TileEntityCrateBase.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityCrateBase.class)
@Pseudo
public abstract class MixinTileEntityCrateBase {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}