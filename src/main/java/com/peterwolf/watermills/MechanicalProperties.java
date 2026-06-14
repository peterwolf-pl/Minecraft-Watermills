package com.peterwolf.watermills;

import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class MechanicalProperties {
	public static final BooleanProperty MECHANICAL_POWERED = BooleanProperty.create("mechanical_powered");
	public static final BooleanProperty ACTIVE = BlockStateProperties.POWERED;
	public static final IntegerProperty SPEED = IntegerProperty.create("speed", 0, 3);

	private MechanicalProperties() {
	}
}
