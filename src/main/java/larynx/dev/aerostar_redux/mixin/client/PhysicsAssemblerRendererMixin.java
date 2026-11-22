package larynx.dev.aerostar_redux.mixin.client;

import larynx.dev.aerostar_redux.block.AeroStarCompBlocks;
import larynx.dev.aerostar_redux.client.AeroStarCompPartials;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerRenderer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({PhysicsAssemblerRenderer.class})
public abstract class PhysicsAssemblerRendererMixin {
    @ModifyExpressionValue(
        method = {"renderSafe"},
        at = {@At(
            value = "FIELD",
            target = "Ldev/simulated_team/simulated/index/SimPartialModels;ASSEMBLER_LEVER:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"
        )},
        require = 0
    )
    private PartialModel aerostar_redux$swapOrbitalLever(PartialModel original, @Local(argsOnly = true) PhysicsAssemblerBlockEntity be) {
        Level level = be.getLevel();
        return level != null && level.getBlockState(be.getBlockPos()).is(AeroStarCompBlocks.ORBITAL_PHYSICS_ASSEMBLER.get())
            ? AeroStarCompPartials.ORBITAL_ASSEMBLER_LEVER
            : original;
    }
}
