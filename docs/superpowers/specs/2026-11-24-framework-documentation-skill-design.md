# 框架文档与 Skill 规范设计

## 状态

待用户评阅后实施。

## 目标

为 `sherlock-framework` 建立可持续维护的中文技术文档体系，并在项目内创建 Skill，使后续对生产代码、公开 API、Starter 自动装配、配置属性和 Maven 模块的修改能够同步更新对应文档。

所有说明性文档、代码注释和 Git 提交信息使用中文。类名、包名、配置键、错误码、HTTP 路径、依赖坐标和其他代码标识符保持原样。

## 文档布局

```text
README.md
<叶子模块>/README.md
<叶子模块>/docs/classes.md
.codex/skills/maintaining-java-framework-docs/SKILL.md
```

### 根 README

根 `README.md` 是项目入口，仅描述全局信息：

- 框架定位与适用范围；
- 整体架构和依赖方向；
- Maven 模块地图；
- Java、Spring Boot、MySQL、Redis 等技术基线；
- 构建、验证和 Docker 对 Testcontainers 的要求；
- 指向模块文档的导航。

根 README 不重复类级细节，也不承担模块配置参考。

### 叶子模块 README

有 `src/main/java` 的 Maven 叶子模块创建模块级 `README.md`。其内容包括：

- 模块职责与不承担的职责；
- 对其他框架模块和第三方依赖的依赖关系；
- 自动装配、配置属性和运行前提；
- 典型接入方式或最小示例；
- 租户、认证、线程上下文、异常或缓存等与模块相关的安全约束；
- 测试策略及 Docker/Testcontainers 是否为必需验证条件；
- 指向 `docs/classes.md` 的链接。

只含聚合 POM、BOM 或无生产代码的模块只创建简短 README，说明聚合或版本管理职责，不创建类说明文件。

### 类说明文档

每个有生产代码的叶子模块使用唯一的 `<模块>/docs/classes.md`，按 Java 包分组，覆盖 `src/main/java` 中的生产类、接口、record、枚举和注解。

每个类型至少说明：

- 类型名称和完整包名；
- 职责及主要协作对象；
- 关键公开方法、构造参数或配置输入；
- 关键不变量、异常语义和安全边界；
- 与租户、认证、数据范围、线程上下文或缓存相关的约束；
- 不适用时明确说明不涉及的并发或租户约束。

类说明描述公开行为与设计决策，不逐行复述实现，也不为测试类单独建档。测试类仅在模块 README 的测试策略中汇总。

## 项目内 Skill

Skill 路径为：

```text
.codex/skills/maintaining-java-framework-docs/
```

Skill 在以下场景应用：

- 新增、删除或修改 `src/main/java` 的生产类型；
- 修改公开 API、错误码、配置属性、自动装配或模块依赖；
- 新增、拆分或调整 Maven 模块；
- 修改影响模块职责、租户边界、认证授权、数据范围、缓存或消息可靠性的行为。

Skill 的工作要求：

1. 识别受影响的根 README、模块 README 和 `docs/classes.md`；
2. 仅更新受影响文档，避免无关重写；
3. 使文档、代码注释和 Git 提交信息使用中文；
4. 保留代码标识符原文；
5. 在提交前检查文档链接、类型名称、模块路径和类清单是否与代码一致；
6. 没有生产代码改动时，不强制重写类说明文档。

Skill 为项目专属规范，不安装到用户全局 Skill 目录，也不约束其他项目。

## 生成顺序

1. 重写根 README 为中文项目入口文档。
2. 为当前叶子模块生成 README；对无生产代码模块生成简短职责说明。
3. 为有生产代码的叶子模块生成 `docs/classes.md`。
4. 创建项目内 Skill，并通过模拟的新增生产类和仅修改测试类两种场景验证其文档更新判断。
5. 校验 Markdown 链接、模块路径、生产类清单和中文提交信息。

## 非目标

- 不迁移或删除现有 `docs/superpowers/specs` 与 `docs/superpowers/plans` 中的设计、计划文档。
- 不为第三方库、测试类、生成代码或 Maven `target` 目录生成类级文档。
- 不将注释翻译规则应用到错误码、配置键、SQL、HTTP 路径或协议字段。
- 不改变 Java 代码、Maven 模块行为或运行时依赖。

## 验收标准

- 根 README、模块 README、类说明文档和 Skill 全部为中文说明。
- 每个当前生产类型恰好在其模块 `docs/classes.md` 中出现一次。
- README 中所有项目内链接和模块路径有效。
- Skill 能区分生产代码改动与仅测试代码改动，并给出对应文档维护动作。
- 此次新增的 Git 提交信息使用中文。
