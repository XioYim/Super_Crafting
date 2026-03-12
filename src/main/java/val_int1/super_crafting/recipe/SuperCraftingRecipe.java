package val_int1.super_crafting.recipe;

import com.google.gson.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import val_int1.super_crafting.init.ModRecipeTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 大型工作台配方（仅支持有序配方）
 *
 * JSON 格式（所有新属性均可选）：
 * {
 *   "type": "super_crafting:super_crafting",
 *   "work_time": 200,
 *
 *   "summon_item": false,
 *
 *   "start_command_player": "say 合成开始！",
 *   // 或多条：
 *   "start_command_player": ["say 合成开始！", "give @s minecraft:bread 1"],
 *
 *   "tick_command_block": "particle minecraft:crit ~ ~1 ~ 0.3 0.3 0.3 0.1 2",
 *   // 或多条：
 *   "tick_command_block": ["particle ...", "particle ..."],
 *
 *   "finish_command": "playsound minecraft:entity.player.levelup master @a ~ ~ ~ 1 1",
 *   // 或多条：
 *   "finish_command": ["playsound ...", "say 合成完成！"],
 *
 *   "ingredients": [...],
 *   "result": {"item": "...", "count": 1}
 * }
 *
 * 属性说明：
 *   summon_item          - 合成开始时，在方块上方生成输出物品实体（漂浮/无敌/不可拾取），
 *                          合成完成后自动消除。
 *   start_command_player - 合成开始时，以距方块最近的玩家为 @s 执行命令（OP 权限级别，
 *                          不会修改玩家的实际 OP 状态）。坐标 ~ 以玩家为原点。
 *   tick_command_block   - 合成过程中每 tick 执行一次，以方块中心为坐标原点（类似重复
 *                          命令方块），合成完成后停止。OP 权限级别，无实体上下文（@s 无效）。
 *   finish_command       - 合成完成后立即执行一次，以方块中心为坐标原点，OP 权限级别。
 */
public class SuperCraftingRecipe implements Recipe<Container> {

    public static final int GRID_SIZE = 5;

    private final ResourceLocation id;
    private final int workTime;
    /** 固定 25 个元素，对应 5×5 网格，空位填 EMPTY */
    private final List<CountedIngredient> ingredients;
    private final ItemStack result;

    // ---- 附加属性 ----
    private final boolean summonItem;
    /** 合成开始时以最近玩家为 @s 执行的命令列表（空列表 = 不执行） */
    private final List<String> startCommandPlayer;
    /**
     * 合成过程中每 tickInterval tick 以方块坐标为源执行的命令列表。
     * 执行间隔由 tick_interval 控制（默认 1 = 每 tick）。
     */
    private final List<String> tickCommandBlock;
    /** tick_command_block 的执行间隔（tick），默认 1 */
    private final int tickInterval;
    /** 合成完成时以方块坐标为源执行的命令列表 */
    private final List<String> finishCommand;

    public SuperCraftingRecipe(ResourceLocation id, int workTime,
                               List<CountedIngredient> ingredients, ItemStack result,
                               boolean summonItem,
                               List<String> startCommandPlayer,
                               List<String> tickCommandBlock,
                               int tickInterval,
                               List<String> finishCommand) {
        this.id                  = id;
        this.workTime            = workTime;
        this.ingredients         = ingredients;
        this.result              = result;
        this.summonItem          = summonItem;
        this.startCommandPlayer  = Collections.unmodifiableList(startCommandPlayer);
        this.tickCommandBlock    = Collections.unmodifiableList(tickCommandBlock);
        this.tickInterval        = Math.max(1, tickInterval);
        this.finishCommand       = Collections.unmodifiableList(finishCommand);
    }

    // ===================== 配方匹配 =====================

    @Override
    public boolean matches(Container container, Level level) {
        return matchesShaped(container);
    }

    private boolean matchesShaped(Container container) {
        return matchesPattern(container, false) || matchesPattern(container, true);
    }

    private boolean matchesPattern(Container container, boolean mirrored) {
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                int slotIndex  = x + y * GRID_SIZE;
                int patternIdx = mirrored ? (GRID_SIZE - 1 - x) + y * GRID_SIZE : slotIndex;
                CountedIngredient expected = ingredients.get(patternIdx);
                ItemStack slotStack = container.getItem(slotIndex);
                if (!expected.test(slotStack)) return false;
            }
        }
        return true;
    }

    // ===================== 材料消耗 =====================

    public void consumeIngredients(Container container) {
        if (matchesPattern(container, false)) {
            doConsume(container, false);
        } else if (matchesPattern(container, true)) {
            doConsume(container, true);
        }
    }

    private void doConsume(Container container, boolean mirrored) {
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                int slotIndex  = x + y * GRID_SIZE;
                int patternIdx = mirrored ? (GRID_SIZE - 1 - x) + y * GRID_SIZE : slotIndex;
                CountedIngredient ci = ingredients.get(patternIdx);
                if (!ci.isEmpty()) {
                    container.removeItem(slotIndex, ci.count());
                }
            }
        }
    }

    // ===================== Recipe 接口 =====================

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w >= GRID_SIZE && h >= GRID_SIZE;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) { return result; }

    @Override
    public ResourceLocation getId() { return id; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return val_int1.super_crafting.init.ModRecipeTypes.SUPER_CRAFTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() { return ModRecipeTypes.SUPER_CRAFTING.get(); }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (CountedIngredient ci : ingredients) list.add(ci.ingredient());
        return list;
    }

    // ===================== Getters =====================

    public int getWorkTime()                               { return workTime; }
    public List<CountedIngredient> getCountedIngredients() { return ingredients; }
    public ItemStack getResult()                           { return result; }
    public boolean isShaped()                              { return true; }
    public int getWidth()                                  { return GRID_SIZE; }
    public int getHeight()                                 { return GRID_SIZE; }

    public boolean isSummonItem()               { return summonItem; }
    public List<String> getStartCommandPlayer() { return startCommandPlayer; }
    public List<String> getTickCommandBlock()   { return tickCommandBlock; }
    public int getTickInterval()                { return tickInterval; }
    public List<String> getFinishCommand()      { return finishCommand; }

    // ===================== 序列化器 =====================

    public static class Serializer implements RecipeSerializer<SuperCraftingRecipe> {

        /**
         * 读取命令字段：支持单字符串或字符串数组，空字符串自动过滤。
         * 例：  "finish_command": "say done"
         *   或  "finish_command": ["say done", "give @a diamond 1"]
         */
        private static List<String> readCommands(JsonObject json, String key) {
            if (!json.has(key)) return List.of();
            JsonElement el = json.get(key);
            List<String> result = new ArrayList<>();
            if (el.isJsonArray()) {
                for (JsonElement e : el.getAsJsonArray()) {
                    String s = e.getAsString().trim();
                    if (!s.isEmpty()) result.add(s);
                }
            } else if (el.isJsonPrimitive()) {
                String s = el.getAsString().trim();
                if (!s.isEmpty()) result.add(s);
            }
            return result;
        }

        @Override
        public SuperCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
            int workTime = GsonHelper.getAsInt(json, "work_time", 200);
            boolean summonItem = GsonHelper.getAsBoolean(json, "summon_item", false);

            List<String> startCommandPlayer = readCommands(json, "start_command_player");
            List<String> tickCommandBlock   = readCommands(json, "tick_command_block");
            int tickInterval                = GsonHelper.getAsInt(json, "tick_interval", 1);
            List<String> finishCommand      = readCommands(json, "finish_command");

            JsonArray ingredientsJson = GsonHelper.getAsJsonArray(json, "ingredients");

            List<CountedIngredient> all = new ArrayList<>();
            for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) all.add(CountedIngredient.EMPTY);

            for (JsonElement elem : ingredientsJson) {
                CountedIngredient ci = CountedIngredient.fromJson(elem.getAsJsonObject());
                int slot = ci.slot();
                if (slot < 0 || slot >= GRID_SIZE * GRID_SIZE) {
                    throw new JsonParseException("Invalid slot " + slot + ", must be 0-24.");
                }
                all.set(slot, ci);
            }

            ItemStack result = ShapedRecipe.itemStackFromJson(
                    GsonHelper.getAsJsonObject(json, "result"));
            return new SuperCraftingRecipe(id, workTime, all, result,
                    summonItem, startCommandPlayer, tickCommandBlock, tickInterval, finishCommand);
        }

        // ---- 网络序列化 ----

        private static void writeCommands(FriendlyByteBuf buf, List<String> cmds) {
            buf.writeVarInt(cmds.size());
            for (String s : cmds) buf.writeUtf(s);
        }

        private static List<String> readCommands(FriendlyByteBuf buf) {
            int size = buf.readVarInt();
            List<String> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) list.add(buf.readUtf());
            return list;
        }

        @Override
        public SuperCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int workTime = buf.readVarInt();
            int size = buf.readVarInt();
            List<CountedIngredient> ingredients = new ArrayList<>();
            for (int i = 0; i < size; i++) ingredients.add(CountedIngredient.fromNetwork(buf));
            ItemStack result = buf.readItem();

            boolean summonItem        = buf.readBoolean();
            List<String> startPlayer  = readCommands(buf);
            List<String> tickBlock    = readCommands(buf);
            int tickInterval          = buf.readVarInt();
            List<String> finish       = readCommands(buf);

            return new SuperCraftingRecipe(id, workTime, ingredients, result,
                    summonItem, startPlayer, tickBlock, tickInterval, finish);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, SuperCraftingRecipe recipe) {
            buf.writeVarInt(recipe.workTime);
            buf.writeVarInt(recipe.ingredients.size());
            for (CountedIngredient ci : recipe.ingredients) ci.toNetwork(buf);
            buf.writeItem(recipe.result);

            buf.writeBoolean(recipe.summonItem);
            writeCommands(buf, recipe.startCommandPlayer);
            writeCommands(buf, recipe.tickCommandBlock);
            buf.writeVarInt(recipe.tickInterval);
            writeCommands(buf, recipe.finishCommand);
        }
    }
}
