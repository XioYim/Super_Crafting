package val_int1.super_crafting.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import val_int1.super_crafting.blockentity.SuperCraftingTableBE;
import val_int1.super_crafting.init.ModMenuTypes;

public class SuperCraftingMenu extends AbstractContainerMenu {

    private final SuperCraftingTableBE blockEntity;
    private final ContainerData data;

    public static final int INPUT_SLOT_START  = 0;
    public static final int INPUT_SLOT_END    = 24;
    public static final int OUTPUT_SLOT       = 25;
    public static final int PREVIEW_SLOT_IDX  = 26;

    private int progressValue    = 0;
    private int maxProgressValue = 0;

    public static final int INV_SLOT_START    = 27;
    public static final int INV_SLOT_END      = 53;
    public static final int HOTBAR_SLOT_START = 54;
    public static final int HOTBAR_SLOT_END   = 62;

    // ======================
    // GUI 布局常量
    // ======================

    public static final int GRID_START_X = 10;

    // 上间距 +8 (10 -> 18)
    public static final int GRID_START_Y = 18;

    // 箭头和输出槽与 5×5 网格第 3 行（y = GRID_START_Y + 2×18 = 54）居中对齐
    // 行中心 y = 54 + 9 = 63；18×18 箭头: ARROW_Y = 54，输出槽同理
    // ARROW_X = 111：在输入格右边（x=100）和输出槽 26×26 左视觉边（x=140）之间居中
    public static final int ARROW_X  = 111;
    public static final int ARROW_Y  = 54;
    public static final int ARROW_W  = 18;
    public static final int ARROW_H  = 18;

    public static final int OUTPUT_X = 144;
    public static final int OUTPUT_Y = 54;

    // UI 与玩家背包间距 +8
    public static final int PLAYER_INV_Y = 118;

    // Hotbar 也一起 +8
    public static final int HOTBAR_Y     = 176;

    public static final int DATA_PROGRESS     = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_COUNT        = 2;

    public SuperCraftingMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, getBlockEntity(playerInv, buf));
    }

    private static SuperCraftingTableBE getBlockEntity(Inventory playerInv, FriendlyByteBuf buf) {
        BlockEntity be = playerInv.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof SuperCraftingTableBE tableBE) return tableBE;
        throw new IllegalStateException("Block entity is not SuperCraftingTableBE");
    }

    public SuperCraftingMenu(int containerId, Inventory playerInv, SuperCraftingTableBE be) {
        super(ModMenuTypes.SUPER_CRAFTING_MENU.get(), containerId);
        this.blockEntity = be;

        this.data = new SimpleContainerData(DATA_COUNT) {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_PROGRESS     -> be.getProgress();
                    case DATA_MAX_PROGRESS -> be.getMaxProgress();
                    default -> 0;
                };
            }
            @Override
            public void set(int index, int value) {
                if (index == DATA_PROGRESS)     progressValue    = value;
                if (index == DATA_MAX_PROGRESS) maxProgressValue = value;
            }
        };

        addDataSlots(data);

        IItemHandler handler = be.getItemHandler();

        // 5×5 输入格
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int slotIndex = col + row * 5;
                addSlot(new SlotItemHandler(
                        handler,
                        slotIndex,
                        GRID_START_X + col * 18,
                        GRID_START_Y + row * 18
                ));
            }
        }

        // 输出槽
        addSlot(new SlotItemHandler(handler, SuperCraftingTableBE.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) { return false; }

            @Override
            public boolean mayPickup(@NotNull Player player) { return true; }
        });

        // 隐藏预览槽
        addSlot(new SlotItemHandler(handler, SuperCraftingTableBE.PREVIEW_SLOT, -10000, -10000) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack)   { return false; }
            @Override
            public boolean mayPickup(@NotNull Player player)    { return false; }
        });

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9,
                        10 + col * 18,
                        PLAYER_INV_Y + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col,
                    10 + col * 18,
                    HOTBAR_Y));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack returnStack = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            returnStack = stack.copy();

            if (index == OUTPUT_SLOT) {
                if (!moveItemStackTo(stack, INV_SLOT_START, HOTBAR_SLOT_END + 1, true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(stack, returnStack);

            } else if (index == PREVIEW_SLOT_IDX) {
                return ItemStack.EMPTY;

            } else if (index >= INV_SLOT_START) {
                if (!moveItemStackTo(stack, INPUT_SLOT_START, INPUT_SLOT_END + 1, false)) {
                    if (index < HOTBAR_SLOT_START) {
                        if (!moveItemStackTo(stack, HOTBAR_SLOT_START, HOTBAR_SLOT_END + 1, false))
                            return ItemStack.EMPTY;
                    } else {
                        if (!moveItemStackTo(stack, INV_SLOT_START, INV_SLOT_END + 1, false))
                            return ItemStack.EMPTY;
                    }
                }
            } else {
                if (!moveItemStackTo(stack, INV_SLOT_START, HOTBAR_SLOT_END + 1, false))
                    return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();

            if (stack.getCount() == returnStack.getCount())
                return ItemStack.EMPTY;

            slot.onTake(player, stack);
        }

        return returnStack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return blockEntity.stillValid(player);
    }

    public int getProgress()    { return progressValue; }
    public int getMaxProgress() { return maxProgressValue; }
    public SuperCraftingTableBE getBlockEntity() { return blockEntity; }
}
