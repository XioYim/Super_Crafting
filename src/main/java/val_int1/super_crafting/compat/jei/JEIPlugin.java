package val_int1.super_crafting.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import val_int1.super_crafting.SuperCrafting;
import val_int1.super_crafting.init.ModBlocks;
import val_int1.super_crafting.init.ModRecipeTypes;
import val_int1.super_crafting.recipe.SuperCraftingRecipe;

import java.util.List;

@JeiPlugin
public class JEIPlugin implements IModPlugin {

    public static final ResourceLocation PLUGIN_ID =
            new ResourceLocation(SuperCrafting.MOD_ID, "jei_plugin");

    /** JEI 运行时实例，客户端可用时由 onRuntimeAvailable 赋值 */
    @Nullable
    public static volatile IJeiRuntime jeiRuntime = null;

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        jeiRuntime = runtime;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new SuperCraftingCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        List<SuperCraftingRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.SUPER_CRAFTING.get());
        registration.addRecipes(SuperCraftingCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.SUPER_CRAFTING_TABLE.get()),
                SuperCraftingCategory.RECIPE_TYPE
        );
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                new SuperCraftingRecipeTransferHandler(),
                SuperCraftingCategory.RECIPE_TYPE
        );
    }

    /**
     * 打开 JEI 中的超级合成台配方列表。
     * 由 SuperCraftingScreen 在点击灰色箭头时调用。
     */
    public static void openSuperCraftingCategory() {
        if (jeiRuntime != null) {
            jeiRuntime.getRecipesGui().showTypes(List.of(SuperCraftingCategory.RECIPE_TYPE));
        }
    }
}
