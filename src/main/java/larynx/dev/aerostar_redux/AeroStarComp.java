package larynx.dev.aerostar_redux;

import larynx.dev.aerostar_redux.block.AeroStarCompBlocks;
import larynx.dev.aerostar_redux.block.entity.AeroStarCompBlockEntityTypes;
import larynx.dev.aerostar_redux.command.DriveTransferCommand;
import larynx.dev.aerostar_redux.item.AeroStarCompCreativeTabs;
import larynx.dev.aerostar_redux.item.AeroStarCompItems;
import larynx.dev.aerostar_redux.menu.AeroStarCompMenuTypes;
import larynx.dev.aerostar_redux.physics.OrbitGravitySystem;
import larynx.dev.aerostar_redux.transfer.SubLevelDimensionTransfer;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import org.slf4j.Logger;

@Mod("aerostar_redux")
public class AeroStarComp {
    public static final String MOD_ID = "aerostar_redux";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AeroStarComp(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Create: Northstar Aeronautics Bridge initializing!");
        AeroStarCompBlocks.BLOCKS.register(modEventBus);
        AeroStarCompItems.ITEMS.register(modEventBus);
        AeroStarCompCreativeTabs.TABS.register(modEventBus);
        AeroStarCompBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        AeroStarCompMenuTypes.MENU_TYPES.register(modEventBus);
        modEventBus.addListener(this::onAddBlockEntityBlocks);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(OrbitGravitySystem::onPrePhysicsTick);
        NeoForge.EVENT_BUS.addListener(OrbitGravitySystem::onServerTick);
    }

    private void onAddBlockEntityBlocks(BlockEntityTypeAddBlocksEvent event) {
        ResourceKey<BlockEntityType<?>> physicsAssembler = ResourceKey.create(
            Registries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("simulated", "physics_assembler")
        );
        event.modify(physicsAssembler, AeroStarCompBlocks.ORBITAL_PHYSICS_ASSEMBLER.get());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        DriveTransferCommand.register(event.getDispatcher());
    }

    private void onServerTick(Post event) {
        SubLevelDimensionTransfer.tick();
    }
}
