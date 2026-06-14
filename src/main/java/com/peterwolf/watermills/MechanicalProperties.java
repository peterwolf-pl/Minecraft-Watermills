package com.peterwolf.watermills;

import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class MechanicalProperties {
	public static final BooleanProperty MECHANICAL_POWERED = BooleanProperty.create("mechanical_powered");
	public static final BooleanProperty ACTIVE = BlockStateProperties.POWERED;

	private MechanicalProperties() {
	}
}
