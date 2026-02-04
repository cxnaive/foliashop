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
- [x] 多段保底系统（支持多档保底规则）
- [x] 稀有奖品广播（MiniMessage格式）
- [x] 奖品预览
- [x] 抽奖记录

### 经济系统
- [x] 支持 XConomy（硬依赖）
- [x] 支持 CraftEngine（硬依赖）

### 数据库
- [x] 支持 MySQL 和 H2
- [x] 异步数据库操作
- [x] 连接池管理（HikariCP）
- [x] 数据库迁移（自动添加 daily_limit 列）

## 最近修复

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

## 待办事项
- [ ] 考虑使用 Component API 替代弃用的 String 标题方法
- [ ] CustomModelData 新 API 待稳定后更新

## 构建命令
```bash
./gradlew shadowJar
```

输出：`build/libs/folia_shop-1.0.0.jar`
