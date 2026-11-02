package wheelchair.compat.mixins.wirelesscrafting;

import net.p455w0rd.wirelesscraftingterminal.common.inventory.WCTInventoryCraftResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(WCTInventoryCraftResult.class)
@Pseudo
public abstract class MixinWCTInventoryCraftResult {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
