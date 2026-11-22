package com.bega.aerostarcomp.block.entity;

import com.bega.aerostarcomp.AeroStarComp;
import com.bega.aerostarcomp.command.DriveTransferCommand;
import com.bega.aerostarcomp.menu.DimensionalDriveMenu;
import com.bega.aerostarcomp.network.DriveFeedbackPacket;
import com.bega.aerostarcomp.transfer.SubLevelDimensionTransfer;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3dc;

public class DimensionalDriveBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    public static final int TRIGGER_ALTITUDE = 1750;
    private static final ResourceLocation NAVIGATOR_ID = ResourceLocation.fromNamespaceAndPath("northstar", "interplanetary_navigator");
    private final ItemStackHandler starMap = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            DimensionalDriveBlockEntity.this.setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return DimensionalDriveBlockEntity.isStarMap(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private boolean armed = false;
    private int cooldownTicks = 0;
    private boolean transferred = false;
    private double teleSpeed = 0.0;
    private double teleAltitude = 0.0;
    private boolean teleHasShip = false;
    private static final ResourceLocation PLANET_COMPONENT_ID = ResourceLocation.fromNamespaceAndPath("northstar", "planet");

    public DimensionalDriveBlockEntity(BlockPos pos, BlockState state) {
        super(AeroStarCompBlockEntityTypes.DIMENSIONAL_DRIVE.get(), pos, state);
    }

    public ItemStackHandler getStarMapHandler() {
        return this.starMap;
    }

    public boolean isArmed() {
        return this.armed;
    }

    public static boolean isStarMap(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return id != null && id.getNamespace().equals("northstar") && id.getPath().startsWith("star_map");
        } else {
            return false;
        }
    }

    public static String planetFromStarMap(ItemStack stack) {
        if (!isStarMap(stack)) {
            return null;
        } else {
            DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(PLANET_COMPONENT_ID);
            if (type == null) {
                return null;
            } else {
                Object value = stack.get(type);
                if (!(value instanceof String)) {
                    return null;
                } else {
                    String p = ((String)value).toLowerCase(Locale.ROOT);
                    if (p.contains("moon")) {
                        return "moon";
                    } else if (p.contains("mars")) {
                        return "mars";
                    } else if (p.contains("venus")) {
                        return "venus";
                    } else if (p.contains("mercury")) {
                        return "mercury";
                    } else {
                        return p.contains("earth") ? "earth" : null;
                    }
                }
            }
        }
    }

    public String resolveDestination() {
        ItemStack map = this.starMap.getStackInSlot(0);
        return !map.isEmpty() && isStarMap(map) ? planetFromStarMap(map) : "earth_orbit";
    }

    public static Component planetName(String planet) {
        return Component.translatable("gui.aerostarcomp.planet." + planet);
    }

    private static boolean navigatorRequired(String planet) {
        return !planet.equals("moon") && !planet.equals("earth") && !planet.equals("earth_orbit");
    }

    public static int requiredAltitude(String planet, ResourceKey<Level> sourceDim) {
        if (sourceDim != null && sourceDim.equals(Level.OVERWORLD)) {
            return switch (planet) {
                case "earth_orbit" -> 1750;
                case "moon" -> 1750;
                case "mars" -> 2200;
                case "venus" -> 2600;
                case "mercury" -> 3000;
                default -> 1750;
            };
        } else {
            return 1750;
        }
    }

    public void openMenu(ServerPlayer player) {
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("block.aerostarcomp.dimensional_drive");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new DimensionalDriveMenu(id, inv, DimensionalDriveBlockEntity.this);
            }
        }, buf -> buf.writeBlockPos(this.getBlockPos()));
    }

    private static void feedback(ServerPlayer player, Component message) {
        PacketDistributor.sendToPlayer(player, new DriveFeedbackPacket(message));
    }

    private static void err(ServerPlayer player, String key) {
        feedback(player, Component.translatable(key).withStyle(ChatFormatting.RED));
    }

    public void tryArm(ServerPlayer player) {
        String planet = this.resolveDestination();
        if (planet == null) {
            err(player, "message.aerostarcomp.unsupported");
        } else {
            ResourceKey<Level> targetDimKey = DriveTransferCommand.resolveDimension(planet);
            if (targetDimKey == null) {
                err(player, "message.aerostarcomp.unknown");
            } else {
                ServerSubLevel subLevel = findSubLevel(this.level, this.worldPosition);
                if (subLevel == null) {
                    err(player, "message.aerostarcomp.not_on_ship");
                } else if (this.level.dimension().equals(targetDimKey)) {
                    feedback(player, Component.translatable("message.aerostarcomp.already_there").withStyle(ChatFormatting.YELLOW));
                } else if (navigatorRequired(planet) && !shipHasNavigator(subLevel)) {
                    err(player, "message.aerostarcomp.need_navigator");
                } else {
                    this.armed = true;
                    this.setChanged();
                    int alt = requiredAltitude(planet, this.level.dimension());
                    feedback(player, Component.translatable("message.aerostarcomp.armed", planetName(planet), alt).withStyle(ChatFormatting.GREEN));
                    AeroStarComp.LOGGER.info("Dimensional Drive armed -> {}", planet);
                }
            }
        }
    }

    private static boolean shipHasNavigator(ServerSubLevel subLevel) {
        Block navBlock = BuiltInRegistries.BLOCK.get(NAVIGATOR_ID);
        if (navBlock != null && navBlock != Blocks.AIR) {
            try {
                for (PlotChunkHolder holder : subLevel.getPlot().getLoadedChunks()) {
                    LevelChunk chunk = holder.getChunk();
                    if (chunk != null) {
                        LevelChunkSection[] sections = chunk.getSections();

                        for (LevelChunkSection section : sections) {
                            if (section != null && !section.hasOnlyAir()) {
                                for (int x = 0; x < 16; x++) {
                                    for (int y = 0; y < 16; y++) {
                                        for (int z = 0; z < 16; z++) {
                                            if (section.getBlockState(x, y, z).is(navBlock)) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                return false;
            } catch (Exception var13) {
                AeroStarComp.LOGGER.warn("Navigator scan failed; allowing launch", (Throwable)var13);
                return true;
            }
        } else {
            return true;
        }
    }

    private static ServerSubLevel findSubLevel(Level level, BlockPos pos) {
        if (level != null && level.getServer() != null) {
            for (ServerLevel sl : level.getServer().getAllLevels()) {
                ServerSubLevelContainer container = SubLevelContainer.getContainer(sl);
                if (container != null) {
                    for (SubLevel candidate : container.getAllSubLevels()) {
                        if (candidate instanceof ServerSubLevel ssl) {
                            if (ssl.getLevel() == level) {
                                return ssl;
                            }

                            if (level instanceof ServerLevel) {
                                BoundingBox3dc bb = ssl.boundingBox();
                                double bx = (double)pos.getX();
                                double by = (double)pos.getY();
                                double bz = (double)pos.getZ();
                                if (bx >= bb.minX() && bx <= bb.maxX() && by >= bb.minY() && by <= bb.maxY() && bz >= bb.minZ() && bz <= bb.maxZ()) {
                                    return ssl;
                                }
                            }
                        }
                    }
                }
            }

            return null;
        } else {
            return null;
        }
    }

    private void updateTelemetry(Level level, BlockPos pos, BlockState state) {
        ServerSubLevel subLevel = findSubLevel(level, pos);
        if (subLevel == null) {
            this.teleHasShip = false;
            this.teleSpeed = 0.0;
            this.teleAltitude = 0.0;
        } else {
            Vector3dc v = subLevel.latestLinearVelocity;
            this.teleSpeed = Math.sqrt(v.x() * v.x() + v.y() * v.y() + v.z() * v.z());
            this.teleAltitude = subLevel.logicalPose().position().y();
            this.teleHasShip = true;
        }

        level.sendBlockUpdated(pos, state, state, 3);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DimensionalDriveBlockEntity be) {
        if (level instanceof ServerLevel && level.getGameTime() % 5L == 0L) {
            be.updateTelemetry(level, pos, state);
        }

        if (be.transferred) {
            be.transferred = false;
        } else if (be.cooldownTicks > 0) {
            be.cooldownTicks--;
        } else if (be.armed) {
            String planet = be.resolveDestination();
            if (planet == null) {
                be.armed = false;
                be.setChanged();
            } else {
                ServerSubLevel subLevel = findSubLevel(level, pos);
                if (subLevel != null) {
                    ServerLevel parentLevel = subLevel.getLevel().getServer().getLevel(level.dimension());
                    if (parentLevel == null && level instanceof ServerLevel) {
                        parentLevel = (ServerLevel)level;
                    }

                    if (parentLevel != null) {
                        double shipY = subLevel.logicalPose().position().y();
                        if (!(shipY < (double)requiredAltitude(planet, parentLevel.dimension()))) {
                            ResourceKey<Level> targetDimKey = DriveTransferCommand.resolveDimension(planet);
                            if (targetDimKey != null) {
                                if (parentLevel.dimension().equals(targetDimKey)) {
                                    be.armed = false;
                                    be.setChanged();
                                } else {
                                    ServerLevel targetLevel = parentLevel.getServer().getLevel(targetDimKey);
                                    if (targetLevel == null) {
                                        AeroStarComp.LOGGER.error("Target dimension {} not loaded.", targetDimKey.location());
                                        be.cooldownTicks = 100;
                                    } else {
                                        AeroStarComp.LOGGER
                                            .info("Dimensional Drive firing: {} -> {}", parentLevel.dimension().location(), targetDimKey.location());
                                        boolean success = SubLevelDimensionTransfer.transfer(subLevel, parentLevel, targetLevel);
                                        if (success) {
                                            be.transferred = true;
                                            be.armed = false;
                                            be.setChanged();
                                        } else {
                                            AeroStarComp.LOGGER.error("Transfer failed. Retrying in 5 seconds.");
                                            be.cooldownTicks = 100;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("armed", this.armed);
        tag.put("starMap", this.starMap.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, Provider registries) {
        super.loadAdditional(tag, registries);
        this.armed = tag.getBoolean("armed");
        if (tag.contains("starMap")) {
            this.starMap.deserializeNBT(registries, tag.getCompound("starMap"));
        }

        if (tag.contains("teleSpeed")) {
            this.teleSpeed = tag.getDouble("teleSpeed");
        }

        if (tag.contains("teleAltitude")) {
            this.teleAltitude = tag.getDouble("teleAltitude");
        }

        if (tag.contains("teleHasShip")) {
            this.teleHasShip = tag.getBoolean("teleHasShip");
        }
    }

    @Override
    public CompoundTag getUpdateTag(Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, registries);
        tag.putDouble("teleSpeed", this.teleSpeed);
        tag.putDouble("teleAltitude", this.teleAltitude);
        tag.putBoolean("teleHasShip", this.teleHasShip);
        return tag;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static ChatFormatting planetColor(String planet) {
        if (planet == null) {
            return ChatFormatting.GRAY;
        } else {
            return switch (planet) {
                case "earth_orbit" -> ChatFormatting.AQUA;
                case "moon" -> ChatFormatting.WHITE;
                case "mars" -> ChatFormatting.RED;
                case "venus" -> ChatFormatting.GOLD;
                case "mercury" -> ChatFormatting.YELLOW;
                case "earth" -> ChatFormatting.GREEN;
                default -> ChatFormatting.LIGHT_PURPLE;
            };
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String indent = "    ";
        tooltip.add(Component.literal(indent).append(Component.translatable("block.aerostarcomp.dimensional_drive").withStyle(ChatFormatting.WHITE)));
        String planet = this.resolveDestination();
        Component destValue = planet != null
            ? planetName(planet).copy().withStyle(planetColor(planet))
            : Component.translatable("gui.aerostarcomp.no_destination").withStyle(ChatFormatting.GRAY);
        tooltip.add(
            Component.literal(indent)
                .append(Component.translatable("gui.aerostarcomp.goggle.destination").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(destValue)
        );
        if (this.teleHasShip) {
            tooltip.add(
                Component.literal(indent)
                    .append(Component.translatable("gui.aerostarcomp.goggle.speed").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format(Locale.ROOT, "%.2f m/s", this.teleSpeed)).withStyle(ChatFormatting.AQUA))
            );
            tooltip.add(
                Component.literal(indent)
                    .append(Component.translatable("gui.aerostarcomp.goggle.altitude").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.format(Locale.ROOT, "%.1f", this.teleAltitude)).withStyle(ChatFormatting.AQUA))
            );
        } else {
            tooltip.add(Component.literal(indent).append(Component.translatable("gui.aerostarcomp.goggle.no_ship").withStyle(ChatFormatting.DARK_GRAY)));
        }

        if (planet != null && this.level != null) {
            int reqAlt = requiredAltitude(planet, this.level.dimension());
            Component reqValue = Component.literal(Integer.toString(reqAlt))
                .withStyle(this.teleHasShip && this.teleAltitude >= (double)reqAlt ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
            tooltip.add(
                Component.literal(indent)
                    .append(Component.translatable("gui.aerostarcomp.goggle.required_altitude").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(reqValue)
            );
        }

        return true;
    }
}
