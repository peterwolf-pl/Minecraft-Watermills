package com.peterwolf.watermills;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class MechanicalBlockItem extends BlockItem {
	public MechanicalBlockItem(final Block block, final Item.Properties properties) {
		super(block, properties);
	}

	@Override
	public void appendHoverText(
		final ItemStack stack,
		final Item.TooltipContext context,
		final TooltipDisplay tooltipDisplay,
		final Consumer<Component> tooltipAdder,
		final TooltipFlag tooltipFlag
	) {
		tooltipAdder.accept(Component.translatable("tooltip.watermills.mechanical_powered").withStyle(ChatFormatting.GRAY));
	}
}
