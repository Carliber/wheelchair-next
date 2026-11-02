package wheelchair.compat.mixins.galacticraft;

import micdoodle8.mods.galacticraft.planets.asteroids.inventory.InventorySchematicTier3Rocket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(InventorySchematicTier3Rocket.class)
@Pseudo
public abstract class MixinInventorySchematicTier3Rocket {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
