package wheelchair.compat.mixins.hbm;

import com.hbm.tileentity.network.TileEntityRequestNetworkContainer; // hbm\tileentity\network\TileEntityRequestNetworkContainer.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityRequestNetworkContainer.class)
@Pseudo
public abstract class MixinTileEntityRequestNetworkContainer {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}