package wheelchair.network;

import java.util.ArrayList;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client mirror of the energy-mark state (filled by PacketEnergySync on the
 * client thread). Consumers: GuiEnergyPage + RenderMarkWorld.
 */
@SideOnly(Side.CLIENT)
public final class EnergyMirror {

    /** {dim,x,y,z,store,eu,rate,nameKey} — same shape as PacketEnergySync.rows */
    public static final ArrayList<Object[]> rows = new ArrayList<Object[]>();
    public static long pool;
    public static int version;

    public static void accept(PacketEnergySync m) {
        rows.clear();
        rows.addAll(m.rows);
        pool = m.pool;
        version++;
    }
}
