# FoliaShop 开发进度

## 项目概述
一个专为 Folia 服务端设计的系统商店和扭蛋插件，支持原版物品和 CraftEngine 自定义物品。

## 核心功能

### 系统商店
- [x] 支持购买和出售物品
- [x] 商品分类管理
- [x] 库存系统（支持无限库存）
- [x] 支持 CraftEngine 自定义物品（CE物品）
- [x] 交易记录（玩家可查询最近20次）
- [x] 每日购买限额（每个物品独立配置）

### 扭蛋系统
- [x] 多扭蛋机支持
- [x] 概率配置
- [x] 抽奖动画（单抽 + 10连抽）
- [x] ~~多段硬保底系统~~ → **软保底机制**（线性概率增长）
- [x] 稀有奖品广播（MiniMessage格式，支持{draws}变量）
- [x] 奖品预览（按概率排序，权限控制显示）
- [x] 抽奖记录
- [x] 保底计数数据库迁移（旧表→新表）

### 经济系统
- [x] 支持 XConomy（硬依赖）
- [x] 支持 CraftEngine（硬依赖）

### 数据库
- [x] 支持 MySQL 和 H2
- [x] 异步数据库操作
- [x] 连接池管理（HikariCP）
- [x] 数据库迁移（自动添加 daily_limit 列）
- [x] 保底表结构迁移（多段→单段，保留数据）

## 最近更新

### 2025-02-09
1. **软保底机制** - 替换多段硬保底为单段软保底
   - 保底目标概率从 `start` 计数开始线性增长
   - 到 `max` 计数达到100%（硬保底）
   - 配置简化：`pity.enabled`, `pity.start`, `pity.max`, `pity.target-max-probability`
2. **数据库表迁移** - 自动检测旧表结构并迁移数据
   - 检测 `rule_hash` 列判断是否为旧表
   - 取每个玩家的最大计数作为新计数
   - 保留玩家保底进度
3. **抽奖广播优化** - 修复十连抽显示次数错误
   - 在十连抽计算时缓存每个奖品的显示次数
   - 避免领取时查询数据库导致的竞态条件
   - 新增 `{draws}` 变量显示距离上次抽到的次数
4. **保底计数更新时机** - 抽奖计算后立即更新数据库
   - 确保连续快速抽奖时的数据一致性
5. **管理员统计命令** - `/foliashop stats [玩家|-] <machineId> <rewardId>`
   - 查询玩家抽中某奖品的平均花费次数
   - 支持查询所有玩家统计
6. **扭蛋机图标优化** - 显示 `description` 配置作为 Lore
7. **权限检查优化** - 绑定方块交互权限检查完善

## 最近修复

### 2025-02-06
1. **每日限额GUI显示** - 商店界面显示物品每日限额信息
2. **扭蛋机启用配置** - 支持 `enabled` 字段单独禁用/启用扭蛋机
3. **分页负数修复** - 修复管理界面、扭蛋预览、分类界面的无限上一页问题
4. **异步加载优化** - 修复 Folia watchdog 超时问题，改回纯异步加载
5. **SQL语法兼容** - 修复 H2 数据库 `MERGE INTO` / `ON DUPLICATE KEY UPDATE` 兼容问题
6. **调试日志清理** - 移除 ShopItemsGUI 和 ItemUtil 的调试日志

### 2025-02-04
1. **数据库迁移** - 自动检测并添加缺失的 `daily_limit` 列
2. **MiniMessage广播** - 修复稀有奖品广播的MiniMessage格式解析
3. **交易记录按钮** - 在商店分类界面添加交易记录入口（动态居中布局）
4. **物品哈希** - 使用 `ItemUtil.getItemKey()` 统一获取物品ID
5. **代码重构** - 提取 `getCEItemKey` 到 ItemUtil，使用直接API调用

### 2025-02-02
1. **10连抽保底计数** - 修复批量更新保底计数器问题
2. **PityRule哈希** - 使用 `Double.toString()` 确保哈希唯一性
3. **出售记录** - 修复交易记录存储的 itemId 不准确问题
4. **再抽一次保底** - 修复10连抽结果界面的"再抽一次"没有使用保底
5. **调试日志清理** - 删除所有 [GachaDebug] 日志
6. **重复代码提取** - GachaMachineGUI 支付流程提取为通用方法

## 界面布局

### 商店分类界面（动态居中）
底部按钮根据可用功能动态居中显示：
- 上一页（条件显示）
- 交易记录
- 关闭
- 页码指示器
- 出售物品（条件显示）
- 下一页（条件显示）

按钮数量：4-6个，根据分页和出售系统启用状态自动调整

## 技术栈
- Java 21
- Folia API 1.21.11
- CraftEngine Bukkit 0.0.67
- HikariCP 6.2.1
- H2 / MySQL

## 最近更新

### 2025-02-08
1. **NBT组件系统** - 为商店物品、扭蛋奖励、扭蛋机ICON添加NBT组件支持
   - 支持附魔、自定义名称、Lore、自定义数据等
   - 配置格式: `path+value`，如 `minecraft:enchantments+{'sharpness':5}`
   - 数据库新增 `components` 列存储JSON格式数据
2. **NBTPathUtils工具类** - 从player_scan项目引入，支持复杂NBT路径解析
3. **Lore显示优化** - GUI中使用 `addLore` 替代 `setLore`，保留物品原有Lore
4. **NBT-API依赖** - 添加 `item-nbt-api-plugin:2.15.5` 依赖
5. **代码问题修复**
   - 修复 ResultSet 未关闭导致的资源泄漏
   - 修复 H2 MERGE INTO 语法不正确问题
   - 修复异步回调中未检查玩家在线状态
   - 修复 GUIManager 使用普通 HashMap 的并发安全问题
   - 重构 updatePityCounters 复用 batchUpdatePityCounters 逻辑
   - 优化数据库查询性能（批量查询替代 N+1）
   - 修复 last_pity_reward 被错误覆盖为 null 的问题

## 待办事项
- [ ] 考虑使用 Component API 替代弃用的 String 标题方法
- [ ] CustomModelData 新 API 待稳定后更新
- [ ] 考虑添加潜行检查（shift+右键允许放置方块）
- [ ] 考虑添加手持物品检查（手持方块时允许放置）

## 构建命令
```bash
./gradlew shadowJar
```

输出：`build/libs/folia_shop-1.0.0.jar`
