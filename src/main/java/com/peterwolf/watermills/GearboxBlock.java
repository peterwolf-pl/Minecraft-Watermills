package com.peterwolf.watermills;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;

public class GearboxBlock extends Block {
	public static final MapCodec<GearboxBlock> CODEC = simpleCodec(GearboxBlock::new);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

	public GearboxBlock(final BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(MechanicalProperties.MECHANICAL_POWERED, false));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext context) {
		Direction facing = context.getClickedFace();
		return this.defaultBlockState().setValue(FACING, context.getPlayer() != null && context.getPlayer().isSecondaryUseActive() ? facing.getOpposite() : facing);
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
		boolean powered = hasPerpendicularInput(level, pos, state);
		if (powered != state.getValue(MechanicalProperties.MECHANICAL_POWERED)) {
			WatermillsMod.LOGGER.info("Gearbox at {} {}", pos, powered ? "transmitted drive around a corner" : "stopped transmitting drive");
			level.setBlock(pos, state.setValue(MechanicalProperties.MECHANICAL_POWERED, powered), Block.UPDATE_ALL);
			MechanicalNetwork.scheduleMechanicalNeighbors(level, pos);
		}

		level.scheduleTick(pos, this, MechanicalNetwork.UPDATE_DELAY);
	}

	private static boolean hasPerpendicularInput(final ServerLevel level, final BlockPos pos, final BlockState state) {
		Direction output = state.getValue(FACING);
		for (Direction direction : Direction.values()) {
			if (direction.getAxis() == output.getAxis()) {
				continue;
			}

			BlockState neighborState = level.getBlockState(pos.relative(direction));
			if (neighborState.getBlock() instanceof DriveShaftBlock
				&& neighborState.getValue(DriveShaftBlock.AXIS) == direction.getAxis()
				&& MechanicalNetwork.isPowered(neighborState)) {
				return true;
			}

			if (neighborState.getBlock() instanceof GearboxBlock && MechanicalNetwork.isPowered(neighborState)) {
				return true;
			}
		}

		return false;
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
		builder.add(FACING, MechanicalProperties.MECHANICAL_POWERED);
	}
}
