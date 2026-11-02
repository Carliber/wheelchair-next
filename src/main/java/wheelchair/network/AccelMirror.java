package wheelchair.network;

import java.util.ArrayList;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client mirror of the accel-mark state (filled by PacketAccelSync on the
 * client thread). Consumers: GuiAccelPage + RenderMarkWorld.
 */
@SideOnly(Side.CLIENT)
public final class AccelMirror {

    /** {isEntity,dim,x,y,z,entityId,radius,rate,remain,cov,nameKey} */
    public static final ArrayList<Object[]> rows = new ArrayList<Object[]>();
    public static boolean on = true;
    public static int version;

    public static void accept(PacketAccelSync m) {
        rows.clear();
        rows.addAll(m.rows);
        on = m.on;
        version++;
    }
}
