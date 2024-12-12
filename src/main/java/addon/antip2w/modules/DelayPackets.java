package addon.antip2w.modules;
//todo: actually make this decent
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.network.packet.Packet;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class DelayPackets extends Module {
    private final Queue<Packet<?>> delayedPackets = new LinkedList<>();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = sgGeneral.add(new PacketListSetting.Builder()
        .name("C2S-packets")
        .description("Client-to-server packets to delay.")
        .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
        .build()
    );


    public DelayPackets() {
        super(Categories.DEFAULT, "delay-packets", "Delays C2S packets when turned on.");
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onSendPacket(PacketEvent.Send event) {
        if (c2sPackets.get().contains(event.packet.getClass())) {
            synchronized (delayedPackets) {
                delayedPackets.add(event.packet);
            }
            event.cancel();
        }
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        toggle();
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }

    @Override
    public void onDeactivate() {
        while (!delayedPackets.isEmpty()) {
            Packet<?> packet = delayedPackets.poll();
            mc.getNetworkHandler().sendPacket(packet);
        }
    }
}
