package wheelchair.compat.mixins.thaumcraft;

import thaumcraft.common.tiles.TileChestHungry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileChestHungry.class)
@Pseudo
public abstract class MixinTileChestHungry {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
