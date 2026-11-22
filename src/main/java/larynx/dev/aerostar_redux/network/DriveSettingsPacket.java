package larynx.dev.aerostar_redux.network;

import larynx.dev.aerostar_redux.AeroStarComp;
import larynx.dev.aerostar_redux.block.entity.DimensionalDriveBlockEntity;
import larynx.dev.aerostar_redux.menu.DimensionalDriveMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DriveSettingsPacket(int action) implements CustomPacketPayload {
    public static final Type<DriveSettingsPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("aerostar_redux", "drive_settings"));
    public static final StreamCodec<FriendlyByteBuf, DriveSettingsPacket> CODEC = StreamCodec.of(
        (buf, pkt) -> buf.writeInt(pkt.action), buf -> new DriveSettingsPacket(buf.readInt())
    );

    public static final int ACTION_ARM = 1;
    public static final int ACTION_CYCLE_DESTINATION = 2;

    public static DriveSettingsPacket arm() {
        return new DriveSettingsPacket(ACTION_ARM);
    }

    public static DriveSettingsPacket cycleDestination() {
        return new DriveSettingsPacket(ACTION_CYCLE_DESTINATION);
    }

    public static void handle(DriveSettingsPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                if (!(player.containerMenu instanceof DimensionalDriveMenu menu)) {
                    AeroStarComp.LOGGER.warn("Drive settings packet received but player has no drive menu open.");
                } else {
                    DimensionalDriveBlockEntity be = menu.getBlockEntity();
                    if (be != null) {
                        switch (pkt.action) {
                            case ACTION_ARM -> be.tryArm(player);
                            case ACTION_CYCLE_DESTINATION -> be.cycleDestination();
                            default -> AeroStarComp.LOGGER.warn("Unknown drive settings action {}", pkt.action);
                        }
                    }
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
