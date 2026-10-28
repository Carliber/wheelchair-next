package wheelchair.server;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import wheelchair.value.EnergyMarkHub;

/**
 * Server tick engine for energy marks: runs the transfer pass every tick and
 * re-broadcasts the mirror when it changed (mark added/removed/auto-cleaned)
 * plus a 1-second heartbeat so open energy pages see fresh rate telemetry.
 * Registered on the FML common bus from WheelchairMod init.
 */
public final class EnergyTick {

    private int beat;

    private EnergyTick() {}

    public static void install() {
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new EnergyTick());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        EnergyMarkHub hub = EnergyMarkHub.get();
        if (hub == null) return;
        int v0 = hub.version;
        hub.tick();
        World w0 = MinecraftServer.getServer().worldServers[0];
        // push when something changed, or on the 1s heartbeat (rate window = 20t)
        if (hub.version != v0 || ++beat >= 20) {
            beat = 0;
            wheelchair.network.EnergyNet.broadcast(hub, w0);
        }
    }
}
