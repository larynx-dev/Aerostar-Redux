package com.bega.aerostarcomp.mixin;

import com.bega.aerostarcomp.block.AeroStarCompBlocks;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({PhysicsAssemblerBlockEntity.class})
public abstract class PhysicsAssemblerBlockEntityMixin {
    @ModifyExpressionValue(
        method = {"throwDisassemblyExceptions"},
        at = {@At(
            value = "INVOKE",
            target = "Ljava/lang/Boolean;booleanValue()Z"
        )}
    )
    private boolean aerostarcomp$allowOrbitalMidAirDisassembly(boolean disallowMidAir) {
        if (!disallowMidAir) {
            return false;
        } else {
            BlockEntity self = (BlockEntity) (Object) this;
            Level level = self.getLevel();
            return level != null && level.getBlockState(self.getBlockPos()).is(AeroStarCompBlocks.ORBITAL_PHYSICS_ASSEMBLER.get()) ? false : disallowMidAir;
        }
    }
}
