package wheelchair.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/** common (server) side of the GUI bridge */
public class CommonProxy implements cpw.mods.fml.common.network.IGuiHandler {
    public void preInit() {}

    public void postInit() {}

    @Override
    public Object getServerGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
        if (id == 0 && p instanceof net.minecraft.entity.player.EntityPlayerMP) {
            int rows = Math.max(3, Math.min(10, x)); // openGui x param carries the row count
            wheelchair.value.StorageData st = wheelchair.value.StorageData.get(w);
            // full snapshot on open: the client mirror starts from the truth
            wheelchair.network.StorageNet.broadcastFull((net.minecraft.entity.player.EntityPlayerMP) p, st);
            return new wheelchair.inventory.ContainerStoragePage(p.inventory, rows);
        }
        if (id == 4 && p instanceof net.minecraft.entity.player.EntityPlayerMP) {
            // enchant page: same adaptive rows as storage (x param), xp snapshot on open
            int rows = Math.max(3, Math.min(10, x));
            wheelchair.network.StorageNet.broadcastFull((net.minecraft.entity.player.EntityPlayerMP) p,
                    wheelchair.value.StorageData.get(w));
            return new wheelchair.inventory.ContainerEnch(p.inventory, rows);
        }
        if (id == 5 && p instanceof net.minecraft.entity.player.EntityPlayerMP) {
            // staff page: push the live profile so the GUI starts from the truth
            net.minecraft.entity.player.EntityPlayerMP mp = (net.minecraft.entity.player.EntityPlayerMP) p;
            wheelchair.WheelchairMod.network.sendTo(
                    new wheelchair.network.PacketStaffCfgSync(wheelchair.value.StaffProfile.get(w)), mp);
            return new wheelchair.inventory.ContainerStaff(p.inventory);
        }
        if (id == 6 && p instanceof net.minecraft.entity.player.EntityPlayerMP) {
            // energy page: push the mark mirror so the GUI starts from the truth
            wheelchair.value.EnergyMarkHub hub = wheelchair.value.EnergyMarkHub.get();
            if (hub != null) wheelchair.network.EnergyNet.broadcast(hub, w);
            return new wheelchair.inventory.ContainerEnergy(p.inventory, Math.max(3, Math.min(10, x)));
        }
        if (id == 7 && p instanceof net.minecraft.entity.player.EntityPlayerMP) {
            // accel page: push the mark mirror so the GUI starts from the truth
            wheelchair.value.AccelHub hub = wheelchair.value.AccelHub.get();
            if (hub != null) wheelchair.network.AccelNet.broadcast(hub);
            return new wheelchair.inventory.ContainerAccel(p.inventory, Math.max(3, Math.min(10, x)));
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
        return null;
    }
}
