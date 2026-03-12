package val_int1.super_crafting.compat.jei;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import val_int1.super_crafting.init.ModMenuTypes;
import val_int1.super_crafting.menu.SuperCraftingMenu;
import val_int1.super_crafting.recipe.SuperCraftingRecipe;
import val_int1.super_crafting.recipe.CountedIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JEI 配方转移辅助类。
 *
 * getRecipeSlots() 返回的槽位顺序必须与 SuperCraftingCategory.setRecipe() 添加槽位的顺序
 * 完全一致，否则 JEI 会将错误的槽位映射到错误的容器格子。
 *
 * setRecipe 添加顺序：
 *   ① 输出槽（OUTPUT）
 *   ② 非空输入槽（INPUT），按行优先逐格扫描
 */
public class SuperCraftingRecipeTransferInfo implements IRecipeTransferInfo<SuperCraftingMenu, SuperCraftingRecipe> {

    @Override @NotNull
    public Class<SuperCraftingMenu> getContainerClass() { return SuperCraftingMenu.class; }

    @Override @NotNull
    public Optional<MenuType<SuperCraftingMenu>> getMenuType() {
        return Optional.of((MenuType<SuperCraftingMenu>) ModMenuTypes.SUPER_CRAFTING_MENU.get());
    }

    @Override @NotNull
    public RecipeType<SuperCraftingRecipe> getRecipeType() { return SuperCraftingCategory.RECIPE_TYPE; }

    @Override
    public boolean canHandle(@NotNull SuperCraftingMenu container, @NotNull SuperCraftingRecipe recipe) {
        return true;
    }

    /**
     * 顺序必须与 SuperCraftingCategory.setRecipe() 完全对应：
     *   [0] 输出槽（container slot 25）
     *   [1..K] 非空输入槽（container slots 0-24，按行优先跳过空位）
     */
    @Override @NotNull
    public List<Slot> getRecipeSlots(@NotNull SuperCraftingMenu container,
                                     @NotNull SuperCraftingRecipe recipe) {
        List<Slot> slots = new ArrayList<>();

        // ① 输出槽（对应 setRecipe 中第一个 addSlot OUTPUT）
        slots.add(container.getSlot(SuperCraftingMenu.OUTPUT_SLOT));

        // ② 非空输入槽（与 setRecipe 中 INPUT addSlot 顺序一致）
        List<CountedIngredient> ingredients = recipe.getCountedIngredients();
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int idx = col + row * 5;
                if (idx >= ingredients.size()) continue;
                if (ingredients.get(idx).isEmpty()) continue;
                slots.add(container.getSlot(SuperCraftingMenu.INPUT_SLOT_START + idx));
            }
        }

        return slots;
    }

    /**
     * 玩家背包 + 快捷栏（用于 JEI 搜寻可用材料）。
     */
    @Override @NotNull
    public List<Slot> getInventorySlots(@NotNull SuperCraftingMenu container,
                                        @NotNull SuperCraftingRecipe recipe) {
        List<Slot> slots = new ArrayList<>();
        for (int i = SuperCraftingMenu.INV_SLOT_START; i <= SuperCraftingMenu.HOTBAR_SLOT_END; i++) {
            slots.add(container.getSlot(i));
        }
        return slots;
    }
}
