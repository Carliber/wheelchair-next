package wheelchair.compat.mixins.amunra;

import de.katzenpapst.amunra.inventory.schematic.InventorySchematicShuttle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(InventorySchematicShuttle.class)
@Pseudo
public abstract class MixinInventorySchematicShuttle {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
