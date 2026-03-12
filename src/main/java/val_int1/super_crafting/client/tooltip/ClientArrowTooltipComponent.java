package val_int1.super_crafting.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/**
 * 渲染 ArrowTooltipData，布局（从上到下）：
 *   行1：[物品图标 16×16]  物品名称
 *   行2..n：物品详情（附魔、属性等，可为空）
 *   末行：时间文字（黄色）
 */
public class ClientArrowTooltipComponent implements ClientTooltipComponent {

    private static final int ITEM_SIZE        = 16;
    private static final int ICON_GAP         = 3;
    private static final int FONT_HEIGHT      = 9;   // Minecraft 默认字体行高
    private static final int ITEM_ROW_HEIGHT  = ITEM_SIZE + 2;   // 18
    private static final int DETAIL_ROW_HEIGHT = FONT_HEIGHT + 1; // 10

    private final ArrowTooltipData data;

    public ClientArrowTooltipComponent(ArrowTooltipData data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        // 图标行 + 详情行 × n + 时间行
        return ITEM_ROW_HEIGHT
                + data.detailLines().size() * DETAIL_ROW_HEIGHT
                + FONT_HEIGHT + 2;
    }

    @Override
    public int getWidth(Font font) {
        int nameW = ITEM_SIZE + ICON_GAP + font.width(data.stack().getHoverName());
        int timeW = font.width(data.timeLine());
        int detailW = data.detailLines().stream()
                .mapToInt(font::width)
                .max().orElse(0);
        return Math.max(nameW, Math.max(timeW, detailW));
    }

    @Override
    public void renderText(Font font, int x, int y,
                           Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {

        // ── 物品名称（图标行右侧，垂直居中）──
        int nameX = x + ITEM_SIZE + ICON_GAP;
        int nameY = y + (ITEM_SIZE - FONT_HEIGHT) / 2;
        font.drawInBatch(
                data.stack().getHoverName().getVisualOrderText(),
                nameX, nameY, 0xFFFFFF, true,
                matrix, bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT
        );

        // ── 详情行（附魔、属性等）──
        int detailBaseY = y + ITEM_ROW_HEIGHT;
        for (int i = 0; i < data.detailLines().size(); i++) {
            Component line = data.detailLines().get(i);
            font.drawInBatch(
                    line.getVisualOrderText(),
                    x, detailBaseY + i * DETAIL_ROW_HEIGHT,
                    0xFFFFFF, true,
                    matrix, bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT
            );
        }

        // ── 时间行（黄色，最底部）──
        int timeY = detailBaseY + data.detailLines().size() * DETAIL_ROW_HEIGHT;
        font.drawInBatch(
                data.timeLine().getVisualOrderText(),
                x, timeY, 0xFFFFFF, true,
                matrix, bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT
        );
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        guiGraphics.renderItem(data.stack(), x, y);
        guiGraphics.renderItemDecorations(font, data.stack(), x, y);
    }
}
