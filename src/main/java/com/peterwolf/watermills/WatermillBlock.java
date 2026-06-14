package com.peterwolf.watermills;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WatermillBlock extends Block {
	public static final MapCodec<WatermillBlock> CODEC = simpleCodec(WatermillBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape NORTH_SOUTH_SHAPE = Block.box(-16.0D, 0.0D, 5.0D, 32.0D, 48.0D, 11.0D);
	private static final VoxelShape EAST_WEST_SHAPE = Block.box(5.0D, 0.0D, -16.0D, 11.0D, 48.0D, 32.0D);

	public WatermillBlock(final BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(MechanicalProperties.ACTIVE, false)
			.setValue(MechanicalProperties.MECHANICAL_POWERED, false));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
		MechanicalNetwork.scheduleSelf(level, pos, state);
	}

	@Override
	protected void neighborChanged(
		final BlockState state,
		final Level level,
		final BlockPos pos,
		final Block neighborBlock,
		final Orientation orientation,
		final boolean movedByPiston
	) {
		MechanicalNetwork.scheduleSelf(level, pos, state);
	}

	@Override
	protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
		boolean active = hasFlowingWater(level, pos, state);
		if (active != state.getValue(MechanicalProperties.ACTIVE)) {
			WatermillsMod.LOGGER.info("Watermill at {} {}", pos, active ? "detected flowing water" : "lost flowing water");
			level.setBlock(pos, state.setValue(MechanicalProperties.ACTIVE, active).setValue(MechanicalProperties.MECHANICAL_POWERED, active), Block.UPDATE_ALL);
			MechanicalNetwork.scheduleMechanicalNeighbors(level, pos);
		}

		level.scheduleTick(pos, this, MechanicalNetwork.UPDATE_DELAY);
	}

	private static boolean hasFlowingWater(final LevelReader level, final BlockPos pos, final BlockState state) {
		Direction facing = state.getValue(FACING);
		BlockPos bottomCenter = pos.below();
		if (MechanicalNetwork.isFlowingWater(level, bottomCenter)) {
			return true;
		}

		Direction right = facing.getClockWise();
		Direction left = facing.getCounterClockWise();
		return MechanicalNetwork.isFlowingWater(level, bottomCenter.relative(facing))
			|| MechanicalNetwork.isFlowingWater(level, bottomCenter.relative(facing.getOpposite()))
			|| MechanicalNetwork.isFlowingWater(level, bottomCenter.relative(right))
			|| MechanicalNetwork.isFlowingWater(level, bottomCenter.relative(left));
	}

	@Override
	protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
		Direction.Axis axis = state.getValue(FACING).getAxis();
		return axis == Direction.Axis.X ? EAST_WEST_SHAPE : NORTH_SOUTH_SHAPE;
	}

	@Override
	protected BlockState rotate(final BlockState state, final Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(final BlockState state, final Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, MechanicalProperties.ACTIVE, MechanicalProperties.MECHANICAL_POWERED);
	}
}
