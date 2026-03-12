package val_int1.super_crafting.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import val_int1.super_crafting.SuperCrafting;
import val_int1.super_crafting.blockentity.SuperCraftingTableBE;
import val_int1.super_crafting.compat.jei.JEIPlugin;
import val_int1.super_crafting.menu.SuperCraftingMenu;
import val_int1.super_crafting.network.SuperCraftStartPacket;
import val_int1.super_crafting.network.NetworkHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SuperCraftingScreen extends AbstractContainerScreen<SuperCraftingMenu> {

    /** 主背景纹理（含玩家背包区域） */
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SuperCrafting.MOD_ID, "textures/gui/big_crafting_table.png");

    /**
     * 箭头四态纹理：
     *   arrow_normal.png   - 18×18，灰色，无配方可用
     *   arrow_ready.png    - 18×18，金色，配方就绪等待点击
     *   arrow_blocked.png  - 18×18，红色/锁定，输出槽有物品未取出（需先取走才能继续合成）
     *   arrow_crafting.png - 18×216，合成动画帧集（12 帧竖向排列，每帧 18×18）
     */
    private static final ResourceLocation ARROW_NORMAL   =
            new ResourceLocation(SuperCrafting.MOD_ID, "textures/gui/arrow_normal.png");
    private static final ResourceLocation ARROW_READY    =
            new ResourceLocation(SuperCrafting.MOD_ID, "textures/gui/arrow_ready.png");
    private static final ResourceLocation ARROW_BLOCKED  =
            new ResourceLocation(SuperCrafting.MOD_ID, "textures/gui/arrow_blocked.png");
    private static final ResourceLocation ARROW_CRAFTING =
            new ResourceLocation(SuperCrafting.MOD_ID, "textures/gui/arrow_crafting.png");

    /** 合成动画：帧数与每帧持续 tick 数 */
    private static final int ARROW_FRAME_COUNT = 12;
    private static final int ARROW_FRAME_TICKS = 3;

    // 大小
    public static final int GUI_WIDTH  = 200;
    public static final int GUI_HEIGHT = 200;

    public SuperCraftingScreen(SuperCraftingMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = SuperCraftingMenu.PLAYER_INV_Y - 10;
    }

    // ===================== 辅助：判断输出槽是否有物品 =====================

    /** 输出槽（slot 25）当前是否有物品未取出 */
    private boolean isOutputOccupied() {
        return !menu.slots.get(SuperCraftingMenu.OUTPUT_SLOT).getItem().isEmpty();
    }

    // ===================== 背景渲染 =====================

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int x = this.leftPos;
        int y = this.topPos;

        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        renderArrow(graphics, x, y);
    }

    /**
     * 渲染箭头按钮，四种状态优先级从高到低：
     *   1. 合成中          → 动画箭头（arrow_crafting.png）
     *   2. 输出槽有物品    → 锁定箭头（arrow_blocked.png）—— 需先取走产物
     *   3. 配方就绪        → 金色箭头（arrow_ready.png）
     *   4. 无配方          → 灰色箭头（arrow_normal.png）
     */
    private void renderArrow(GuiGraphics graphics, int x, int y) {
        int ax = x + SuperCraftingMenu.ARROW_X;
        int ay = y + SuperCraftingMenu.ARROW_Y;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int maxProgress = menu.getMaxProgress();
        ItemStack preview = menu.slots.get(SuperCraftingMenu.PREVIEW_SLOT_IDX).getItem();

        if (maxProgress > 0) {
            // 合成中
            long gameTime = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getGameTime() : 0L;
            int frame = (int) ((gameTime / ARROW_FRAME_TICKS) % ARROW_FRAME_COUNT);
            blitAnimatedFrame(graphics, ARROW_CRAFTING, ax, ay, frame);
        } else if (isOutputOccupied()) {
            // 输出槽有物品未取出，锁定
            blitTexture(graphics, ARROW_BLOCKED, ax, ay);
        } else if (!preview.isEmpty()) {
            // 就绪
            blitTexture(graphics, ARROW_READY, ax, ay);
        } else {
            // 无配方
            blitTexture(graphics, ARROW_NORMAL, ax, ay);
        }
    }

    private static void blitAnimatedFrame(GuiGraphics graphics, ResourceLocation texture,
                                          int x, int y, int frame) {
        int vOffset   = frame * 18;
        int texHeight = ARROW_FRAME_COUNT * 18;
        graphics.blit(texture, x, y, 0, vOffset, 18, 18, 18, texHeight);
    }

    private static void blitTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, 0, 0, 18, 18, 18, 18);
    }

    // ===================== 主渲染 =====================

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    // ===================== 标签 & 箭头提示 =====================

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title,
                this.titleLabelX, this.titleLabelY, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        if (isHovering(SuperCraftingMenu.ARROW_X, SuperCraftingMenu.ARROW_Y,
                SuperCraftingMenu.ARROW_W, SuperCraftingMenu.ARROW_H, mouseX, mouseY)) {

            int progress    = menu.getProgress();
            int maxProgress = menu.getMaxProgress();
            ItemStack preview = menu.slots.get(SuperCraftingMenu.PREVIEW_SLOT_IDX).getItem();

            int rx = mouseX - this.leftPos;
            int ry = mouseY - this.topPos;

            if (maxProgress > 0) {
                // 合成中
                int remainSec = (int) Math.ceil((maxProgress - progress) / 20.0);
                List<Component> tip = buildArrowTooltip(preview,
                        Component.translatable("tooltip.super_crafting.time_remaining", remainSec)
                                .withStyle(ChatFormatting.YELLOW));
                graphics.renderTooltip(this.font, tip, Optional.empty(), rx, ry);

            } else if (isOutputOccupied()) {
                // 输出槽有物品：提示玩家先取走
                graphics.renderTooltip(this.font,
                        Component.translatable("tooltip.super_crafting.output_occupied")
                                .withStyle(ChatFormatting.RED),
                        rx, ry);

            } else if (!preview.isEmpty()) {
                // 就绪
                int workSec = (int) Math.ceil(SuperCraftingTableBE.getPreviewWorkTime(preview) / 20.0);
                List<Component> tip = buildArrowTooltip(preview,
                        Component.translatable("tooltip.super_crafting.work_time_cost", workSec)
                                .withStyle(ChatFormatting.YELLOW));
                graphics.renderTooltip(this.font, tip, Optional.empty(), rx, ry);

            } else {
                // 无配方
                graphics.renderTooltip(this.font,
                        Component.translatable("tooltip.super_crafting.view_in_jei"),
                        rx, ry);
            }
        }
    }

    // ===================== 鼠标点击 =====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int ax = this.leftPos + SuperCraftingMenu.ARROW_X;
            int ay = this.topPos  + SuperCraftingMenu.ARROW_Y;

            if (mouseX >= ax && mouseX < ax + SuperCraftingMenu.ARROW_W
                    && mouseY >= ay && mouseY < ay + SuperCraftingMenu.ARROW_H) {

                if (menu.getMaxProgress() > 0) return true; // 合成中，忽略

                if (isOutputOccupied()) return true; // 输出槽未清空，忽略

                ItemStack preview = menu.slots.get(SuperCraftingMenu.PREVIEW_SLOT_IDX).getItem();
                if (!preview.isEmpty()) {
                    // 金色箭头：发送合成请求
                    NetworkHandler.CHANNEL.sendToServer(
                            new SuperCraftStartPacket(menu.getBlockEntity().getBlockPos()));
                } else {
                    // 灰色箭头：打开 JEI
                    openJeiCategory();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 构建箭头按钮 tooltip 行：
     *   行1：物品名称，行2..n：附魔，末行：timeLine（黄色时间提示）。
     */
    private static List<Component> buildArrowTooltip(ItemStack stack, Component timeLine) {
        List<Component> list = new ArrayList<>();

        if (!stack.isEmpty()) {
            list.add(stack.getHoverName());

            CompoundTag nbt = stack.getTag();
            if (nbt == null || nbt.getInt("HideFlags") == 0) {
                EnchantmentHelper.getEnchantments(stack)
                        .forEach((ench, level) -> list.add(ench.getFullname(level)));

                if (nbt != null && nbt.contains("StoredEnchantments", Tag.TAG_LIST)) {
                    ListTag stored = nbt.getList("StoredEnchantments", Tag.TAG_COMPOUND);
                    for (int i = 0; i < stored.size(); i++) {
                        CompoundTag enchTag = stored.getCompound(i);
                        BuiltInRegistries.ENCHANTMENT
                                .getOptional(ResourceLocation.tryParse(enchTag.getString("id")))
                                .ifPresent(ench -> list.add(ench.getFullname(enchTag.getShort("lvl"))));
                    }
                }
            }
        }

        list.add(timeLine);
        return list;
    }

    private void openJeiCategory() {
        if (ModList.get().isLoaded("jei")) {
            JEIPlugin.openSuperCraftingCategory();
        }
    }
}
