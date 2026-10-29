# LegacyLink - ITransformationService 实现

## 概述

LegacyLink 现在使用 Forge 的 `ITransformationService` API 来注册字节码转换器，而不是依赖 CoreMod JavaScript 文件。这种方式更可靠，因为：

1. **更早的加载时机**：TransformationService 在 ModLauncher 初始化时就被加载，比 CoreMod 更早
2. **更稳定的 API**：使用 Java 接口而不是 JavaScript，避免了 JS 引擎的兼容性问题
3. **更好的错误处理**：Java 代码更容易调试和捕获异常

## 架构变化

### 之前（CoreMod 方式）
```
mods.toml → coremod.js → LegacyLinkTransformer
```

### 现在（ITransformationService 方式）
```
META-INF/services/cpw.mods.modlauncher.api.ITransformationService 
    → LegacyLinkTransformationService 
    → LegacyLinkTransformer
```

## 文件结构

### 核心文件
- `LegacyLinkTransformer.java` - 字节码转换逻辑（未改变）
- `LegacyLinkTransformationService.java` - 注册转换器服务（新增）
- `META-INF/services/cpw.mods.modlauncher.api.ITransformationService` - 服务注册文件（新增）

### 辅助文件
- `CreativeModeTabIconHelper.java` - 处理 CreativeModeTab 图标（新增）
- 删除了 `legacylink_coremod.js`
- 删除了 `CreativeModeTabCompat.java`
- 删除了 `LegacyLinkRuntimeTransformer.java`

## CreativeModeTab 处理

### 问题
1.19.2 的 `CreativeModeTab(String)` 构造函数在 1.20.1 中被移除，只保留了 `CreativeModeTab(Builder)` 构造函数。

### 解决方案
不重命名超类（保持匿名类继承真实的 `CreativeModeTab`），而是重写构造函数调用：

**转换前：**
```java
new CreativeModeTab("my_tab") {
    @Override
    public ItemStack makeIcon() {
        return new ItemStack(MY_ITEM.get());
    }
}
```

**转换后（字节码层面）：**
```
1. 调用 CreativeModeTabIconHelper.buildBuilder("my_tab") 获取 Builder
2. 调用 super(Builder) 而不是 super(String)
3. 调用 CreativeModeTabIconHelper.bindPendingIcon(this) 绑定图标
```

### CreativeModeTabIconHelper 工作原理
1. `buildBuilder(String)` - 创建带有正确标题的 Builder
2. `bindPendingIcon(Object)` - 在构造后绑定图标提供者，使用反射调用 `makeIcon()` 方法

## 配置变化

### mods.toml
移除了 `[[coremods]]` 部分，不再需要 CoreMod 声明。

### build.gradle
移除了 JAR manifest 中的 `FMLCorePlugin` 和 `FMLCorePluginContainsFMLMod` 属性。

## 测试

重新构建 mod：
```bash
./gradlew clean build
```

检查日志中是否有：
- `[LegacyLink] Transformation service initializing`
- `[LegacyLink] Transformation service loaded`
- `[LegacyLink] Registering class transformers`
- `[LegacyLink] Transforming class via service: ...`

## 优势

1. **无需 JavaScript** - 纯 Java 实现，更稳定
2. **更早加载** - 在所有 mod 类加载之前就开始转换
3. **更好的集成** - 使用 Forge 官方推荐的扩展点
4. **更容易调试** - Java 代码比 JavaScript 更容易调试

## 未来改进

- 添加更多的转换规则来处理其他 API 变化
- 实现更智能的 CreativeModeTab 图标处理（避免使用反射）
- 添加转换统计和报告功能
