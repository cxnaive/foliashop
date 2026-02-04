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

### 扭蛋系统
- 🎰 多扭蛋机支持
- 🎯 概率配置
- 🔧 支持 CraftEngine 自定义物品（CE物品）
- ✨ 抽奖动画（单抽 + 10连抽）
- 🛡️ **多段保底系统**（支持多档保底规则）
- 📢 稀有奖品广播
- 👁️ 奖品预览
- 📊 抽奖记录

## 📋 依赖要求

### 必需插件（硬依赖）
| 插件 | 版本 | 用途 |
|------|------|------|
| Folia | 1.21+ | 服务端核心 |
| XConomy | 2.25+ | 经济系统 |
| CraftEngine | 0.0.67+ | 自定义物品系统 |

## 🚀 安装

1. 下载最新版本的 `folia_shop-1.0.0.jar`
2. 将 JAR 文件放入服务器的 `plugins` 文件夹
3. 重启服务器或加载插件
4. 编辑 `plugins/FoliaShop/config.yml` 配置商店和扭蛋
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
| 🔄 **从配置文件重置** | 从 `config.yml` 重新加载该物品的所有配置 |
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

## ⚙️ 配置说明

### 基础配置
```yaml
# 数据库配置
database:
  type: H2  # 可选: H2, MYSQL
  host: localhost
  port: 3306
  name: foliashop
  user: root
  password: password

# 经济设置
currency:
  name: "金币"
  symbol: "§e💰"

# 商店设置
shop:
  sell-system:
    enabled: true
    mode: ALL  # SHOP_ONLY, CONFIG_ONLY, ALL
```

### 商品配置示例
```yaml
shop:
  items:
    diamond_shop:
      item: "minecraft:diamond"  # 原版物品
      buy-price: 100.0
      sell-price: 50.0
      stock: -1        # -1表示无限库存
      daily-limit: 10  # 每日购买限额（0表示无限制）
      category: "misc"
      slot: 11
    magic_sword_shop:
      item: "craftengine:magic_sword"  # CE物品
      buy-price: 1000.0
      sell-price: 0.0  # 0表示不可出售
      stock: 10
      daily-limit: 0   # 无购买限制
      category: "tools"
      slot: 13
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
      # 多段保底配置（可选）
      pity-rules:
        - count: 10              # 10抽保底
          max-probability: 0.1   # 概率≤0.1的奖品池
        - count: 50              # 50抽保底
          max-probability: 0.03  # 概率≤0.03的奖品池
        - count: 90              # 90抽保底
          max-probability: 0.01  # 概率≤0.01的奖品池
      rewards:
        - id: "reward_1"
          item: "minecraft:diamond"
          amount: 2
          probability: 0.08
          display-name: "钻石礼包"
          broadcast: true
```

**多段保底说明**：
- 每个规则使用 `count_maxProbability` 作为唯一标识（如 `10_0.10`）
- 各规则独立计数，互不影响
- 优先触发高档次保底（按 count 降序检查）
- 触发保底后该规则计数重置为0，其他规则继续累积

## 🛠️ 构建

```bash
./gradlew shadowJar
```

构建后的 JAR 文件位于 `build/libs/folia_shop-1.0.0.jar`

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
| HikariCP | 6.2.1 | Maven Central |
| H2 | 2.3.232 | Maven Central |
| MySQL Connector | 9.2.0 | Maven Central |

## 📄 许可证

MIT License

---

**注意**：本插件专为 Folia/Paper 服务端设计。
