package com.peterwolf.watermills;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public final class MechanicalNetwork {
	public static final int UPDATE_DELAY = 8;
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
		return state.getBlock() instanceof WatermillBlock || state.getBlock() instanceof DriveShaftBlock || state.getBlock() instanceof GearboxBlock;
	}

	public static boolean isPowered(final BlockState state) {
		return state.hasProperty(MechanicalProperties.MECHANICAL_POWERED) && state.getValue(MechanicalProperties.MECHANICAL_POWERED);
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
