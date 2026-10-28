package wheelchair.mixin;

import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Yields every big-stacking mixin when InfiniStack is on the classpath: both
 * mods patch the same vanilla hot spots (PacketBuffer append, ItemStack NBT
 * Count) and injecting twice would corrupt the wire format. Detection is by
 * CLASS PROBE, not Loader.isModLoaded — shouldApplyMixin runs before FML
 * finishes mod discovery. Called lazily on first target-class load, when the
 * other mod's classes are already on the LaunchClassLoader.
 */
public class WheelchairMixinPlugin implements IMixinConfigPlugin {

    private static Boolean infinistackPresent;

    private static boolean infinistackPresent() {
        if (infinistackPresent == null) {
            try {
                Class.forName("com.phasico.infinistack.InfiniStackMod", false, Launch.classLoader);
                infinistackPresent = Boolean.TRUE;
            } catch (Throwable t) {
                infinistackPresent = Boolean.FALSE;
            }
        }
        return infinistackPresent.booleanValue();
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (infinistackPresent()) {
            cpw.mods.fml.relauncher.FMLRelaunchLog.info(
                    "[wheelchair] InfiniStack detected: mixin %s disabled (it owns big stacking)",
                    new Object[]{mixinClassName});
            return false;
        }
        return true;
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
