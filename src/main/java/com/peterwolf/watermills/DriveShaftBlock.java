package com.peterwolf.watermills;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;

public class DriveShaftBlock extends Block {
	public static final MapCodec<DriveShaftBlock> CODEC = simpleCodec(DriveShaftBlock::new);
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

	public DriveShaftBlock(final BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any()
			.setValue(AXIS, Direction.Axis.X)
			.setValue(MechanicalProperties.MECHANICAL_POWERED, false)
			.setValue(MechanicalProperties.SPEED, 0));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext context) {
		return this.defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
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
		MechanicalPower power = MechanicalNetwork.resolvePower(level, pos, state);
		if (power.powered() != state.getValue(MechanicalProperties.MECHANICAL_POWERED)
			|| power.speed() != state.getValue(MechanicalProperties.SPEED)) {
			WatermillsMod.LOGGER.info("Drive shaft at {} {}", pos, power.powered() ? "received drive speed " + power.speed() : "lost drive");
			level.setBlock(
				pos,
				state.setValue(MechanicalProperties.MECHANICAL_POWERED, power.powered()).setValue(MechanicalProperties.SPEED, power.speed()),
				Block.UPDATE_ALL
			);
			MechanicalNetwork.scheduleMechanicalNeighbors(level, pos);
		}

		level.scheduleTick(pos, this, MechanicalNetwork.UPDATE_DELAY);
	}

	@Override
	protected BlockState rotate(final BlockState state, final Rotation rotation) {
		Direction.Axis axis = state.getValue(AXIS);
		if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) {
			if (axis == Direction.Axis.X) {
				return state.setValue(AXIS, Direction.Axis.Z);
			}
			if (axis == Direction.Axis.Z) {
				return state.setValue(AXIS, Direction.Axis.X);
			}
		}
		return state;
	}

	@Override
	protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, MechanicalProperties.MECHANICAL_POWERED, MechanicalProperties.SPEED);
	}
}
