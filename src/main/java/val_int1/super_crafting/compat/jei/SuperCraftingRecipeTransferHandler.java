package val_int1.super_crafting.compat.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import val_int1.super_crafting.init.ModMenuTypes;
import val_int1.super_crafting.menu.SuperCraftingMenu;
import val_int1.super_crafting.network.SuperCraftFillGridPacket;
import val_int1.super_crafting.network.NetworkHandler;
import val_int1.super_crafting.recipe.SuperCraftingRecipe;

import java.util.Optional;

/**
 * JEI 配方转移处理器（替换旧的 IRecipeTransferInfo）。
 * 通过自定义数据包将配方所需数量的物品从玩家背包填入合成格。
 */
public class SuperCraftingRecipeTransferHandler
        implements IRecipeTransferHandler<SuperCraftingMenu, SuperCraftingRecipe> {

    @Override
    public @NotNull Class<SuperCraftingMenu> getContainerClass() {
        return SuperCraftingMenu.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Optional<MenuType<SuperCraftingMenu>> getMenuType() {
        return Optional.of((MenuType<SuperCraftingMenu>) ModMenuTypes.SUPER_CRAFTING_MENU.get());
    }

    @Override
    public @NotNull RecipeType<SuperCraftingRecipe> getRecipeType() {
        return SuperCraftingCategory.RECIPE_TYPE;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(
            @NotNull SuperCraftingMenu container,
            @NotNull SuperCraftingRecipe recipe,
            @NotNull IRecipeSlotsView recipeSlotsView,
            @NotNull Player player,
            boolean maxTransfer,
            boolean doTransfer) {

        if (doTransfer) {
            NetworkHandler.CHANNEL.sendToServer(
                    new SuperCraftFillGridPacket(
                            container.getBlockEntity().getBlockPos(),
                            recipe.getId()
                    )
            );
        }

        // null = 转移可行（绿色按钮）
        return null;
    }
}
