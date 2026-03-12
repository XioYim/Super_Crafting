package val_int1.super_crafting.client.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 箭头按钮 tooltip 数据：
 *   stack       — 要展示的物品（渲染图标 + 悬浮名称）
 *   detailLines — 物品的附加信息行（附魔、属性等），HideFlags 非零时传空列表
 *   timeLine    — 底部时间行（黄色），如 "剩余合成时间：15s"
 */
public record ArrowTooltipData(
        ItemStack stack,
        List<Component> detailLines,
        Component timeLine
) implements TooltipComponent {}
