package larynx.dev.aerostar_redux.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(
    modid = "aerostar_redux"
)
public final class AeroStarCompNetwork {
    private AeroStarCompNetwork() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("aerostar_redux").versioned("1.0.0");
        registrar.playToServer(DriveSettingsPacket.TYPE, DriveSettingsPacket.CODEC, DriveSettingsPacket::handle);
        registrar.playToClient(DriveFeedbackPacket.TYPE, DriveFeedbackPacket.CODEC, DriveFeedbackPacket::handle);
    }
}
