package wheelchair.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

/**
 * Local-only HTTP debug bridge (trimmed port of the old-tree DebugBridge):
 * /state, /chat?msg= (run a command as the player), /join?name= (enter a test
 * world, creating it creative+cheats), /screenshot (vanilla screenshot to
 * <gameDir>/screenshots, path returned). Enabled by the marker file
 * config/wheelchair_debug.enable under the game dir (dev: eclipse/).
 */
@SideOnly(Side.CLIENT)
public class DebugBridge {
    private static final Queue<Runnable> tasks = new ConcurrentLinkedQueue<Runnable>();

    public static boolean enabled() {
        try {
            return new java.io.File(Minecraft.getMinecraft().mcDataDir,
                    "config/wheelchair_debug.enable").exists();
        } catch (Throwable t) {
            return false;
        }
    }

    public static void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8123), 0);
            server.createContext("/", new HttpHandler() {
                public void handle(HttpExchange ex) {
                    DebugBridge.handle(ex);
                }
            });
            server.setExecutor(null);
            server.start();
            FMLLog.info("[wheelchair] debug bridge listening on 127.0.0.1:8123", new Object[0]);
            FMLCommonHandler.instance().bus().register(new DebugBridge());
        } catch (Throwable t) {
            FMLLog.warning("[wheelchair] debug bridge failed to start: %s",
                    new Object[]{String.valueOf(t)});
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Runnable r;
        while ((r = tasks.poll()) != null) {
            try {
                r.run();
            } catch (Throwable t) {
                FMLLog.warning("[wheelchair] bridge task failed: %s", new Object[]{String.valueOf(t)});
            }
        }
    }

    private static void handle(HttpExchange ex) {
        String resp = "{}";
        try {
            String path = ex.getRequestURI().getPath();
            Map<String, String> q = query(ex.getRequestURI().getRawQuery());
            final Minecraft mc = Minecraft.getMinecraft();
            if (path.equals("/state")) {
                resp = "{\"screen\":\"" + (mc.currentScreen == null ? "none"
                        : mc.currentScreen.getClass().getSimpleName()) + "\""
                        + ",\"world\":" + (mc.theWorld != null) + "}";
            } else if (path.equals("/chat")) {
                final String msg = q.containsKey("msg") ? q.get("msg") : "";
                tasks.add(new Runnable() {
                    public void run() {
                        if (mc.thePlayer != null) mc.thePlayer.sendChatMessage(msg);
                    }
                });
                resp = "{\"queued\":true}";
            } else if (path.equals("/join")) {
                final String name = q.containsKey("name") ? q.get("name") : "dbg";
                if (mc.theWorld != null) {
                    resp = "{\"error\":\"already in world\"}";
                } else {
                    tasks.add(new Runnable() {
                        public void run() {
                            mc.launchIntegratedServer(name, name,
                                    new net.minecraft.world.WorldSettings(0,
                                            net.minecraft.world.WorldSettings.GameType.CREATIVE,
                                            true, false,
                                            net.minecraft.world.WorldType.DEFAULT).enableCommands());
                        }
                    });
                    resp = "{\"queued\":true}";
                }
            } else if (path.equals("/screenshot")) {
                final File dir = new File(".");
                final String name = "shot_" + System.currentTimeMillis() + ".png";
                tasks.add(new Runnable() {
                    public void run() {
                        ScreenShotHelper.saveScreenshot(dir, name,
                                mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                    }
                });
                resp = "{\"saved\":\"" + new File(new File(dir, "screenshots"), name).getAbsolutePath() + "\"}";
            } else {
                resp = "{\"endpoints\":[\"/state\",\"/chat?msg=\",\"/join?name=\",\"/screenshot\"]}";
            }
            ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            byte[] b = resp.getBytes("UTF-8");
            ex.sendResponseHeaders(200, b.length);
            OutputStream os = ex.getResponseBody();
            os.write(b);
            os.close();
        } catch (Throwable t) {
            try {
                byte[] b = ("{\"error\":\"" + t + "\"}").getBytes("UTF-8");
                ex.sendResponseHeaders(500, b.length);
                OutputStream os = ex.getResponseBody();
                os.write(b);
                os.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> m = new TreeMap<String, String>();
        if (raw == null) return m;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            try {
                m.put(pair.substring(0, eq), URLDecoder.decode(pair.substring(eq + 1), "UTF-8"));
            } catch (Throwable ignored) {
            }
        }
        return m;
    }
}
