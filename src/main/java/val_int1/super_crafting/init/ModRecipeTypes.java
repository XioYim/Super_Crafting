package val_int1.super_crafting.init;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import val_int1.super_crafting.SuperCrafting;
import val_int1.super_crafting.recipe.SuperCraftingRecipe;

public class ModRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, SuperCrafting.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SuperCrafting.MOD_ID);

    // 配方类型
    public static final RegistryObject<RecipeType<SuperCraftingRecipe>> SUPER_CRAFTING =
            RECIPE_TYPES.register("super_crafting",
                    () -> RecipeType.simple(SuperCrafting.modLoc("super_crafting")));

    // 配方序列化器
    public static final RegistryObject<RecipeSerializer<SuperCraftingRecipe>> SUPER_CRAFTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("super_crafting",
                    SuperCraftingRecipe.Serializer::new);
}
