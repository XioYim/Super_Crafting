# Super Crafting

A powerful 5×5 crafting system for Minecraft modpacks

> 感谢 Claude Code 全程编写了此 Mod 99% 的内容！
>
> Thank you Claude Code for writing 99% of this mod!

### 中文描述请往下滑 ↓ 

---

# 🔧 Super Crafting

Super Crafting is a Forge mod for Minecraft 1.20.1 that expands the vanilla crafting system with a powerful **5×5 crafting table**.

It introduces a new block called the **Super Crafting Table**, designed primarily for **modpacks and custom recipe systems**.

With flexible JSON recipes, timed crafting, command triggers, and full JEI integration, it allows creators to build complex crafting mechanics and automation-like experiences.

---

# ✨ Features

## 🧰 5×5 Crafting Table

- Up to **25 ingredient slots**
- Supports **items, tags, counts, and NBT matching**
- Optional **mirrored recipe support**
- Configurable **crafting duration**
- Built-in **crafting animation**

---

## 📦 Built-in Storage

- The Super Crafting Table also works as a **container block**
- Players can **store items like a chest**

---

## ⚙️ Fully Custom Recipes

Recipes are defined using **JSON files** and support:

- Custom ingredient positions
- Custom ingredient counts
- NBT-based ingredient matching
- Custom output items
- Configurable crafting time

Recipes are located in:


data/super_crafting/recipes/


---

# 🎬 Crafting Effects & Events

Recipes can optionally trigger special effects and commands.

---

## Floating Crafting Display


"summon_item": true


- Spawns a floating display item above the block while crafting
- Visually shows what is being crafted

---

## Start Command


"start_command_player"


Runs command(s) when crafting starts.

Useful for:

- sounds
- titles
- actionbars
- datapack triggers

---

## Tick Commands


"tick_command_block"
"tick_interval"


Runs commands repeatedly during crafting.

Works similarly to a **Repeating Command Block**.

Example uses:

- particle effects
- beam effects
- charging animations

Execution interval example:


"tick_interval": 1


---

## Finish Command


"finish_command"


Runs command(s) immediately when crafting completes.

All commands can contain **multiple entries**.
Permission level **4**


Useful for:

- explosion particles
- completion sounds
- global titles

---

# 🔎 JEI Integration

When **Just Enough Items (JEI)** is installed:

- View full **5×5 recipes**
- See **crafting time**
- Bookmark recipes
- Use **ingredient auto-transfer**

Players can also click the crafting arrow to jump directly to JEI recipe pages.

---

# 📁 Example Recipes Included

The mod includes **10 example recipes** to demonstrate the system.

These examples show:

- basic recipes
- NBT matching
- command triggers
- timed crafting

They can be used as templates for your own recipes.

---

# ⚠️ Important Notes

- This mod is primarily designed for **modpacks**
- The block **does NOT include a default crafting recipe**
- You must define your own recipes

Technical notes:

- Slot numbers range from **0–24**
- NBT matching is **partial**, except **ListTag** which requires **exact matching**
- High-frequency tick commands may affect server performance

---

# ❗ Dependency

This mod requires:

- **Just Enough Items (JEI) 15.0.0.0 or newer**

The game **will not start without JEI installed**.

---

# 🐞 Known Issue

- If the Super Crafting Table is broken during crafting, the consumed ingredients **will NOT be returned**

---

---

# 🔧 超级合成台（Super Crafting）

一个为 **Minecraft 1.20.1 Forge** 设计的模组，在原版工作台基础上扩展合成系统。

模组新增 **5×5 超级合成台**，支持复杂的自定义配方、合成时间、命令触发以及 **JEI 深度集成**，非常适合用于整合包与自定义玩法。

---

# ✨ 核心功能

## 🧰 5×5 合成系统

- 最多 **25 个材料槽**
- 支持 **物品 / 标签 / 数量 / NBT 匹配**
- 支持 **镜像配方**
- 支持 **自定义合成时间**
- 带 **合成动画**

---

## 📦 方块储物

- 超级合成台同时具有 **储物功能**
- 可以像 **箱子一样存放物品**

---

## ⚙️ 自定义 JSON 配方

配方使用 **JSON 文件**定义，支持：

- 自定义材料位置
- 自定义材料数量
- NBT 匹配
- 自定义输出物品
- 自定义合成时间

配方目录：


data/super_crafting/recipes/


---

# 🎬 特殊效果系统

配方可以绑定多种事件效果。

---

## 悬浮展示物品


"summon_item": true


合成开始时会在方块上方生成一个 **悬浮展示物品**。

---

## 开始命令


start_command_player


合成开始时执行命令：

---

## Tick 命令


tick_command_block
tick_interval


合成过程中按 **tick 间隔执行命令**，类似 **重复命令方块**。

常用于：

- 粒子特效
- 激光效果
- 充能动画

---

## 完成命令


finish_command


合成完成时执行命令。

以上命令 **均可填写多条**。
权限OP等级 **4**
---

# 🔎 JEI 集成

安装 JEI 后可以：

- 查看 **完整 5×5 配方**
- 查看 **合成时间**
- 收藏配方
- 一键 **材料转移**

---

# 📁 示例配方

模组内置 **10 个示例配方**，用于展示：

- 基础配方
- NBT 匹配
- 命令触发
- 定时合成

可作为自定义配方的参考模板。

---

# ⚠️ 注意事项

- 本模组主要用于 **整合包辅助**
- 模组方块 **默认没有合成配方**
- 需要自行配置配方

技术说明：

- slot 范围 **0–24**
- NBT 为 **部分匹配**
- ListTag（附魔列表等）为 **精确匹配**
- 高频 tick 命令可能影响服务器性能

---

# ❗依赖

需要安装：

- **JEI 15.0.0.0 及以上版本**

否则游戏 **可能无法启动**

---

# 🐞 已知问题

- 如果玩家在合成过程中破坏方块，已消耗的材料 **不会返还**（虽然我并不觉得他是BUG）

---

### 感谢ChatGpt为我编写以上描述~
