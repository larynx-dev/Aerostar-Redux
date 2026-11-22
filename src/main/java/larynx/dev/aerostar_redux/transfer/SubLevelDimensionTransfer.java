package larynx.dev.aerostar_redux.transfer;

import larynx.dev.aerostar_redux.AeroStarComp;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public final class SubLevelDimensionTransfer {
    public static final double ARRIVAL_ALTITUDE = 300.0;
    private static final ConcurrentLinkedQueue<SubLevelDimensionTransfer.PendingReapply> PENDING = new ConcurrentLinkedQueue<>();
    private static final int REAPPLY_TICKS = 60;
    private static final ConcurrentLinkedQueue<SubLevelDimensionTransfer.PendingRemount> PENDING_REMOUNT = new ConcurrentLinkedQueue<>();
    private static final int REMOUNT_DELAY_TICKS = 10;
    private static final String SUPER_GLUE_ID = "create:super_glue";

    private SubLevelDimensionTransfer() {
    }

    public static void scheduleReapply(CompoundTag plotData, ServerSubLevel newSubLevel, BlockPos sourcePlotCenter, int sourceMinBuildHeight) {
        PENDING.add(new SubLevelDimensionTransfer.PendingReapply(plotData, newSubLevel, sourcePlotCenter, sourceMinBuildHeight));
    }

    public static void tick() {
        if (!PENDING.isEmpty()) {
            PENDING.removeIf(job -> {
                try {
                    reapplyBlockEntities(job.plotData, job.newSubLevel, job.sourcePlotCenter, job.sourceMinBuildHeight, false);
                } catch (Exception var2) {
                    return true;
                }

                return --job.ticksLeft <= 0;
            });
        }

        if (!PENDING_REMOUNT.isEmpty()) {
            PENDING_REMOUNT.removeIf(job -> {
                if (--job.ticksLeft > 0) {
                    return false;
                } else {
                    remountSeated(job);
                    return true;
                }
            });
        }
    }

    private static void remountSeated(SubLevelDimensionTransfer.PendingRemount job) {
        try {
            ServerPlayer player = job.newPlotLevel.getServer().getPlayerList().getPlayer(job.playerUuid);
            if (player == null) {
                AeroStarComp.LOGGER.info("[REMOUNT] player {} gone before delayed re-seat", job.playerUuid);
                return;
            }

            if (player.getVehicle() != null) {
                return;
            }

            double sx = (double)job.newPlotCenter.getX() + job.seatLocalOffset.x;
            double sy = (double)job.newPlotCenter.getY() + job.seatLocalOffset.y + job.seatYCorrection;
            double sz = (double)job.newPlotCenter.getZ() + job.seatLocalOffset.z;
            Optional<Entity> recreated = EntityType.create(job.seatNbt, job.newPlotLevel);
            if (recreated.isEmpty()) {
                AeroStarComp.LOGGER.warn("[REMOUNT] EntityType.create returned empty for captured seat of player {}", job.playerUuid);
                return;
            }

            Entity seat = recreated.get();
            seat.moveTo(sx, sy, sz, seat.getYRot(), seat.getXRot());
            job.newPlotLevel.addFreshEntity(seat);
            boolean ok = player.startRiding(seat, true);
            AeroStarComp.LOGGER
                .info("[REMOUNT] (delayed) recreated seat for player {} at plot-level ({}, {}, {}) startRiding={}", job.playerUuid, sx, sy, sz, ok);
        } catch (Exception var11) {
            AeroStarComp.LOGGER.warn("Failed delayed re-seat of player after transfer", (Throwable)var11);
        }
    }

    public static boolean transfer(ServerSubLevel subLevel, ServerLevel source, ServerLevel target) {
        try {
            return doTransfer(subLevel, source, target);
        } catch (Exception var4) {
            AeroStarComp.LOGGER.error("SubLevel dimension transfer threw an exception", (Throwable)var4);
            return false;
        }
    }

    private static boolean doTransfer(ServerSubLevel subLevel, ServerLevel source, ServerLevel target) {
        double worldX = subLevel.logicalPose().position().x();
        double worldY = subLevel.logicalPose().position().y();
        double worldZ = subLevel.logicalPose().position().z();
        Vector3d linearVelocity = new Vector3d(subLevel.latestLinearVelocity);
        Vector3d angularVelocity = new Vector3d(subLevel.latestAngularVelocity);
        BlockPos sourcePlotCenter = subLevel.getPlot().getCenterBlock();
        AeroStarComp.LOGGER
            .info("Transfer: {} -> {} | pos=({:.1f}, {:.1f}, {:.1f})", source.dimension().location(), target.dimension().location(), worldX, worldY, worldZ);
        List<SubLevelDimensionTransfer.PassengerRecord> passengers = collectPassengers(subLevel, source, worldX, worldY, worldZ);
        ServerLevelPlot sourcePlot = subLevel.getPlot();

        CompoundTag plotData;
        try {
            plotData = sourcePlot.save();
        } catch (Exception var22) {
            AeroStarComp.LOGGER.error("Failed to save plot data", (Throwable)var22);
            return false;
        }

        ServerSubLevelContainer sourceContainer = SubLevelContainer.getContainer(source);
        if (sourceContainer == null) {
            AeroStarComp.LOGGER.error("Source has no SubLevelContainer");
            return false;
        } else {
            List<CompoundTag> glueJoints = collectSuperGlue(subLevel, sourcePlotCenter);
            subLevel.getPlot().kickAllEntities();
            sourceContainer.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
            ServerSubLevelContainer targetContainer = SubLevelContainer.getContainer(target);
            if (targetContainer == null) {
                AeroStarComp.LOGGER.error("Target has no SubLevelContainer");
                return false;
            } else {
                Pose3d arrivalPose = new Pose3d();
                arrivalPose.position().set(worldX, 300.0, worldZ);
                arrivalPose.orientation().set(subLevel.logicalPose().orientation());
                ServerSubLevel newSubLevel = (ServerSubLevel)targetContainer.allocateNewSubLevel(arrivalPose);
                sanitizeStalePlotIds(plotData);

                try {
                    newSubLevel.getPlot().load(plotData);
                } catch (Exception var21) {
                    AeroStarComp.LOGGER.error("Failed to load plot data into new sub-level", (Throwable)var21);
                    targetContainer.removeSubLevel(newSubLevel, SubLevelRemovalReason.REMOVED);
                    return false;
                }

                newSubLevel.updateLastPose();
                applyScaledVelocity(newSubLevel, targetContainer, linearVelocity, angularVelocity, source.dimension(), target.dimension());
                int sourceMinBuildHeight = source.getMinBuildHeight();
                reapplyBlockEntities(plotData, newSubLevel, sourcePlotCenter, sourceMinBuildHeight, true);
                scheduleReapply(plotData, newSubLevel, sourcePlotCenter, sourceMinBuildHeight);
                recreateSuperGlue(glueJoints, newSubLevel, sourcePlotCenter, sourceMinBuildHeight);
                teleportPassengers(passengers, newSubLevel, source, target, worldX, worldZ);
                AeroStarComp.LOGGER.info("Transfer complete. New sub-level: {}", newSubLevel.getUniqueId());
                return true;
            }
        }
    }

    private static List<SubLevelDimensionTransfer.PassengerRecord> collectPassengers(
        ServerSubLevel subLevel, ServerLevel source, double cx, double cy, double cz
    ) {
        ArrayList<SubLevelDimensionTransfer.PassengerRecord> records = new ArrayList<>();
        ServerLevel plotLevel = subLevel.getLevel();
        BlockPos plotCenter = subLevel.getPlot().getCenterBlock();

        for (Entity entity : source.getAllEntities()) {
            SubLevel tracking = Sable.HELPER.getTrackingSubLevel(entity);
            if (tracking != null
                && tracking.getUniqueId().equals(subLevel.getUniqueId())
                && !"create:super_glue".equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString())) {
                Vec3 offset = new Vec3(entity.getX() - cx, entity.getY() - cy, entity.getZ() - cz);
                CompoundTag nbt = new CompoundTag();
                entity.saveWithoutId(nbt);
                UUID trackedVehicleUuid = null;
                CompoundTag seatNbt = null;
                Vec3 seatLocalOffset = null;
                if (entity instanceof ServerPlayer && entity.getVehicle() != null) {
                    Entity vehicle = entity.getVehicle();
                    trackedVehicleUuid = vehicle.getUUID();
                    SubLevelDimensionTransfer.SeatCapture cap = captureSeat(vehicle, plotLevel, plotCenter);
                    if (cap != null) {
                        seatNbt = cap.nbt();
                        seatLocalOffset = cap.offset();
                    }
                }

                records.add(
                    new SubLevelDimensionTransfer.PassengerRecord(
                        entity.getUUID(), entity, nbt, offset, entity instanceof ServerPlayer, trackedVehicleUuid, seatNbt, seatLocalOffset
                    )
                );
            }
        }

        for (Entity entityx : source.getAllEntities()) {
            if (entityx instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)entityx;
                boolean alreadyCollected = records.stream().anyMatch(rx -> rx.uuid().equals(player.getUUID()));
                Entity vehicle;
                if (!alreadyCollected && (vehicle = player.getVehicle()) != null && vehicle.level() == plotLevel) {
                    Vec3 worldVehiclePos = subLevel.logicalPose().transformPosition(vehicle.position());
                    Vec3 offset = new Vec3(worldVehiclePos.x - cx, worldVehiclePos.y - cy, worldVehiclePos.z - cz);
                    CompoundTag nbt = new CompoundTag();
                    player.saveWithoutId(nbt);
                    SubLevelDimensionTransfer.SeatCapture cap = captureSeat(vehicle, plotLevel, plotCenter);
                    CompoundTag seatNbt = cap != null ? cap.nbt() : null;
                    Vec3 seatLocalOffset = cap != null ? cap.offset() : null;
                    records.add(
                        new SubLevelDimensionTransfer.PassengerRecord(player.getUUID(), player, nbt, offset, true, vehicle.getUUID(), seatNbt, seatLocalOffset)
                    );
                }
            }
        }

        AeroStarComp.LOGGER.info("Collected {} passengers.", records.size());

        for (SubLevelDimensionTransfer.PassengerRecord r : records) {
            AeroStarComp.LOGGER
                .info("[PASSENGER] uuid={} isPlayer={} vehicleUuid={} seatCaptured={}", r.uuid(), r.isPlayer(), r.vehicleUuid(), r.seatNbt() != null);
        }

        return records;
    }

    private static SubLevelDimensionTransfer.SeatCapture captureSeat(Entity seat, ServerLevel plotLevel, BlockPos plotCenter) {
        if (seat.level() != plotLevel) {
            AeroStarComp.LOGGER.info("[SEAT] vehicle {} is not in the plot level; cannot capture for re-seating", seat.getUUID());
            return null;
        } else {
            CompoundTag tag = new CompoundTag();
            boolean saved = seat.save(tag);
            if (saved && tag.contains("id")) {
                Vec3 offset = new Vec3(
                    seat.getX() - (double)plotCenter.getX(), seat.getY() - (double)plotCenter.getY(), seat.getZ() - (double)plotCenter.getZ()
                );
                AeroStarComp.LOGGER.info("[SEAT] captured seat {} ({}) offsetFromCenter={}", seat.getUUID(), seat.getType(), offset);
                return new SubLevelDimensionTransfer.SeatCapture(tag, offset);
            } else {
                AeroStarComp.LOGGER
                    .warn(
                        "[SEAT] vehicle {} ({}) is not serializable (saved={}, hasId={}); cannot recreate it",
                        seat.getUUID(),
                        seat.getType(),
                        saved,
                        tag.contains("id")
                    );
                return null;
            }
        }
    }

    private static void teleportPassengers(
        List<SubLevelDimensionTransfer.PassengerRecord> passengers,
        ServerSubLevel newSubLevel,
        ServerLevel source,
        ServerLevel target,
        double arrivalX,
        double arrivalZ
    ) {
        ServerLevel newPlotLevel = newSubLevel.getLevel();
        BlockPos newPlotCenter = newSubLevel.getPlot().getCenterBlock();
        int sourceCenterY = (source.getMinBuildHeight() + source.getMaxBuildHeight()) / 2;
        int centerYDelta = newPlotCenter.getY() - sourceCenterY;
        int yShift = newPlotLevel.getMinBuildHeight() - source.getMinBuildHeight();
        double seatYCorrection = (double)(yShift - centerYDelta);

        for (SubLevelDimensionTransfer.PassengerRecord record : passengers) {
            if (!record.isPlayer()) {
                double destX = arrivalX + record.offset().x;
                double destY = 300.0 + record.offset().y;
                double destZ = arrivalZ + record.offset().z;
                Entity sourceEntity = source.getEntity(record.uuid());
                if (sourceEntity != null) {
                    sourceEntity.remove(RemovalReason.CHANGED_DIMENSION);
                    Entity newEntity = sourceEntity.getType().create(target);
                    if (newEntity != null) {
                        newEntity.load(record.nbt());
                        newEntity.moveTo(destX, destY, destZ, newEntity.getYRot(), newEntity.getXRot());
                        target.addFreshEntity(newEntity);
                    }
                }
            }
        }

        for (SubLevelDimensionTransfer.PassengerRecord recordx : passengers) {
            if (recordx.isPlayer()) {
                double destX = arrivalX + recordx.offset().x;
                double destY = 300.0 + recordx.offset().y;
                double destZ = arrivalZ + recordx.offset().z;
                ServerPlayer player = source.getServer().getPlayerList().getPlayer(recordx.uuid());
                if (player != null) {
                    player.stopRiding();
                    player.teleportTo(target, destX, destY, destZ, player.getYRot(), player.getXRot());
                    if (recordx.seatNbt() != null && recordx.seatLocalOffset() != null) {
                        PENDING_REMOUNT.add(
                            new SubLevelDimensionTransfer.PendingRemount(
                                newPlotLevel, newPlotCenter, recordx.uuid(), recordx.seatNbt(), recordx.seatLocalOffset(), seatYCorrection
                            )
                        );
                        AeroStarComp.LOGGER.info("[REMOUNT] scheduled delayed re-seat for player {} ({} ticks)", recordx.uuid(), 10);
                    } else {
                        AeroStarComp.LOGGER.info("[REMOUNT] no seat captured for player {}; staying unseated", recordx.uuid());
                    }
                }
            }
        }
    }

    private static void applyScaledVelocity(
        ServerSubLevel newSubLevel,
        ServerSubLevelContainer targetContainer,
        Vector3d linearVelocity,
        Vector3d angularVelocity,
        ResourceKey<Level> sourceDim,
        ResourceKey<Level> targetDim
    ) {
        SubLevelPhysicsSystem physicsSystem = targetContainer.physicsSystem();
        PhysicsPipeline pipeline = physicsSystem.getPipeline();
        pipeline.teleport(newSubLevel, newSubLevel.logicalPose().position(), newSubLevel.logicalPose().orientation());
        Vector3d beforeLin = pipeline.getLinearVelocity(newSubLevel, new Vector3d());
        Vector3d beforeAng = pipeline.getAngularVelocity(newSubLevel, new Vector3d());
        AeroStarComp.LOGGER
            .info("[ARRIVAL] captured(old) lin={} ang={} | body-before-reset lin={} ang={}", linearVelocity, angularVelocity, beforeLin, beforeAng);
        pipeline.resetVelocity(newSubLevel);
        Vector3d afterLin = pipeline.getLinearVelocity(newSubLevel, new Vector3d());
        Vector3d afterAng = pipeline.getAngularVelocity(newSubLevel, new Vector3d());
        AeroStarComp.LOGGER.info("[ARRIVAL] body-after-reset lin={} ang={}", afterLin, afterAng);
        pipeline.addLinearAndAngularVelocity(newSubLevel, new Vector3d(0.0, -2.0, 0.0), new Vector3d(0.0, 0.0, 0.0));
    }

    private static void reapplyBlockEntities(
        CompoundTag plotData, ServerSubLevel newSubLevel, BlockPos sourcePlotCenter, int sourceMinBuildHeight, boolean verbose
    ) {
        try {
            ServerLevel newPlotLevel = newSubLevel.getLevel();
            BlockPos newPlotCenter = newSubLevel.getPlot().getCenterBlock();
            Provider registries = newPlotLevel.registryAccess();
            int xShift = newPlotCenter.getX() - sourcePlotCenter.getX();
            int zShift = newPlotCenter.getZ() - sourcePlotCenter.getZ();
            int yShift = newPlotLevel.getMinBuildHeight() - sourceMinBuildHeight;
            CompoundTag chunks = plotData.getCompound("chunks");
            int reapplied = 0;
            int missing = 0;
            int diagLogged = 0;
            if (verbose) {
                AeroStarComp.LOGGER
                    .info(
                        "[BE-DIAG] sourceCenter={} newCenter={} shift=({}, {}, {}) [Y from minBuild {} -> {}]",
                        sourcePlotCenter,
                        newPlotCenter,
                        xShift,
                        yShift,
                        zShift,
                        sourceMinBuildHeight,
                        newPlotLevel.getMinBuildHeight()
                    );
            }

            for (String key : chunks.getAllKeys()) {
                CompoundTag chunkTag = chunks.getCompound(key);
                ListTag bes = chunkTag.getList("block_entities", 10);

                for (int i = 0; i < bes.size(); i++) {
                    CompoundTag beTag = bes.getCompound(i);
                    if (!beTag.getBoolean("keepPacked") && beTag.contains("x") && beTag.contains("y") && beTag.contains("z")) {
                        int tx = beTag.getInt("x") + xShift;
                        int ty = beTag.getInt("y") + yShift;
                        int tz = beTag.getInt("z") + zShift;
                        BlockPos targetPos = new BlockPos(tx, ty, tz);
                        CompoundTag fixed = beTag.copy();
                        fixed.putInt("x", tx);
                        fixed.putInt("y", ty);
                        fixed.putInt("z", tz);
                        BlockEntity be = newPlotLevel.getBlockEntity(targetPos);
                        if (be == null) {
                            BlockState st = newPlotLevel.getBlockState(targetPos);
                            if (st.isAir()) {
                                missing++;
                                if (verbose && diagLogged < 5) {
                                    diagLogged++;
                                    BlockPos savedPos = new BlockPos(beTag.getInt("x"), beTag.getInt("y"), beTag.getInt("z"));
                                    AeroStarComp.LOGGER
                                        .info(
                                            "[BE-DIAG] miss {} | savedPos={} state@saved={} | targetPos={} state@target={}",
                                            beTag.getString("id"),
                                            savedPos,
                                            newPlotLevel.getBlockState(savedPos),
                                            targetPos,
                                            st
                                        );
                                }
                            } else {
                                try {
                                    BlockEntity created = BlockEntity.loadStatic(targetPos, st, fixed, registries);
                                    if (created == null) {
                                        missing++;
                                    } else {
                                        newPlotLevel.setBlockEntity(created);
                                        created.setChanged();
                                        reapplied++;
                                    }
                                } catch (Exception var30) {
                                    missing++;
                                    AeroStarComp.LOGGER.warn("[BE-REAPPLY] recreate failed for {} at {}", beTag.getString("id"), targetPos, var30);
                                }
                            }
                        } else {
                            try {
                                be.loadWithComponents(fixed, registries);
                                be.setChanged();
                                reapplied++;
                            } catch (Exception var29) {
                                AeroStarComp.LOGGER.warn("[BE-REAPPLY] failed for {} at {}", beTag.getString("id"), targetPos, var29);
                            }
                        }
                    }
                }
            }

            AeroStarComp.LOGGER.info("[BE-REAPPLY] reapplied={} missing={}", reapplied, missing);
        } catch (Exception var31) {
            AeroStarComp.LOGGER.error("Failed to re-apply block entity NBT after transfer", (Throwable)var31);
        }
    }

    private static List<CompoundTag> collectSuperGlue(ServerSubLevel subLevel, BlockPos center) {
        ArrayList<CompoundTag> out = new ArrayList<>();

        try {
            ServerLevel plotLevel = subLevel.getLevel();
            AABB area = new AABB(
                (double)center.getX() - 1024.0,
                (double)plotLevel.getMinBuildHeight(),
                (double)center.getZ() - 1024.0,
                (double)center.getX() + 1024.0,
                (double)plotLevel.getMaxBuildHeight(),
                (double)center.getZ() + 1024.0
            );

            for (Entity e : plotLevel.getEntitiesOfClass(Entity.class, area)) {
                if ("create:super_glue".equals(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString())) {
                    CompoundTag tag = new CompoundTag();
                    if (e.save(tag)) {
                        out.add(tag);
                    }
                }
            }

            if (!out.isEmpty()) {
                AeroStarComp.LOGGER.info("[GLUE] captured {} super glue joint(s)", out.size());
            }
        } catch (Exception var8) {
            AeroStarComp.LOGGER.warn("Failed to capture super glue before transfer", (Throwable)var8);
        }

        return out;
    }

    private static void recreateSuperGlue(List<CompoundTag> glue, ServerSubLevel newSubLevel, BlockPos sourceCenter, int sourceMinBuildHeight) {
        if (!glue.isEmpty()) {
            try {
                ServerLevel newPlotLevel = newSubLevel.getLevel();
                BlockPos newCenter = newSubLevel.getPlot().getCenterBlock();
                int xShift = newCenter.getX() - sourceCenter.getX();
                int zShift = newCenter.getZ() - sourceCenter.getZ();
                int yShift = newPlotLevel.getMinBuildHeight() - sourceMinBuildHeight;
                int restored = 0;

                for (CompoundTag tag : glue) {
                    ListTag pos = tag.getList("Pos", 6);
                    if (pos.size() == 3) {
                        double nx = pos.getDouble(0) + (double)xShift;
                        double ny = pos.getDouble(1) + (double)yShift;
                        double nz = pos.getDouble(2) + (double)zShift;
                        Optional<Entity> created = EntityType.create(tag, newPlotLevel);
                        if (!created.isEmpty()) {
                            Entity glueEntity = created.get();
                            glueEntity.setPos(nx, ny, nz);
                            newPlotLevel.addFreshEntity(glueEntity);
                            restored++;
                        }
                    }
                }

                AeroStarComp.LOGGER.info("[GLUE] restored {}/{} super glue joint(s)", restored, glue.size());
            } catch (Exception var21) {
                AeroStarComp.LOGGER.warn("Failed to restore super glue after transfer", (Throwable)var21);
            }
        }
    }

    private static String remapStaleId(String id) {
        return "cnbridge:dimensional_drive".equals(id) || "aerostarcomp:dimensional_drive".equals(id) ? "aerostar_redux:dimensional_drive" : null;
    }

    private static void sanitizeStalePlotIds(CompoundTag plotData) {
        try {
            int blocks = 0;
            int bes = 0;
            CompoundTag chunks = plotData.getCompound("chunks");

            label52:
            for (String key : chunks.getAllKeys()) {
                CompoundTag chunkTag = chunks.getCompound(key);
                CompoundTag sections = chunkTag.getCompound("sections");
                Iterator beList = sections.getAllKeys().iterator();

                label50:
                while (true) {
                    if (beList.hasNext()) {
                        String sk = (String)beList.next();
                        ListTag palette = sections.getCompound(sk).getCompound("block_states").getList("palette", 10);
                        int i = 0;

                        while (true) {
                            if (i >= palette.size()) {
                                continue label50;
                            }

                            CompoundTag entry = palette.getCompound(i);
                            String remap = remapStaleId(entry.getString("Name"));
                            if (remap != null) {
                                entry.putString("Name", remap);
                                blocks++;
                            }

                            i++;
                        }
                    }

                    ListTag beListx = chunkTag.getList("block_entities", 10);
                    int i = 0;

                    while (true) {
                        if (i >= beListx.size()) {
                            continue label52;
                        }

                        CompoundTag beTag = beListx.getCompound(i);
                        String remap = remapStaleId(beTag.getString("id"));
                        if (remap != null) {
                            beTag.putString("id", remap);
                            bes++;
                        }

                        i++;
                    }
                }
            }

            if (blocks > 0 || bes > 0) {
                AeroStarComp.LOGGER.info("[SANITIZE] remapped stale ids: blocks={} blockEntities={}", blocks, bes);
            }
        } catch (Exception var14) {
            AeroStarComp.LOGGER.warn("Failed to sanitize stale plot ids", (Throwable)var14);
        }
    }

    private static record PassengerRecord(
        UUID uuid, Entity entity, CompoundTag nbt, Vec3 offset, boolean isPlayer, UUID vehicleUuid, CompoundTag seatNbt, Vec3 seatLocalOffset
    ) {
    }

    private static final class PendingReapply {
        final CompoundTag plotData;
        final ServerSubLevel newSubLevel;
        final BlockPos sourcePlotCenter;
        final int sourceMinBuildHeight;
        int ticksLeft;

        PendingReapply(CompoundTag plotData, ServerSubLevel newSubLevel, BlockPos sourcePlotCenter, int sourceMinBuildHeight) {
            this.plotData = plotData;
            this.newSubLevel = newSubLevel;
            this.sourcePlotCenter = sourcePlotCenter;
            this.sourceMinBuildHeight = sourceMinBuildHeight;
            this.ticksLeft = 60;
        }
    }

    private static final class PendingRemount {
        final ServerLevel newPlotLevel;
        final BlockPos newPlotCenter;
        final UUID playerUuid;
        final CompoundTag seatNbt;
        final Vec3 seatLocalOffset;
        final double seatYCorrection;
        int ticksLeft;

        PendingRemount(ServerLevel newPlotLevel, BlockPos newPlotCenter, UUID playerUuid, CompoundTag seatNbt, Vec3 seatLocalOffset, double seatYCorrection) {
            this.newPlotLevel = newPlotLevel;
            this.newPlotCenter = newPlotCenter;
            this.playerUuid = playerUuid;
            this.seatNbt = seatNbt;
            this.seatLocalOffset = seatLocalOffset;
            this.seatYCorrection = seatYCorrection;
            this.ticksLeft = 10;
        }
    }

    private static record SeatCapture(CompoundTag nbt, Vec3 offset) {
    }
}
