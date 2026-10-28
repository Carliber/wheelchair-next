package wheelchair.client;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Client mirror of ServerTaskQueue: 1.7.10 simpleimpl delivers packets on the
 * netty thread, GUI/model work must hop to the client thread.
 */
public class ClientTaskQueue {
    private static final Queue<Runnable> tasks = new ConcurrentLinkedQueue<Runnable>();

    public static void add(Runnable r) { tasks.add(r); }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Runnable r;
        int guard = 0;
        while ((r = tasks.poll()) != null && guard++ < 1000) {
            try {
                r.run();
            } catch (Throwable t) {
                System.out.println("[wheelchair] client task failed: " + t);
            }
        }
    }
}
