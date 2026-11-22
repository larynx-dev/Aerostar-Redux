package larynx.dev.aerostar_redux.physics;

import larynx.dev.aerostar_redux.transfer.SubLevelDimensionTransfer;
import com.lightning.northstar.content.world.NorthstarDimensions;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class OrbitGravitySystem {
    private static final double VOID_RETURN_Y = 20.0;
    private static final double RETURN_SKY_Y = 300.0;

    private OrbitGravitySystem() {
    }

    public static void onPrePhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        SubLevelPhysicsSystem physicsSystem = event.getPhysicsSystem();
        ServerLevel level = physicsSystem.getLevel();
        if (level.dimension().equals(NorthstarDimensions.EARTH_ORBIT)) {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container != null) {
                double timeStep = event.getTimeStep();
                Vector3d gravity = DimensionPhysicsData.getGravity(level);

                for (ServerSubLevel subLevel : container.getAllSubLevels()) {
                    if (!subLevel.isRemoved()) {
                        MassData massData = subLevel.getMassTracker();
                        if (!massData.isInvalid()) {
                            Vector3dc centerOfMass = massData.getCenterOfMass();
                            if (centerOfMass != null) {
                                double mass = massData.getMass();
                                Vector3d antiGravity = new Vector3d(-gravity.x * mass * timeStep, -gravity.y * mass * timeStep, -gravity.z * mass * timeStep);
                                QueuedForceGroup levitation = subLevel.getOrCreateQueuedForceGroup(ForceGroups.LEVITATION.get());
                                levitation.applyAndRecordPointForce(centerOfMass, antiGravity);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void onServerTick(Post event) {
        ServerLevel orbitLevel = event.getServer().getLevel(NorthstarDimensions.EARTH_ORBIT);
        ServerLevel overworld = event.getServer().overworld();
        if (orbitLevel != null) {
            ServerSubLevelContainer container = SubLevelContainer.getContainer(orbitLevel);
            if (container != null) {
                for (ServerSubLevel subLevel : container.getAllSubLevels()) {
                    if (!subLevel.isRemoved() && subLevel.logicalPose().position().y() < 20.0) {
                        SubLevelDimensionTransfer.transfer(subLevel, orbitLevel, overworld);
                    }
                }
            }
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            SubLevel tracking = Sable.HELPER.getTrackingSubLevel(player);
            if (player.isNoGravity()) {
                player.setNoGravity(false);
            }

            if (tracking == null && player.level().dimension().equals(NorthstarDimensions.EARTH_ORBIT) && player.getY() < 20.0) {
                BlockPos spawn = overworld.getSharedSpawnPos();
                player.teleportTo(overworld, (double)spawn.getX() + 0.5, 300.0, (double)spawn.getZ() + 0.5, player.getYRot(), player.getXRot());
            }
        }
    }
}
