# FoliaShop - 高版本Folia/Paper系统商店与扭蛋插件

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Folia](https://img.shields.io/badge/Folia-1.21+-blue.svg)](https://github.com/PaperMC/Folia)

一个专为 [Folia/Paper](https://github.com/PaperMC/Folia) 服务端设计的系统商店和扭蛋插件，支持原版物品和 [CraftEngine](https://github.com/Momirealms/CraftEngine) 自定义物品。

## ✨ 功能特性

### 系统商店
- 🛒 支持购买和出售物品
- 📂 商品分类管理
- 📦 库存系统（支持无限库存）
- 🔧 支持 CraftEngine 自定义物品（CE物品）
- 📜 交易记录（玩家可查询最近20次）
- ⏰ **每日购买限额**（每个物品独立配置）
- 🔒 **玩家终身限购**（每个物品独立配置）
- 💎 **PlayerPoints 点券支付**（支持金币+点券混合支付）
- 🏷️ **NBT 组件支持**（附魔、自定义名称、Lore、自定义数据）
- ⚡ **纯命令商品**（给予权限、执行命令，可不给予物品）

### 扭蛋系统
- 🎰 多扭蛋机支持
- 🎯 概率配置
- 🔧 支持 CraftEngine 自定义物品（CE物品）
- ✨ 抽奖动画（单抽 + 10连抽）
- 🛡️ **软保底机制**（线性概率增长）
- 📢 稀有奖品广播（MiniMessage格式）
- 👁️ 奖品预览（按概率排序）
- 📊 抽奖记录
- 🧊 **方块绑定**（将扭蛋机绑定到方块，右键交互）
- 🎨 **展示实体**（悬浮物品动画、粒子效果）

### 数据库功能
- 💾 **数据库备份/恢复**（支持 H2 和 MySQL 互导）
- 🔀 支持 H2（本地）和 MySQL（跨服）

## 📋 依赖要求

### 必需插件（硬依赖）
| 插件 | 版本 | 用途 |
|------|------|------|
| Folia | 1.21+ | 服务端核心 |
| XConomy | 2.25+ | 经济系统 |
| CraftEngine | 0.0.67+ | 自定义物品系统 |

### 可选插件（软依赖）
| 插件 | 版本 | 用途 |
|------|------|------|
| PlayerPoints | 3.2+ | 点券系统 |

## 🚀 安装

1. 下载最新版本的 `folia_shop-1.0.3.jar`
2. 将 JAR 文件放入服务器的 `plugins` 文件夹
3. 重启服务器或加载插件
4. 编辑 `plugins/FoliaShop/config.yml` 配置数据库连接
5. 执行 `/foliashop reload` 重载配置

## 📖 命令

### 玩家命令
| 命令 | 描述 | 权限 |
|------|------|------|
| `/foliashop` | 打开主菜单 | `foliashop.use` |
| `/foliashop shop` | 打开系统商店 | `foliashop.shop.use` |
| `/foliashop gacha` | 打开扭蛋界面 | `foliashop.gacha.use` |
| `/shop [分类]` | 直接打开商店（可指定分类） | `foliashop.shop.use` |
| `/gacha [扭蛋机ID]` | 直接打开扭蛋（可指定机器） | `foliashop.gacha.use` |

### 管理员命令
| 命令 | 描述 | 权限 |
|------|------|------|
| `/foliashop reload` | 重载配置文件 | `foliashop.admin` |
| `/foliashop admin` | 打开商店管理界面 | `foliashop.admin` |
| `/foliashop reset` | 清空数据库并从配置重新加载 | `foliashop.admin` |
| `/foliashop clean <天数>` | 清理旧数据（5/10/30天） | `foliashop.admin` |
| `/foliashop bindblock <machineId>` | 将看向的方块绑定到扭蛋机 | `foliashop.admin` |
| `/foliashop unbindblock` | 解绑看向的方块 | `foliashop.admin` |
| `/foliashop listblocks [machineId]` | 列出方块绑定 | `foliashop.admin` |
| `/foliashop export [full\|config\|state]` | 导出数据库备份 | `foliashop.admin` |
| `/foliashop import <文件名> [replace\|merge]` | 从备份恢复数据库 | `foliashop.admin` |
| `/foliashop stats [-\|玩家名] <machineId> <rewardId>` | 查询奖品统计 | `foliashop.admin` |
| `/foliashop exportshop` | 导出商店数据到 YAML | `foliashop.admin` |

## 🔐 权限节点

### 玩家权限
```yaml
foliashop.use:          # 使用基础命令（打开主菜单）
  default: true

foliashop.shop.use:     # 使用商店功能
  default: true

foliashop.shop.sell:    # 出售物品
  default: true

foliashop.gacha.use:    # 使用扭蛋功能
  default: true
```

### 管理员权限
```yaml
foliashop.admin:        # 管理员权限（编辑商店、重载配置等）
  default: op
```

### 权限继承关系
```
foliashop.admin
  └─ 包含所有其他权限

foliashop.use
  └─ foliashop.shop.use
  └─ foliashop.gacha.use
```

## 👑 管理员功能

### 商店物品管理
管理员可以通过 `/foliashop admin` 命令打开商店管理界面，对单个物品进行以下操作：

| 功能 | 说明 |
|------|------|
| 📦 库存调整 | +1, +10, +64, -1, -10, 设为无限 |
| 🗑️ **清空库存** | 将库存设为 0 |
| 🔄 **从配置文件重置** | 从 `shop.yml` 重新加载该物品的所有配置 |
| ❌ **删除物品** | 从数据库中永久删除该商店物品（带确认对话框） |

### 数据清理
管理员可以使用 `/foliashop clean <天数>` 命令清理旧数据：

```bash
/foliashop clean 5    # 清理5天以前的数据
/foliashop clean 10   # 清理10天以前的数据
/foliashop clean 30   # 清理30天以前的数据
```

**清理的数据类型：**
- 交易记录
- 抽奖记录
- 过期购买计数

### 数据库备份/恢复
管理员可以使用备份命令导出和恢复数据库：

```bash
# 导出备份
/foliashop export           # 导出配置+状态（推荐）
/foliashop export config    # 只导出配置（商品、方块绑定）
/foliashop export state     # 导出配置+玩家状态（限购、保底）
/foliashop export full      # 导出所有数据（包含日志）

# 恢复备份
/foliashop import backup_20250215_143022      # 清空现有数据后导入
/foliashop import backup_20250215_143022 merge # 合并导入，跳过冲突

# 查看可用备份
/foliashop import  # 不填文件名会列出所有备份
```

**备份文件位置：** `plugins/FoliaShop/backups/`

**跨数据库迁移：** 支持从 H2 导出，导入到 MySQL（或反过来）

### 扭蛋方块绑定
管理员可以将扭蛋机绑定到方块，玩家右键点击方块即可打开扭蛋界面：

```bash
/foliashop bindblock normal      # 将看向的方块绑定到 normal 扭蛋机
/foliashop unbindblock           # 解绑看向的方块
/foliashop listblocks            # 列出所有方块绑定
/foliashop listblocks normal     # 列出 normal 扭蛋机的方块绑定
```

绑定方块后会自动生成展示实体（悬浮的物品图标）。

## ⚙️ 配置说明

### 配置文件结构

插件使用分离的配置文件结构，便于管理：

```
plugins/FoliaShop/
├── config.yml          # 主配置：数据库、经济、GUI、消息
├── shop.yml            # 商店配置：商品、分类、回收设置
├── gacha.yml           # 扭蛋配置：扭蛋机、奖品、保底规则
└── backups/            # 自动创建的备份目录
```

**各文件作用：**
- `config.yml` - 数据库连接、经济系统设置、GUI界面配置、消息文本
- `shop.yml` - 商品定义、分类设置、系统回收物品列表
- `gacha.yml` - 扭蛋机定义、奖品池、保底配置、展示实体效果

### 基础配置 (config.yml)
```yaml
# 数据库配置
database:
  type: h2  # 可选: h2, mysql
  mysql:
    host: localhost
    port: 3306
    database: foliashop
    username: root
    password: password
  h2:
    filename: foliashop

# 经济系统设置
economy:
  enabled: true
  currency-name: "金币"
  currency-format: "{amount} {currency}"

# GUI界面设置
gui:
  titles:
    main-menu: "<dark_gray>主菜单"
    shop: "<green>系统商店"
    gacha: "<gold>扭蛋中心"

# 消息设置（支持 MiniMessage 格式）
messages:
  prefix: "<gold>[系统商店] <reset>"
  purchase-success: "<green>✔ 成功购买 <white>{item} <yellow>x{amount}"
  # ... 更多消息配置
```

**注意：** 商店商品配置在 `shop.yml`，扭蛋机配置在 `gacha.yml`，不在 `config.yml` 中。

### 商品配置示例
```yaml
shop:
  items:
    # 基础商品
    diamond_shop:
      item: "minecraft:diamond"
      buy-price: 100.0
      sell-price: 50.0
      stock: -1        # -1表示无限库存
      daily-limit: 10  # 每日购买限额
      category: "misc"
      slot: 11

    # CE自定义物品
    magic_sword_shop:
      item: "craftengine:magic_sword"
      buy-price: 1000.0
      sell-price: 0.0
      stock: 10
      category: "tools"
      slot: 13

    # 带NBT组件的商品（附魔、自定义名称等）
    enchanted_sword:
      item: "minecraft:iron_sword"
      buy-price: 500.0
      buy-points: 100  # 需要100点券
      sell-price: 0
      daily-limit: 1
      player-limit: 1  # 每个玩家终身限购1次
      components:
        - "minecraft:enchantments+{'minecraft:sharpness':5,'minecraft:unbreaking':3}"
        - "minecraft:custom_name+\"§6传说铁剑\""
        - "minecraft:lore+[\"§7商店限定版\"]"
        - "minecraft:unbreakable+{}"

    # 纯点券商品
    vip_token:
      item: "minecraft:emerald"
      buy-price: 0
      buy-points: 500
      components:
        - "minecraft:custom_name+\"§aVIP令牌\""

    # 纯命令商品（给予权限，不给予物品）
    vip_rank:
      item: "minecraft:diamond_block"
      buy-price: 10000
      buy-points: 1000
      stock: 10              # 全服限量10个
      player-limit: 1        # 每人限购1次
      give-item: false       # 不给予物品
      commands:
        - "lp user {player} parent addtemp vip 30d"
      conditions:
        - "!permission:group.vip"  # 已有VIP不能购买
```

### 扭蛋机配置示例
```yaml
gacha:
  machines:
    normal:
      name: "普通扭蛋机"
      cost: 100.0
      animation-duration: 3      # 单抽动画时长（秒）
      animation-duration-ten: 9  # 10连抽动画时长（秒）
      broadcast-rare: true
      broadcast-threshold: 0.05  # 概率低于此值时广播
      # 软保底配置（可选）
      pity:
        enabled: true
        start: 70              # 70抽后开始增加保底概率
        max: 90                # 90抽必出保底目标（硬保底）
        target-max-probability: 0.05  # 概率≤5%的奖品为保底目标
      # 展示实体配置（可选）
      display-entity:
        enabled: true
        scale: 1.2
        floating-animation: true
        particle-effect:
          type: STAR_RING      # 粒子效果类型
      rewards:
        - id: "common_coal"
          item: "minecraft:coal"
          amount: 16
          probability: 0.10
          display-name: "煤炭"
        - id: "rare_diamond"
          item: "minecraft:diamond"
          amount: 2
          probability: 0.03
          display-name: "钻石礼包"
          broadcast: true
        - id: "epic_sword"
          item: "minecraft:diamond_sword"
          amount: 1
          probability: 0.01
          display-name: "传说之剑"
          broadcast: true
          components:
            - "minecraft:enchantments+{'minecraft:sharpness':5}"
            - "minecraft:custom_name+\"§6传说之剑\""
```

**软保底说明**：
- `start`: 达到此抽数后，保底目标奖品的概率开始线性增长
- `max`: 达到此抽数时，保底目标概率达到100%（必出）
- 触发保底后计数器重置为0

## 🛠️ 构建

```bash
./gradlew shadowJar
```

构建后的 JAR 文件位于 `build/libs/folia_shop-1.0.3.jar`

## 🏗️ 项目结构

```
folia_shop/
├── build.gradle.kts              # Gradle构建配置
├── settings.gradle.kts
├── gradlew
├── gradle/wrapper/
├── src/
│   └── main/
│       ├── java/dev/user/shop/
│       │   ├── FoliaShopPlugin.java
│       │   ├── command/          # 命令处理
│       │   ├── config/           # 配置管理
│       │   ├── database/         # 数据库
│       │   ├── economy/          # 经济系统 (XConomy API)
│       │   ├── gacha/            # 扭蛋系统
│       │   ├── gui/              # GUI界面
│       │   ├── listener/         # 事件监听
│       │   ├── shop/             # 商店系统
│       │   └── util/             # 工具类 (CraftEngine API)
│       └── resources/
│           ├── plugin.yml
│           └── config.yml
└── README.md
```

## 📦 依赖

| 依赖 | 版本 | 来源 |
|------|------|------|
| Folia-API | 1.21.11-R0.1-SNAPSHOT | PaperMC |
| XConomyAPI | 2.25.1 | JitPack |
| CraftEngine | 0.0.67 | Momirealms |
| PlayerPoints | 3.2+ | GitHub |
| HikariCP | 6.2.1 | Maven Central |
| H2 | 2.3.232 | Maven Central |
| MySQL Connector | 9.2.0 | Maven Central |

## 📄 许可证

MIT License

---

**注意**：本插件专为 Folia/Paper 服务端设计。
