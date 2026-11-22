package larynx.dev.aerostar_redux.network;

import larynx.dev.aerostar_redux.client.DimensionalDriveScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DriveFeedbackPacket(Component message) implements CustomPacketPayload {
    public static final Type<DriveFeedbackPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("aerostar_redux", "drive_feedback"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DriveFeedbackPacket> CODEC = StreamCodec.composite(
        ComponentSerialization.STREAM_CODEC, DriveFeedbackPacket::message, DriveFeedbackPacket::new
    );

    public static void handle(DriveFeedbackPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> DimensionalDriveScreen.setFeedback(pkt.message()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
