package wheelchair.compat.mixins.manametal;

import project.studio.manametalmod.atlantis.TileEntityOceanEnergyGenerator; // atlantis\TileEntityOceanEnergyGenerator.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;


@Mixin(TileEntityOceanEnergyGenerator.class)
@Pseudo
public abstract class MixinTileEntityOceanEnergyGenerator {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}