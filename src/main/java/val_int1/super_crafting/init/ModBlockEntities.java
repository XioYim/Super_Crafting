package val_int1.super_crafting.init;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import val_int1.super_crafting.SuperCrafting;
import val_int1.super_crafting.blockentity.SuperCraftingTableBE;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SuperCrafting.MOD_ID);

    public static final RegistryObject<BlockEntityType<SuperCraftingTableBE>> SUPER_CRAFTING_TABLE_BE =
            BLOCK_ENTITIES.register("super_crafting_table",
                    () -> BlockEntityType.Builder
                            .of(SuperCraftingTableBE::new, ModBlocks.SUPER_CRAFTING_TABLE.get())
                            .build(null));
}
