package wheelchair.compat.mixins.thaumcraft;

import thaumcraft.common.tiles.TileMagicBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileMagicBox.class)
@Pseudo
public abstract class MixinTileMagicBox {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
