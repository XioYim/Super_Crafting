package val_int1.super_crafting.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import val_int1.super_crafting.SuperCrafting;
import val_int1.super_crafting.block.SuperCraftingTableBlock;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SuperCrafting.MOD_ID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SuperCrafting.MOD_ID);

    // 5x5 大型工作台方块
    public static final RegistryObject<Block> SUPER_CRAFTING_TABLE = registerBlock(
            "super_crafting_table",
            () -> new SuperCraftingTableBlock(BlockBehaviour.Properties.copy(Blocks.CRAFTING_TABLE))
    );

    // ---- 工具方法 ----

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> b = BLOCKS.register(name, block);
        registerBlockItem(name, b);
        return b;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
