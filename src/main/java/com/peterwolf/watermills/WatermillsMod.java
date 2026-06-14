package com.peterwolf.watermills;

import java.util.function.BiFunction;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatermillsMod implements ModInitializer {
	public static final String MOD_ID = "watermills";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final WatermillBlock WATERMILL = registerBlock(
		"watermill",
		key -> new WatermillBlock(baseWoodProperties(key).dynamicShape()),
		MechanicalBlockItem::new
	);
	public static final DriveShaftBlock DRIVE_SHAFT = registerBlock(
		"drive_shaft",
		key -> new DriveShaftBlock(baseWoodProperties(key).dynamicShape()),
		MechanicalBlockItem::new
	);
	public static final GearboxBlock GEARBOX = registerBlock(
		"gearbox",
		key -> new GearboxBlock(baseWoodProperties(key).dynamicShape()),
		MechanicalBlockItem::new
	);

	@Override
	public void onInitialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(WATERMILL);
			output.accept(DRIVE_SHAFT);
			output.accept(GEARBOX);
		});
		LOGGER.info("Watermills initialized");
	}

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	private static BlockBehaviour.Properties baseWoodProperties(final ResourceKey<Block> key) {
		return BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
			.setId(key)
			.sound(SoundType.WOOD)
			.strength(2.0F, 3.0F);
	}

	private static <T extends Block> T registerBlock(
		final String path,
		final Function<ResourceKey<Block>, T> blockFactory,
		final BiFunction<Block, Item.Properties, BlockItem> itemFactory
	) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id(path));
		T block = Registry.register(BuiltInRegistries.BLOCK, blockKey, blockFactory.apply(blockKey));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id(path));
		BlockItem item = itemFactory.apply(block, new Item.Properties().setId(itemKey));
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		return block;
	}
}
