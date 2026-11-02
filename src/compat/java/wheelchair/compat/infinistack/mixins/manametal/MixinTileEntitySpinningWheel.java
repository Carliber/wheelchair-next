package wheelchair.compat.mixins.manametal;

import project.studio.manametalmod.produce.textile.TileEntitySpinningWheel; // produce\textile\TileEntitySpinningWheel.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;


@Mixin(TileEntitySpinningWheel.class)
@Pseudo
public abstract class MixinTileEntitySpinningWheel {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}