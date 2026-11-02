package wheelchair.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * C→S accel page action (discriminator 11).
 * op 0 = unmark [X] (isEntity/dim/x/y/z/entityId identify the row)
 * op 1/2 = radius chip step +1/-1 (block marks only)
 * op 3/4 = rate chip step +1/-1
 * op 5 = master switch toggle
 * Server mutates the hub and re-broadcasts the mirror.
 */
public class PacketAccelAction implements IMessage {

    public byte op;
    public boolean isEntity;
    public int dim, x, y, z, entityId;

    public PacketAccelAction() {}

    public PacketAccelAction(int op, boolean isEntity, int dim, int x, int y, int z, int entityId) {
        this.op = (byte) op;
        this.isEntity = isEntity;
        this.dim = dim; this.x = x; this.y = y; this.z = z;
        this.entityId = entityId;
    }

    @Override
    public void fromBytes(ByteBuf b) {
        op = b.readByte();
        isEntity = b.readBoolean();
        dim = b.readInt(); x = b.readInt(); y = b.readInt(); z = b.readInt();
        entityId = b.readInt();
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeByte(op);
        b.writeBoolean(isEntity);
        b.writeInt(dim); b.writeInt(x); b.writeInt(y); b.writeInt(z);
        b.writeInt(entityId);
    }

    public static class Handler implements IMessageHandler<PacketAccelAction, IMessage> {
        @Override
        public IMessage onMessage(final PacketAccelAction m, MessageContext ctx) {
            wheelchair.network.ServerTaskQueue.add(new Runnable() {
                public void run() {
                    wheelchair.value.AccelHub hub = wheelchair.value.AccelHub.get();
                    if (hub == null) return;
                    if (m.op == 5) {
                        hub.on = !hub.on;
                    } else {
                        wheelchair.value.AccelHub.Mark mk = m.isEntity
                                ? findEntity(hub, m.dim, m.entityId)
                                : findBlock(hub, m.dim, m.x, m.y, m.z);
                        if (mk == null) return;
                        if (m.op == 0) {
                            hub.marks.remove(mk);
                        } else if (m.op == 1 || m.op == 2) {
                            mk.radius = step(mk.radius, wheelchair.value.AccelHub.RADIUS_STEPS, m.op == 1 ? 1 : -1);
                            mk.teCache = null; // force rescan with the new radius
                            mk.randCache = null;
                        } else if (m.op == 3 || m.op == 4) {
                            mk.rate = step(mk.rate, wheelchair.value.AccelHub.RATE_STEPS, m.op == 3 ? 1 : -1);
                        }
                    }
                    hub.markDirty();
                    hub.version++;
                    wheelchair.network.AccelNet.broadcast(hub);
                }
            });
            return null;
        }

        private static wheelchair.value.AccelHub.Mark findBlock(wheelchair.value.AccelHub hub, int dim, int x, int y, int z) {
            for (int i = 0; i < hub.marks.size(); i++) {
                wheelchair.value.AccelHub.Mark mk = hub.marks.get(i);
                if (!mk.isEntity && mk.dim == dim && mk.x == x && mk.y == y && mk.z == z) return mk;
            }
            return null;
        }

        private static wheelchair.value.AccelHub.Mark findEntity(wheelchair.value.AccelHub hub, int dim, int entityId) {
            for (int i = 0; i < hub.marks.size(); i++) {
                wheelchair.value.AccelHub.Mark mk = hub.marks.get(i);
                if (mk.isEntity && mk.dim == dim && mk.entityId == entityId) return mk;
            }
            return null;
        }

        private static int step(int cur, int[] steps, int dir) {
            int idx = 0;
            for (int i = 0; i < steps.length; i++) if (steps[i] == cur) { idx = i; break; }
            idx = (idx + dir + steps.length) % steps.length;
            return steps[idx];
        }
    }
}
