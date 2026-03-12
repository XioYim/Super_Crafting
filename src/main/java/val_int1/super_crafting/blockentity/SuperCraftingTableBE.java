package val_int1.super_crafting.blockentity;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import val_int1.super_crafting.init.ModBlockEntities;
import val_int1.super_crafting.init.ModRecipeTypes;
import val_int1.super_crafting.menu.SuperCraftingMenu;
import val_int1.super_crafting.recipe.SuperCraftingRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SuperCraftingTableBE extends BlockEntity implements MenuProvider {

    public static final int GRID_SLOTS   = 25;   // 5×5 输入
    public static final int OUTPUT_SLOT  = 25;
    public static final int PREVIEW_SLOT = 26;   // 客户端预览同步槽（隐藏）
    public static final int TOTAL_SLOTS  = 27;

    // 预览物品的 NBT 标识：保存配方所需合成时间
    public static final String WORK_TIME_TAG = "SuperCraftingWorkTime";

    /**
     * 展示实体专用 Minecraft 实体标签（entity tag）。
     * 用 entity.addTag(DISPLAY_ENTITY_TAG) 标记，之后可通过
     * @e[tag=super_crafting_display] 指令定位，也可在扫描时精准删除。
     */
    public static final String DISPLAY_ENTITY_TAG = "super_crafting_display";

    /** 构造一个预览物品（含合成时间 NBT），仅用于同步到客户端 */
    public static ItemStack makePreview(ItemStack original, int workTime) {
        if (original.isEmpty()) return ItemStack.EMPTY;
        ItemStack preview = original.copy();
        preview.getOrCreateTag().putInt(WORK_TIME_TAG, workTime);
        return preview;
    }

    /** 从预览物品读取合成时间（tick），非预览则返回 0 */
    public static int getPreviewWorkTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(WORK_TIME_TAG) : 0;
    }

    // -------- 物品栏 --------

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) { setChanged(); }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot < GRID_SLOTS;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == PREVIEW_SLOT) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        public void deserializeNBT(net.minecraft.nbt.CompoundTag nbt) {
            int savedSize = nbt.getInt("Size");
            if (savedSize != TOTAL_SLOTS) {
                nbt = nbt.copy();
                nbt.putInt("Size", TOTAL_SLOTS);
            }
            super.deserializeNBT(nbt);
        }
    };

    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);

    // -------- 合成核心状态 --------
    private int progress    = 0;
    private int maxProgress = 0;
    private boolean isCrafting = false;
    private ItemStack craftingResult = ItemStack.EMPTY;

    // -------- 合成附加效果状态（持久化到 NBT） --------
    /** tick_command_block 命令列表（合成期间每 craftingTickInterval tick 执行一次）。 */
    private List<String> craftingTickCommands   = new ArrayList<>();
    /** tick_command_block 的执行间隔（tick），最小 1 */
    private int craftingTickInterval = 1;
    /** finish_command 命令列表（合成完成时执行一次）。 */
    private List<String> craftingFinishCommands = new ArrayList<>();
    /**
     * summon_item 召唤的展示物品实体 UUID（主键）。
     * 合成完成且玩家从输出槽取出物品后，由 serverTick 负责移除实体。
     * 额外通过 DISPLAY_ENTITY_TAG 标记实体，提供备用定位手段（仅在本方块正上方 1×1 列内扫描）。
     */
    @Nullable
    private UUID craftingDisplayEntityUUID = null;

    public SuperCraftingTableBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUPER_CRAFTING_TABLE_BE.get(), pos, state);
    }

    // ===================== Tick =====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, SuperCraftingTableBE be) {
        if (be.isCrafting) {
            // tick_command_block：按间隔执行（progress 从 0 开始，interval=1 时每 tick 执行）
            if (!be.craftingTickCommands.isEmpty()
                    && be.progress % be.craftingTickInterval == 0) {
                be.executeCraftingTickCommands((ServerLevel) level);
            }
            be.progress++;
            be.setChanged();
            if (be.progress >= be.maxProgress) {
                be.finishCrafting();
            }
        } else {
            // 非合成状态：玩家取走输出槽物品后，移除展示实体
            if (be.craftingDisplayEntityUUID != null
                    && be.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
                if (level instanceof ServerLevel sl) {
                    be.killDisplayEntities(sl);
                }
                be.setChanged();
            }
            be.updatePreviewSlot(level);
        }
    }

    private void updatePreviewSlot(Level level) {
        ItemStack currentPreview = itemHandler.getStackInSlot(PREVIEW_SLOT);
        Optional<SuperCraftingRecipe> recipe = findMatchingRecipe(level);

        if (recipe.isPresent()) {
            SuperCraftingRecipe r = recipe.get();
            ItemStack wanted = makePreview(r.getResult(), r.getWorkTime());
            if (!ItemStack.matches(currentPreview, wanted)) {
                itemHandler.setStackInSlot(PREVIEW_SLOT, wanted);
            }
        } else {
            if (!currentPreview.isEmpty()) {
                itemHandler.setStackInSlot(PREVIEW_SLOT, ItemStack.EMPTY);
            }
        }
    }

    // ===================== 展示实体清理 =====================

    /**
     * 清理方块上方的展示实体：
     * 1. 通过 UUID 精准定位并删除本方块记录的实体（主路径）
     * 2. 再在【本方块正上方的 1×1 竖直柱体】内扫描带 DISPLAY_ENTITY_TAG 标签的 ItemEntity，
     *    仅覆盖本方块的列，不向外扩展，避免误删相邻方块的展示实体（备用/兜底路径）。
     */
    private void killDisplayEntities(ServerLevel serverLevel) {
        // 主路径：通过 UUID 精准删除
        if (craftingDisplayEntityUUID != null) {
            Entity entity = serverLevel.getEntity(craftingDisplayEntityUUID);
            if (entity != null) entity.discard();
            craftingDisplayEntityUUID = null;
        }
        // 备用路径：搜索范围严格限定在本方块正上方的 1×1 列内（x/z 与方块完全对齐），
        // 高度取 y+0.5 ~ y+3.0，不会影响相邻方块上方的展示实体。
        AABB searchBox = new AABB(
                worldPosition.getX(),       worldPosition.getY() + 0.5, worldPosition.getZ(),
                worldPosition.getX() + 1.0, worldPosition.getY() + 3.0, worldPosition.getZ() + 1.0
        );
        serverLevel.getEntitiesOfClass(ItemEntity.class, searchBox,
                        e -> e.getTags().contains(DISPLAY_ENTITY_TAG))
                .forEach(Entity::discard);
    }

    /**
     * 供外部（如 SuperCraftingTableBlock.onRemove）调用的公开入口。
     */
    public void killDisplayEntitiesPublic(ServerLevel serverLevel) {
        killDisplayEntities(serverLevel);
    }

    // ===================== 合成逻辑 =====================

    /**
     * 客户端点击金色箭头后，由网络包调用此方法开始合成。
     *
     * <p>前置检查：
     *   - 输出槽必须为空（防止产物丢失）
     *   - 未正在合成
     *   - 存在匹配配方
     *
     * <p>所有命令均以方块中心为坐标原点（~ 基准），OP 权限级别（4）执行。
     * withPermission(4) 只影响本次命令执行的权限级别，不修改玩家的实际 OP 状态。
     */
    public void startCrafting() {
        if (level == null || level.isClientSide || isCrafting) return;

        // 服务端二次校验：输出槽非空时拒绝合成，防止产物丢失
        if (!itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty()) return;

        Optional<SuperCraftingRecipe> recipe = findMatchingRecipe(level);
        if (recipe.isEmpty()) return;

        SuperCraftingRecipe r = recipe.get();

        // 消耗输入材料（预览槽保留，合成中仍可显示目标物品）
        ContainerWrapper wrapper = new ContainerWrapper(itemHandler, GRID_SLOTS);
        r.consumeIngredients(wrapper);

        // 开始计时
        isCrafting     = true;
        craftingResult = r.getResult().copy();
        maxProgress    = r.getWorkTime();
        progress       = 0;

        // 存储本次合成的附加效果数据
        craftingTickCommands   = new ArrayList<>(r.getTickCommandBlock());
        craftingTickInterval   = r.getTickInterval();
        craftingFinishCommands = new ArrayList<>(r.getFinishCommand());

        ServerLevel serverLevel = (ServerLevel) level;
        MinecraftServer server  = serverLevel.getServer();

        // ── 效果 1：先清理旧展示实体，再召唤新展示物品 ──────────────────────
        // 无论本次配方是否启用 summon_item，都先做一次清理，防止残留。
        killDisplayEntities(serverLevel);

        if (r.isSummonItem() && !craftingResult.isEmpty()) {
            ItemEntity display = new ItemEntity(
                    serverLevel,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.2,
                    worldPosition.getZ() + 0.5,
                    craftingResult.copy()
            );
            // 添加实体标签，使其可通过 @e[tag=super_crafting_display] 指令或扫描精准定位
            display.addTag(DISPLAY_ENTITY_TAG);
            display.setNeverPickUp();
            display.setNoGravity(true);
            display.setInvulnerable(true);
            display.setDeltaMovement(Vec3.ZERO);
            display.lifespan = Integer.MAX_VALUE; // Forge 字段，防止自动消失
            serverLevel.addFreshEntity(display);
            craftingDisplayEntityUUID = display.getUUID();
        }

        // ── 效果 2：以最近玩家为 @s 执行命令（start_command_player） ─────────
        if (server != null && !r.getStartCommandPlayer().isEmpty()) {
            Player nearest = serverLevel.getNearestPlayer(
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5,
                    -1, false);
            if (nearest instanceof ServerPlayer sp) {
                CommandSourceStack src = sp.createCommandSourceStack()
                        .withPermission(4)
                        .withPosition(Vec3.atCenterOf(worldPosition))
                        .withLevel(serverLevel)
                        .withSuppressedOutput();
                for (String cmd : r.getStartCommandPlayer()) {
                    server.getCommands().performPrefixedCommand(src, cmd);
                }
            }
        }

        setChanged();
    }

    /**
     * 每间隔 craftingTickInterval tick 执行 tick_command_block 命令列表。
     */
    private void executeCraftingTickCommands(ServerLevel serverLevel) {
        MinecraftServer server = serverLevel.getServer();
        if (server == null) return;

        CommandSourceStack src = server.createCommandSourceStack()
                .withLevel(serverLevel)
                .withPosition(Vec3.atCenterOf(worldPosition))
                .withPermission(4)
                .withSuppressedOutput();
        for (String cmd : craftingTickCommands) {
            server.getCommands().performPrefixedCommand(src, cmd);
        }
    }

    /**
     * 计时结束：放置产物，执行完成命令。
     * 展示物品不在此处移除，由 serverTick 在玩家取走产物后移除。
     */
    private void finishCrafting() {
        ItemStack current = itemHandler.getStackInSlot(OUTPUT_SLOT);
        ItemStack result  = craftingResult.copy();

        if (current.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else if (ItemStack.isSameItemSameTags(current, result)
                && current.getCount() + result.getCount() <= current.getMaxStackSize()) {
            current.grow(result.getCount());
        }

        // ── 执行完成命令（finish_command） ────────────────────────────────────
        if (!craftingFinishCommands.isEmpty() && level instanceof ServerLevel sl) {
            MinecraftServer server = sl.getServer();
            if (server != null) {
                CommandSourceStack src = server.createCommandSourceStack()
                        .withLevel(sl)
                        .withPosition(Vec3.atCenterOf(worldPosition))
                        .withPermission(4)
                        .withSuppressedOutput();
                for (String cmd : craftingFinishCommands) {
                    server.getCommands().performPrefixedCommand(src, cmd);
                }
            }
        }

        // 重置合成核心状态（保留 craftingDisplayEntityUUID，由 serverTick 在玩家取走后清理）
        isCrafting             = false;
        progress               = 0;
        maxProgress            = 0;
        craftingResult         = ItemStack.EMPTY;
        craftingTickCommands   = new ArrayList<>();
        craftingTickInterval   = 1;
        craftingFinishCommands = new ArrayList<>();
        itemHandler.setStackInSlot(PREVIEW_SLOT, ItemStack.EMPTY);
        setChanged();
    }

    private Optional<SuperCraftingRecipe> findMatchingRecipe(Level level) {
        ContainerWrapper wrapper = new ContainerWrapper(itemHandler, GRID_SLOTS);
        List<SuperCraftingRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.SUPER_CRAFTING.get());
        for (SuperCraftingRecipe r : recipes) {
            if (r.matches(wrapper, level)) return Optional.of(r);
        }
        return Optional.empty();
    }

    // ===================== NBT 持久化 =====================

    private static ListTag stringsToListTag(List<String> list) {
        ListTag tag = new ListTag();
        for (String s : list) tag.add(StringTag.valueOf(s));
        return tag;
    }

    private static List<String> listTagToStrings(CompoundTag compound, String key) {
        if (!compound.contains(key, Tag.TAG_LIST)) return new ArrayList<>();
        ListTag listTag = compound.getList(key, Tag.TAG_STRING);
        List<String> result = new ArrayList<>(listTag.size());
        for (int i = 0; i < listTag.size(); i++) result.add(listTag.getString(i));
        return result;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putBoolean("isCrafting", isCrafting);
        if (isCrafting && !craftingResult.isEmpty()) {
            tag.put("craftingResult", craftingResult.save(new CompoundTag()));
        }
        if (!craftingTickCommands.isEmpty()) {
            tag.put("craftingTickCommands", stringsToListTag(craftingTickCommands));
        }
        if (craftingTickInterval != 1) {
            tag.putInt("craftingTickInterval", craftingTickInterval);
        }
        if (!craftingFinishCommands.isEmpty()) {
            tag.put("craftingFinishCommands", stringsToListTag(craftingFinishCommands));
        }
        if (craftingDisplayEntityUUID != null) {
            tag.putUUID("craftingDisplayEntity", craftingDisplayEntityUUID);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progress    = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        isCrafting  = tag.getBoolean("isCrafting");
        if (tag.contains("craftingResult")) {
            craftingResult = ItemStack.of(tag.getCompound("craftingResult"));
        }
        craftingTickCommands   = listTagToStrings(tag, "craftingTickCommands");
        craftingTickInterval   = tag.contains("craftingTickInterval") ? tag.getInt("craftingTickInterval") : 1;
        craftingFinishCommands = listTagToStrings(tag, "craftingFinishCommands");
        craftingDisplayEntityUUID = tag.hasUUID("craftingDisplayEntity")
                ? tag.getUUID("craftingDisplayEntity") : null;
    }

    // ===================== Capabilities =====================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    // ===================== MenuProvider =====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.super_crafting.super_crafting_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new SuperCraftingMenu(containerId, playerInv, this);
    }

    public boolean stillValid(Player player) {
        if (this.level != null && this.level.getBlockEntity(this.worldPosition) != this) return false;
        return player.distanceToSqr(
                this.worldPosition.getX() + 0.5D,
                this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    // ===================== Getters =====================

    public ItemStackHandler getItemHandler()  { return itemHandler; }
    public int getProgress()                  { return progress; }
    public int getMaxProgress()               { return maxProgress; }
    public BlockPos getBlockPos()             { return worldPosition; }
    /** 是否正在计时合成中（供方块 onRemove 判断是否丢弃产物）*/
    public boolean isCrafting()               { return isCrafting; }

    // ===================== 内部容器包装类 =====================

    public static class ContainerWrapper implements Container {
        private final ItemStackHandler handler;
        private final int size;

        public ContainerWrapper(ItemStackHandler handler, int size) {
            this.handler = handler;
            this.size    = size;
        }

        @Override public int getContainerSize()                     { return size; }
        @Override public boolean isEmpty() {
            for (int i = 0; i < size; i++) if (!handler.getStackInSlot(i).isEmpty()) return false;
            return true;
        }
        @Override public ItemStack getItem(int slot)                { return handler.getStackInSlot(slot); }
        @Override public ItemStack removeItem(int slot, int amount) { return handler.extractItem(slot, amount, false); }
        @Override public ItemStack removeItemNoUpdate(int slot) {
            ItemStack s = handler.getStackInSlot(slot).copy();
            handler.setStackInSlot(slot, ItemStack.EMPTY);
            return s;
        }
        @Override public void setItem(int slot, ItemStack stack)    { handler.setStackInSlot(slot, stack); }
        @Override public void setChanged()                          {}
        @Override public boolean stillValid(Player player)          { return true; }
        @Override public void clearContent() {
            for (int i = 0; i < size; i++) handler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}
