package com.peterwolf.watermills;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public final class MechanicalNetwork {
	public static final int UPDATE_DELAY = 8;
	private static final int WATERMILL_SPEED = 2;
	private static final int MAX_RESOLVE_DEPTH = 64;
	private static final int MAX_CHAIN_LENGTH = 16;
	private static final Direction[] DIRECTIONS = Direction.values();

	private MechanicalNetwork() {
	}

	public static void scheduleSelf(final Level level, final BlockPos pos, final BlockState state) {
		if (!level.isClientSide()) {
			level.scheduleTick(pos, state.getBlock(), UPDATE_DELAY);
		}
	}

	public static void scheduleMechanicalNeighbors(final ServerLevel level, final BlockPos pos) {
		for (Direction direction : DIRECTIONS) {
			BlockPos neighborPos = pos.relative(direction);
			BlockState neighborState = level.getBlockState(neighborPos);
			if (isMechanicalBlock(neighborState)) {
				level.scheduleTick(neighborPos, neighborState.getBlock(), 1);
			}
		}
	}

	public static boolean isMechanicalBlock(final BlockState state) {
		return state.getBlock() instanceof WatermillBlock
			|| state.getBlock() instanceof DriveShaftBlock
			|| state.getBlock() instanceof GearboxBlock
			|| state.getBlock() instanceof SprocketBlock;
	}

	public static boolean isPowered(final BlockState state) {
		return state.hasProperty(MechanicalProperties.MECHANICAL_POWERED) && state.getValue(MechanicalProperties.MECHANICAL_POWERED);
	}

	public static MechanicalPower resolvePower(final ServerLevel level, final BlockPos pos, final BlockState state) {
		return resolvePower(level, pos, state, new HashSet<>(), 0);
	}

	private static MechanicalPower resolvePower(
		final ServerLevel level,
		final BlockPos pos,
		final BlockState state,
		final Set<BlockPos> visited,
		final int depth
	) {
		if (depth > MAX_RESOLVE_DEPTH || !isMechanicalBlock(state) || !visited.add(pos)) {
			return MechanicalPower.OFF;
		}

		if (state.getBlock() instanceof WatermillBlock) {
			return isPowered(state) ? MechanicalPower.of(WATERMILL_SPEED) : MechanicalPower.OFF;
		}

		if (state.getBlock() instanceof DriveShaftBlock) {
			return resolveShaftPower(level, pos, state, visited, depth);
		}

		if (state.getBlock() instanceof GearboxBlock) {
			return resolveGearboxPower(level, pos, state, visited, depth);
		}

		if (state.getBlock() instanceof SprocketBlock sprocket) {
			return resolveSprocketPower(level, pos, state, sprocket, visited, depth);
		}

		return MechanicalPower.OFF;
	}

	private static MechanicalPower resolveShaftPower(
		final ServerLevel level,
		final BlockPos pos,
		final BlockState state,
		final Set<BlockPos> visited,
		final int depth
	) {
		Direction.Axis axis = state.getValue(DriveShaftBlock.AXIS);
		MechanicalPower best = MechanicalPower.OFF;
		for (Direction direction : DIRECTIONS) {
			if (direction.getAxis() == axis) {
				best = best.strongest(resolveAxisNeighbor(level, pos, direction, axis, visited, depth));
			}
		}
		return best;
	}

	private static MechanicalPower resolveGearboxPower(
		final ServerLevel level,
		final BlockPos pos,
		final BlockState state,
		final Set<BlockPos> visited,
		final int depth
	) {
		Direction output = state.getValue(GearboxBlock.FACING);
		MechanicalPower best = MechanicalPower.OFF;
		for (Direction direction : DIRECTIONS) {
			if (direction.getAxis() != output.getAxis()) {
				best = best.strongest(resolveAxisNeighbor(level, pos, direction, direction.getAxis(), visited, depth));
			}
		}
		return best;
	}

	private static MechanicalPower resolveSprocketPower(
		final ServerLevel level,
		final BlockPos pos,
		final BlockState state,
		final SprocketBlock sprocket,
		final Set<BlockPos> visited,
		final int depth
	) {
		Direction.Axis axis = state.getValue(SprocketBlock.AXIS);
		MechanicalPower best = MechanicalPower.OFF;
		for (Direction direction : DIRECTIONS) {
			if (direction.getAxis() == axis) {
				best = best.strongest(resolveAxisNeighbor(level, pos, direction, axis, visited, depth));
			}
		}

		for (Direction direction : DIRECTIONS) {
			if (direction.getAxis() != axis) {
				best = best.strongest(resolveChainNeighbor(level, pos, direction, axis, sprocket, visited, depth));
			}
		}
		return best;
	}

	private static MechanicalPower resolveAxisNeighbor(
		final ServerLevel level,
		final BlockPos pos,
		final Direction direction,
		final Direction.Axis axis,
		final Set<BlockPos> visited,
		final int depth
	) {
		BlockPos neighborPos = pos.relative(direction);
		BlockState neighborState = level.getBlockState(neighborPos);
		if (!canConnectOnAxis(neighborState, axis)) {
			return MechanicalPower.OFF;
		}
		return resolvePower(level, neighborPos, neighborState, new HashSet<>(visited), depth + 1);
	}

	private static boolean canConnectOnAxis(final BlockState state, final Direction.Axis axis) {
		if (state.getBlock() instanceof WatermillBlock) {
			return true;
		}
		if (state.getBlock() instanceof DriveShaftBlock) {
			return state.getValue(DriveShaftBlock.AXIS) == axis;
		}
		if (state.getBlock() instanceof GearboxBlock) {
			return state.getValue(GearboxBlock.FACING).getAxis() == axis;
		}
		if (state.getBlock() instanceof SprocketBlock) {
			return state.getValue(SprocketBlock.AXIS) == axis;
		}
		return false;
	}

	private static MechanicalPower resolveChainNeighbor(
		final ServerLevel level,
		final BlockPos pos,
		final Direction direction,
		final Direction.Axis axis,
		final SprocketBlock targetSprocket,
		final Set<BlockPos> visited,
		final int depth
	) {
		BlockPos scanPos = pos.relative(direction);
		BlockState scanState = level.getBlockState(scanPos);
		int chainLength = 0;
		while (chainLength < MAX_CHAIN_LENGTH && scanState.is(Blocks.IRON_CHAIN)) {
			chainLength++;
			scanPos = scanPos.relative(direction);
			scanState = level.getBlockState(scanPos);
		}

		if (chainLength == 0 || !(scanState.getBlock() instanceof SprocketBlock sourceSprocket) || scanState.getValue(SprocketBlock.AXIS) != axis) {
			return MechanicalPower.OFF;
		}

		MechanicalPower input = resolvePower(level, scanPos, scanState, new HashSet<>(visited), depth + chainLength + 1);
		return input.scale(sourceSprocket.size().ratioSize(), targetSprocket.size().ratioSize());
	}

	public static boolean isFlowingWater(final BlockGetter level, final BlockPos pos) {
		FluidState fluidState = level.getFluidState(pos);
		if (fluidState.isSource() || fluidState.getType() != Fluids.WATER && fluidState.getType() != Fluids.FLOWING_WATER) {
			return false;
		}

		Vec3 flow = fluidState.getFlow(level, pos);
		return flow.horizontalDistanceSqr() > 0.0001D || fluidState.getType() == Fluids.FLOWING_WATER;
	}
}
