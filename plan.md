# 文件管理器 (file-m25) 项目规划

## 项目概述
基于Android的本地文件管理器应用，支持文件浏览、搜索、创建文件夹、重命名、删除等基础功能。

## 技术栈
- Kotlin 2.3.20 + Jetpack Compose + Material 3
- MVVM + Clean Architecture
- Hilt依赖注入
- Room数据库
- DataStore键值对存储
- Coil图片加载
- Navigation Compose导航
- 最小/最大SDK: API 36

## 版本规划

### 0.2.0 (已完成版本)
**目标**: 增强功能
- [x] 项目脚手架搭建
- [x] 主题配置 (Material 3)
- [x] 主页文件列表展示
- [x] 文件夹导航功能
- [x] 文件排序功能 (名称/大小/日期)
- [x] 视图切换 (列表/网格)
- [x] 创建文件夹
- [x] 文件重命名
- [x] 文件删除
- [x] 存储空间显示
- [x] 设置页面
- [x] 日志系统
- [x] 简体中文支持
- [x] 文件搜索功能
- [x] 文件复制/移动功能
- [x] 文件详情查看
- [x] 多选操作模式

### 0.2.1 (修复版本)
**目标**: 修复GitHub Actions构建错误
- [x] 修复StorageInfo引用错误

### 0.3.0 (已完成版本)
**目标**: 文件压缩/解压缩、缩略图显示、性能优化
- [x] 文件压缩功能 (ZIP格式)
- [x] 文件解压缩功能 (ZIP格式)
- [x] 图片缩略图显示
- [x] 视频缩略图显示
- [x] 缩略图缓存优化
- [x] 深色模式主题优化
- [x] 代码重构优化

### 0.3.1 (当前版本)
**目标**: 重构优化
- [x] 提取公共FileViewModel消除HomeViewModel和FileViewModel的重复代码
- [x] 统一工具函数位置（formatFileSize、formatDate）
- [x] 添加用户错误反馈（操作失败时显示Snackbar）
- [x] 修复moveFile原子性问题（复制后删除改为确认后再删除）
- [x] Logger添加shutdown方法
- [x] searchRecursive添加深度限制防止性能问题

### 0.4.0 (已完成版本)
**目标**: 高级功能
- [x] 标签/收藏功能（收藏文件/文件夹，方便快速访问）
- [x] 主题自定义（自定义主题颜色）
- [x] OTA文件更新（本地APK文件更新安装）

### 0.4.1 (已完成版本)
**目标**: 修复GitHub Actions构建错误
- [x] 修复FileScreen.kt编译错误（缺少onToggleFavorite参数）
- [x] FileViewModel添加toggleFavorite方法和FavoriteRepository依赖
- [x] 移除RepositoryModule中多余的FavoriteRepository绑定

### 0.4.2 (已完成版本)
**目标**: 修复GitHub Actions构建错误
- [x] 修复FileScreen.kt编译错误（缺少onToggleFavorite参数）
- [x] FileViewModel添加toggleFavorite方法和FavoriteRepository依赖
- [x] 移除RepositoryModule中多余的FavoriteRepository绑定

### 0.5.0 (已完成版本)
**目标**: 文件分享、最近文件
- [x] 文件分享功能（通过系统分享菜单分享文件）
- [x] 最近打开文件列表（记录并展示最近打开的文件）
- [x] 最近文件DAO和Entity创建
- [x] RecentRepository实现
- [x] 最近文件UI入口
- [x] HomeViewModel添加最近文件模式
- [x] FileViewModel同步添加分享和最近文件功能

### 0.5.1 (已完成版本)
**目标**: 修复GitHub Actions构建错误
- [x] 修复HomeViewModel缺少RecentRepository导入错误

### 0.5.2 (已完成版本)
**目标**: 重构优化
- [x] 代码审查与架构优化
- [x] 清理冗余的combinedClickable处理
- [x] 提取共享函数getSortModeLabel到FileUtils.kt

### 0.6.0 (已完成版本)
**目标**: 导航优化和隐藏文件支持
- [x] 隐藏文件/文件夹显示/隐藏切换功能
- [x] 面包屑导航路径优化（可点击路径片段）
- [x] 书签功能（收藏常用目录方便快速访问）
- [x] 平板/折叠屏布局优化（添加WindowSizeClass支持）

### 0.7.0 (已完成版本)
**目标**: 图片预览功能
- [x] 图片预览功能（点击图片文件时全屏预览）
- [x] 图片预览支持缩放和滑动切换
- [x] 图片预览支持分享按钮

### 0.7.1 (当前版本)
**目标**: 修复文件列表显示问题 + 标题栏按钮优化
- [x] 修复Issue #2: 文件列表只显示文件夹不显示文件问题（添加存储权限请求逻辑）
- [x] 修复Issue #1: 标题栏按钮数量超过3个优化（精简为3个按钮）

## 功能优先级
1. 基础文件浏览 - P0
2. 文件操作(创建/删除/重命名) - P0
3. 文件搜索 - P1
4. 复制/移动 - P1
5. 缩略图显示 - P2
6. 压缩/解压 - P2

## 代码量限制
- 当前版本代码量: <5000行
- 目标: 保持在10000行以内
