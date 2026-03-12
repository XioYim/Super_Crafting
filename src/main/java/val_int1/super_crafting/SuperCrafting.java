package val_int1.super_crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import val_int1.super_crafting.init.ModBlockEntities;
import val_int1.super_crafting.init.ModBlocks;
import val_int1.super_crafting.init.ModMenuTypes;
import val_int1.super_crafting.init.ModRecipeTypes;
import val_int1.super_crafting.network.NetworkHandler;

@Mod(SuperCrafting.MOD_ID)
public class SuperCrafting {

    public static final String MOD_ID = "super_crafting";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static ResourceLocation modLoc(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public SuperCrafting() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(bus);
        ModBlocks.ITEMS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModMenuTypes.MENU_TYPES.register(bus);
        ModRecipeTypes.RECIPE_TYPES.register(bus);
        ModRecipeTypes.RECIPE_SERIALIZERS.register(bus);
        NetworkHandler.register();

        // 注册创造模式物品栏事件
        bus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 加入原版功能方块栏
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.SUPER_CRAFTING_TABLE.get());
        }
    }
}