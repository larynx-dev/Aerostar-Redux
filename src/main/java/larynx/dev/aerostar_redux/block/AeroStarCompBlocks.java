package larynx.dev.aerostar_redux.block;

import larynx.dev.aerostar_redux.item.AeroStarCompItems;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;

public class AeroStarCompBlocks {
    public static final Blocks BLOCKS = DeferredRegister.createBlocks("aerostar_redux");
    public static final DeferredBlock<DimensionalDriveBlock> DIMENSIONAL_DRIVE = registerWithItem(
        "dimensional_drive",
        () -> new DimensionalDriveBlock(Properties.of().mapColor(MapColor.METAL).strength(3.0F, 4.0F).sound(SoundType.METAL).noOcclusion())
    );
    public static final DeferredBlock<OrbitalPhysicsAssemblerBlock> ORBITAL_PHYSICS_ASSEMBLER = registerWithItem(
        "orbital_physics_assembler",
        () -> new OrbitalPhysicsAssemblerBlock(Properties.of().mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion())
    );

    private static <T extends Block> DeferredBlock<T> registerWithItem(String name, Supplier<T> blockSupplier) {
        DeferredBlock<T> block = BLOCKS.register(name, blockSupplier);
        AeroStarCompItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
}
