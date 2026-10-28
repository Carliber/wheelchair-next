package wheelchair.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * 1.7.10 has no MinecraftServer.addScheduledTask; run netty-thread packet work
 * on the server tick thread via this queue. Must be a public named class:
 * FML's ASM event handler cannot reach anonymous or package-private listeners.
 */
public class ServerTaskQueue {
    private static final Queue<Runnable> tasks = new ConcurrentLinkedQueue<Runnable>();

    public static void add(Runnable r) { tasks.add(r); }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Runnable r;
        int guard = 0;
        while ((r = tasks.poll()) != null && guard++ < 1000) {
            try {
                r.run();
            } catch (Throwable t) {
                cpw.mods.fml.common.FMLLog.warning("[wheelchair] server task failed: %s",
                        new Object[]{String.valueOf(t)});
                StackTraceElement[] st = t.getStackTrace();
                for (int i = 0; i < Math.min(6, st.length); i++) {
                    cpw.mods.fml.common.FMLLog.warning("[wheelchair]   at %s", new Object[]{st[i].toString()});
                }
            }
        }
    }
}
