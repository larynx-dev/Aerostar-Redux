package larynx.dev.aerostar_redux.block;

import larynx.dev.aerostar_redux.block.entity.AeroStarCompBlockEntityTypes;
import larynx.dev.aerostar_redux.block.entity.DimensionalDriveBlockEntity;
import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.EnumMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class DimensionalDriveBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final EnumMap<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    public DimensionalDriveBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPES.get(Direction.NORTH));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DimensionalDriveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
            ? null
            : createTickerHelper(type, AeroStarCompBlockEntityTypes.DIMENSIONAL_DRIVE.get(), DimensionalDriveBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(DimensionalDriveBlock::new);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof DimensionalDriveBlockEntity drive) {
                ItemStackHandler handler = drive.getStarMapHandler();

                for (int i = 0; i < handler.getSlots(); i++) {
                    popResource(level, pos, handler.getStackInSlot(i));
                }
            }

            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        } else {
            DimensionalDriveBlockEntity drive = findDrive(level, pos);
            if (drive == null) {
                return InteractionResult.PASS;
            } else {
                drive.openMenu(serverPlayer);
                return InteractionResult.sidedSuccess(false);
            }
        }
    }

    @Nullable
    public static DimensionalDriveBlockEntity findDrive(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DimensionalDriveBlockEntity) {
            return (DimensionalDriveBlockEntity)be;
        } else {
            if (level.getServer() != null) {
                for (ServerLevel sl : level.getServer().getAllLevels()) {
                    ServerSubLevelContainer container = SubLevelContainer.getContainer(sl);
                    if (container != null) {
                        for (SubLevel subLevel : container.getAllSubLevels()) {
                            BlockEntity sublevelBe = subLevel.getLevel().getBlockEntity(pos);
                            if (sublevelBe instanceof DimensionalDriveBlockEntity) {
                                return (DimensionalDriveBlockEntity)sublevelBe;
                            }
                        }
                    }
                }
            }

            return null;
        }
    }

    static {
        SHAPES.put(Direction.NORTH, Block.box(0.0, 0.0, 4.0, 16.0, 16.0, 16.0));
        SHAPES.put(Direction.SOUTH, Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 12.0));
        SHAPES.put(Direction.EAST, Block.box(0.0, 0.0, 0.0, 12.0, 16.0, 16.0));
        SHAPES.put(Direction.WEST, Block.box(4.0, 0.0, 0.0, 16.0, 16.0, 16.0));
        SHAPES.put(Direction.UP, Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0));
        SHAPES.put(Direction.DOWN, Block.box(0.0, 4.0, 0.0, 16.0, 16.0, 16.0));
    }
}
