package val_int1.super_crafting.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkEvent;
import val_int1.super_crafting.blockentity.SuperCraftingTableBE;
import val_int1.super_crafting.init.ModRecipeTypes;
import val_int1.super_crafting.recipe.SuperCraftingRecipe;
import val_int1.super_crafting.recipe.CountedIngredient;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 客户端 → 服务端：JEI 物品转移请求。
 * 服务端将配方所需物品（含正确数量）从玩家背包填入合成格。
 */
public record SuperCraftFillGridPacket(BlockPos pos, ResourceLocation recipeId) {

    public static void encode(SuperCraftFillGridPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeResourceLocation(pkt.recipeId);
    }

    public static SuperCraftFillGridPacket decode(FriendlyByteBuf buf) {
        return new SuperCraftFillGridPacket(buf.readBlockPos(), buf.readResourceLocation());
    }

    public static void handle(SuperCraftFillGridPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level().getBlockEntity(pkt.pos);
            if (!(be instanceof SuperCraftingTableBE tableBE)) return;

            // 找到目标配方
            Optional<SuperCraftingRecipe> recipeOpt = player.level()
                    .getRecipeManager()
                    .getAllRecipesFor(ModRecipeTypes.SUPER_CRAFTING.get())
                    .stream()
                    .filter(r -> r.getId().equals(pkt.recipeId))
                    .findFirst();
            if (recipeOpt.isEmpty()) return;

            SuperCraftingRecipe recipe = recipeOpt.get();
            var handler = tableBE.getItemHandler();

            // 第一步：将合成格中现有物品归还给玩家
            for (int slot = 0; slot < SuperCraftingTableBE.GRID_SLOTS; slot++) {
                ItemStack stack = handler.extractItem(slot, Integer.MAX_VALUE, false);
                if (!stack.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(player, stack);
                }
            }

            // 第二步：从玩家背包按配方数量填入合成格
            List<CountedIngredient> ingredients = recipe.getCountedIngredients();
            Inventory inv = player.getInventory();

            for (int slot = 0; slot < ingredients.size(); slot++) {
                CountedIngredient ci = ingredients.get(slot);
                if (ci.isEmpty()) continue;

                int needed = ci.count();
                for (int invSlot = 0; invSlot < inv.getContainerSize() && needed > 0; invSlot++) {
                    ItemStack invStack = inv.getItem(invSlot);
                    if (invStack.isEmpty() || !ci.ingredient().test(invStack)) continue;

                    int canTake = Math.min(needed, invStack.getCount());
                    ItemStack toPlace = invStack.copy();
                    toPlace.setCount(canTake);
                    invStack.shrink(canTake);
                    if (invStack.isEmpty()) inv.setItem(invSlot, ItemStack.EMPTY);

                    handler.insertItem(slot, toPlace, false);
                    needed -= canTake;
                }
            }

            inv.setChanged();
        });
        ctx.get().setPacketHandled(true);
    }
}
