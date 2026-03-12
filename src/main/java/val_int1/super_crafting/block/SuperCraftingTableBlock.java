package val_int1.super_crafting.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import val_int1.super_crafting.blockentity.SuperCraftingTableBE;
import val_int1.super_crafting.init.ModBlockEntities;

public class SuperCraftingTableBlock extends BaseEntityBlock {

    public SuperCraftingTableBlock(Properties properties) {
        super(properties);
    }

    // ---- 方块实体 ----

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SuperCraftingTableBE(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.SUPER_CRAFTING_TABLE_BE.get(),
                SuperCraftingTableBE::serverTick);
    }

    // ---- 右键打开界面 ----

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SuperCraftingTableBE tableBE) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, tableBE, pos);
            }
        }
        return InteractionResult.CONSUME;
    }

    // ---- 破坏时掉落物品 ----

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SuperCraftingTableBE tableBE) {

                // 服务端：先清理方块上方的所有展示实体
                if (!level.isClientSide && level instanceof ServerLevel sl) {
                    tableBE.killDisplayEntitiesPublic(sl);
                }

                // 始终掉落输入网格（0-24）中的物品
                for (int i = 0; i < SuperCraftingTableBE.GRID_SLOTS; i++) {
                    net.minecraft.world.Containers.dropItemStack(
                            level, pos.getX(), pos.getY(), pos.getZ(),
                            tableBE.getItemHandler().getStackInSlot(i)
                    );
                }

                // 输出槽（25）：仅在【合成已完成、未处于计时中】时才掉落。
                // 若玩家在合成进行中强行打破方块，则输出槽此时为空，
                // craftingResult 仅保存在 BE 字段中，随方块销毁一起丢失——这是预期行为。
                if (!tableBE.isCrafting()) {
                    net.minecraft.world.Containers.dropItemStack(
                            level, pos.getX(), pos.getY(), pos.getZ(),
                            tableBE.getItemHandler().getStackInSlot(SuperCraftingTableBE.OUTPUT_SLOT)
                    );
                }

                // 预览槽（26）为虚拟同步槽，永不掉落

                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
