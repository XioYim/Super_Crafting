package val_int1.super_crafting.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import val_int1.super_crafting.SuperCrafting;
import val_int1.super_crafting.client.tooltip.ArrowTooltipData;
import val_int1.super_crafting.client.tooltip.ClientArrowTooltipComponent;
import val_int1.super_crafting.init.ModMenuTypes;
import val_int1.super_crafting.screen.SuperCraftingScreen;

@Mod.EventBusSubscriber(modid = SuperCrafting.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.SUPER_CRAFTING_MENU.get(), SuperCraftingScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ArrowTooltipData.class, ClientArrowTooltipComponent::new);
    }
}

