package val_int1.super_crafting.init;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import val_int1.super_crafting.SuperCrafting;
import val_int1.super_crafting.menu.SuperCraftingMenu;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, SuperCrafting.MOD_ID);

    public static final RegistryObject<MenuType<SuperCraftingMenu>> SUPER_CRAFTING_MENU =
            MENU_TYPES.register("super_crafting_menu",
                    () -> IForgeMenuType.create(SuperCraftingMenu::new));
}
