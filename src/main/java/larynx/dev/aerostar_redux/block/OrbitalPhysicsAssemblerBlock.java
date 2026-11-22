package larynx.dev.aerostar_redux.block;

import com.mojang.serialization.MapCodec;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class OrbitalPhysicsAssemblerBlock extends PhysicsAssemblerBlock {
    public static final MapCodec<OrbitalPhysicsAssemblerBlock> CODEC = simpleCodec(OrbitalPhysicsAssemblerBlock::new);

    public OrbitalPhysicsAssemblerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
