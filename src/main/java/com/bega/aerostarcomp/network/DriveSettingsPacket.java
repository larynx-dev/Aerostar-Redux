package com.bega.aerostarcomp.network;

import com.bega.aerostarcomp.AeroStarComp;
import com.bega.aerostarcomp.block.entity.DimensionalDriveBlockEntity;
import com.bega.aerostarcomp.menu.DimensionalDriveMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DriveSettingsPacket(int action) implements CustomPacketPayload {
    public static final Type<DriveSettingsPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("aerostarcomp", "drive_settings"));
    public static final StreamCodec<FriendlyByteBuf, DriveSettingsPacket> CODEC = StreamCodec.of(
        (buf, pkt) -> buf.writeInt(pkt.action), buf -> new DriveSettingsPacket(buf.readInt())
    );

    public static DriveSettingsPacket arm() {
        return new DriveSettingsPacket(1);
    }

    public static void handle(DriveSettingsPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                if (!(player.containerMenu instanceof DimensionalDriveMenu menu)) {
                    AeroStarComp.LOGGER.warn("Arm packet received but player has no drive menu open.");
                } else {
                    DimensionalDriveBlockEntity be = menu.getBlockEntity();
                    if (be != null) {
                        be.tryArm(player);
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
