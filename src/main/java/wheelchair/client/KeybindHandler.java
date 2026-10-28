package wheelchair.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import wheelchair.WheelchairMod;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

@SideOnly(Side.CLIENT)
public class KeybindHandler {
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.currentScreen != null) return;
        if (wheelchair.proxy.ClientProxy.terminalKey.isPressed()) {
            // ask the server to open the GUI; rows = AE2-style adaptive count for our canvas
            net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(
                    mc, mc.displayWidth, mc.displayHeight);
            int rows = wheelchair.client.GuiStoragePage.rowsFor(sr.getScaledHeight());
            WheelchairMod.network.sendToServer(new wheelchair.network.PacketOpenTerminal(0, rows));
        }
    }
}
