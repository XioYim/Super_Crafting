package val_int1.super_crafting.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import val_int1.super_crafting.SuperCrafting;
import val_int1.super_crafting.init.ModBlocks;
import val_int1.super_crafting.recipe.SuperCraftingRecipe;
import val_int1.super_crafting.recipe.CountedIngredient;

import java.util.Arrays;
import java.util.List;

public class SuperCraftingCategory implements IRecipeCategory<SuperCraftingRecipe> {

    public static final RecipeType<SuperCraftingRecipe> RECIPE_TYPE =
            RecipeType.create(SuperCrafting.MOD_ID, "super_crafting", SuperCraftingRecipe.class);

    private static final ResourceLocation SLOT_TEXTURE =
            new ResourceLocation("super_crafting", "textures/gui/inventory.png");

    private static final ResourceLocation CRAFTING_TEXTURE =
            new ResourceLocation("super_crafting", "textures/gui/crafting_table.png");


    // ================= UI尺寸 =================

    private static final int SLOT_SIZE = 18;

    private static final int BIG_SLOT_W = 26;
    private static final int BIG_SLOT_H = 27;

    private static final int GRID_SIZE = 5;

    private static final int TOP_PADDING = 6;
    /** 输出槽底部到输入网格顶部的间距（含合成时间文字，字体高≈9px，需留够间隙）*/
    private static final int GAP = 14;

    public static final int WIDTH = 90;

    public static final int HEIGHT =
            TOP_PADDING + BIG_SLOT_H + GAP + GRID_SIZE * SLOT_SIZE;

    private static final int OUTPUT_X = (WIDTH - BIG_SLOT_W) / 2;
    private static final int OUTPUT_Y = TOP_PADDING;

    private static final int INPUT_START_Y =
            OUTPUT_Y + BIG_SLOT_H + GAP;


    // ================= JEI组件 =================

    private final IDrawable icon;

    public SuperCraftingCategory(IGuiHelper guiHelper) {

        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.SUPER_CRAFTING_TABLE.get())
        );
    }

    @Override
    public @NotNull RecipeType<SuperCraftingRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.super_crafting.super_crafting");
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    // ================= JEI槽位布局 =================

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder,
                          @NotNull SuperCraftingRecipe recipe,
                          @NotNull IFocusGroup focuses) {

        // 输出槽
        builder.addSlot(
                RecipeIngredientRole.OUTPUT,
                OUTPUT_X + 5,
                OUTPUT_Y + 6
        ).addItemStack(recipe.getResult());

        List<CountedIngredient> ingredients = recipe.getCountedIngredients();

        // 5×5 输入槽
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {

                int index = col + row * GRID_SIZE;
                if (index >= ingredients.size()) continue;

                CountedIngredient ci = ingredients.get(index);
                if (ci.isEmpty()) continue;

                List<ItemStack> stacks = Arrays.stream(ci.ingredient().getItems())
                        .map(stack -> {
                            ItemStack copy = stack.copy();
                            copy.setCount(ci.count());
                            // 将配方要求的 NBT 写入显示用 ItemStack，使 JEI 正确呈现附魔等细节
                            if (ci.nbt() != null) {
                                copy.setTag(ci.nbt().copy());
                            }
                            return copy;
                        })
                        .toList();

                builder.addSlot(
                        RecipeIngredientRole.INPUT,
                        col * SLOT_SIZE + 1,
                        INPUT_START_Y + row * SLOT_SIZE + 1
                ).addItemStacks(stacks);
            }
        }
    }

    // ================= JEI UI绘制 =================

    @Override
    public void draw(@NotNull SuperCraftingRecipe recipe,
                     @NotNull IRecipeSlotsView recipeSlotsView,
                     @NotNull GuiGraphics guiGraphics,
                     double mouseX,
                     double mouseY) {

        // ===== 大输出槽 =====
        guiGraphics.blit(
                CRAFTING_TEXTURE,
                OUTPUT_X,
                OUTPUT_Y,
                119,
                29,
                BIG_SLOT_W,
                BIG_SLOT_H
        );

        // ===== 输入槽 =====
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {

                int x = col * SLOT_SIZE;
                int y = INPUT_START_Y + row * SLOT_SIZE;

                guiGraphics.blit(
                        SLOT_TEXTURE,
                        x,
                        y,
                        7,
                        83,
                        SLOT_SIZE,
                        SLOT_SIZE
                );
            }
        }

        // ===== 合成时间文字（输出槽下方）=====
        int workSec = (int) Math.ceil(recipe.getWorkTime() / 20.0);
        Component timeText = Component.translatable("jei.super_crafting.work_time", workSec);
        var font = Minecraft.getInstance().font;
        int textX = (WIDTH - font.width(timeText)) / 2;
        // 在 GAP 范围内垂直居中（font.lineHeight ≈ 9）
        int textY = OUTPUT_Y + BIG_SLOT_H + (GAP - font.lineHeight) / 2;
        guiGraphics.drawString(font, timeText, textX, textY, 0x404040, false);
    }
}
