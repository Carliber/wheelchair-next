package wheelchair.proxy;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

public class ClientProxy extends CommonProxy {
    public static KeyBinding terminalKey;

    @Override
    public void preInit() {
        // client-thread hop for S->C packet work (mirror of ServerTaskQueue)
        FMLCommonHandler.instance().bus().register(new wheelchair.client.ClientTaskQueue());
        terminalKey = new KeyBinding("key.wheelchair.terminal", Keyboard.KEY_O, "key.categories.wheelchair");
        ClientRegistry.registerKeyBinding(terminalKey);
        FMLCommonHandler.instance().bus().register(new wheelchair.client.KeybindHandler());
        if (wheelchair.client.DebugBridge.enabled()) wheelchair.client.DebugBridge.start();
        // world-space mark highlight (energy marks: green glow / translucent red)
        wheelchair.client.MarkRenderHandler.install();
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
        if (id == 0) {
            int rows = Math.max(3, Math.min(10, x));
            return new wheelchair.client.GuiStoragePage(
                    new wheelchair.inventory.ContainerStoragePage(p.inventory, rows), rows);
        }
        if (id == 4) {
            int rows = Math.max(3, Math.min(10, x));
            return new wheelchair.client.GuiEnchPage(
                    new wheelchair.inventory.ContainerEnch(p.inventory, rows), rows);
        }
        if (id == 5) {
            int rows = Math.max(3, Math.min(10, x));
            return new wheelchair.client.GuiStaffPage(
                    new wheelchair.inventory.ContainerStaff(p.inventory), rows);
        }
        if (id == 6) {
            return new wheelchair.client.GuiEnergyPage(
                    new wheelchair.inventory.ContainerEnergy(p.inventory));
        }
        return null;
    }

    @Override
    public void postInit() {
        wheelchair.client.GuiStoragePage.installBigCountRendererPublic();
    }
}
