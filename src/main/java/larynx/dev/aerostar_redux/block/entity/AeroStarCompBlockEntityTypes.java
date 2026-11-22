package larynx.dev.aerostar_redux.block.entity;

import larynx.dev.aerostar_redux.block.AeroStarCompBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AeroStarCompBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "aerostar_redux");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DimensionalDriveBlockEntity>> DIMENSIONAL_DRIVE = BLOCK_ENTITY_TYPES.register(
        "dimensional_drive", () -> Builder.of(DimensionalDriveBlockEntity::new, AeroStarCompBlocks.DIMENSIONAL_DRIVE.get()).build(null)
    );
}
