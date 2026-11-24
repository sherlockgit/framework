---
name: maintaining-java-framework-docs
description: Use when changing Java production types, public APIs, Spring Boot auto-configuration, Maven modules, or framework documentation in this repository.
---

# 维护 Java 框架文档

## 目标

保持代码、模块设计文档和提交记录一致。所有说明性文档、Java 注释及 Git 提交信息使用中文；类名、包名、配置键、错误码、HTTP 路径、依赖坐标和其他代码标识符保持原样。

## 文档边界

| 改动范围 | 必须同步的文档 |
| --- | --- |
| 根模块结构、技术基线、全局依赖方向 | 根 `README.md` |
| Maven 叶子模块职责、依赖、配置、接入方式或测试策略 | `<模块>/README.md` |
| 任意 `src/main/java` 类型新增、删除或行为变更 | `<模块>/docs/classes.md` |
| 仅 `src/test` 改动 | 仅在测试策略或验证方式改变时更新模块 `README.md` |
| 仅构建、BOM 或聚合模块改动 | 相应模块 README，必要时根 `README.md` |

生产类型包括 class、interface、record、enum 和 annotation。即使新类型是模块内部实现，也必须出现在所属模块的 `docs/classes.md`。测试类不建立类级说明。

## 文档结构

根 `README.md` 只写项目定位、架构、模块导航、技术基线和构建验证，不重复类级细节。

有生产代码的叶子模块：

```text
<模块>/README.md
<模块>/docs/classes.md
```

模块 README 写职责与边界、依赖、配置或自动装配、接入方式、安全约束、测试策略，并链接到 `docs/classes.md`。

`docs/classes.md` 按 Java 包分组。每个生产类型写：完整包名、职责、主要协作对象、关键公开 API 或输入、关键不变量或异常语义，以及适用的租户、认证、线程上下文、缓存和并发约束。

不要逐行复述实现；没有相关约束时明确写“无特殊租户或线程上下文约束”。

## 工作流程

1. 识别改动是否涉及根 README、模块 README、类说明或测试策略。
2. 修改代码的同一变更中更新受影响文档；仅更新必要范围，不重写无关模块。
3. 对新增、删除或改名的生产类型，核对 `docs/classes.md` 中的类型清单同步增删改，且每个类型只出现一次。
4. 检查文档中的模块路径、包名、类名、配置键和项目内链接与实际仓库一致。
5. 使用中文撰写本项目的 Git 提交信息；可保留 Conventional Commit 前缀，例如 `feat: 增加租户缓存适配器`。
6. 若项目的忽略规则包含 `docs/`，不自动改写用户的 `.gitignore`；新增或首次跟踪的模块 `docs/classes.md` 使用 `git add -f` 暂存。

## 提交前检查

```text
- 生产类型改动已同步到对应 classes.md。
- 模块职责、配置或测试策略变更已同步到模块 README。
- 全局结构变更已同步到根 README。
- 文档说明与 Java 注释使用中文，代码标识符未被翻译。
- Git 提交标题使用中文。
```

## 常见错误

- 只在公开 API 改动时更新类说明：错误。内部生产类同样需要可维护的职责说明。
- 为测试类创建类级文档：错误。测试类只在测试策略中汇总。
- 把 `tenant_id`、`JWT`、类名或配置键翻译为自然语言：错误。说明使用中文，标识符保留原文。
- 因文档内容是中文仍提交英文信息：错误。本仓库提交信息使用中文。
