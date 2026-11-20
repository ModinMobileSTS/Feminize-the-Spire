# AGENT Guidelines

## 融合新娘化模组

- 优先把外部娘化模组的资源解包进本项目的 `src/main/resources/`，不要让 `build.gradle.kts` 在构建时依赖桌面、下载目录或其它本机绝对路径下的 jar。
- 如果资源来自可选模组，例如 Downfall，建议放到独立前缀下，例如 `src/main/resources/feminizethespire/downfall_overrides/`，再由补丁按需重定向资源路径。
- 不要直接复制外部 jar 里的补丁 `.class` 到本模组，除非确认它不硬引用可选模组类。优先反编译/反汇编理解行为，然后在本项目里重写最小兼容补丁。
- 对可选模组的补丁必须避免 Java 层硬依赖。不要 `import slimebound...`、`import gremlin...`、`import theHexaghost...`、`import reskinContent...` 等 Downfall 类。
- patch 可选模组类时使用字符串类名，例如 `@SpirePatch(cls = "reskinContent.reskinContent", ...)`，并设置 `requiredModId = "downfall"`、`optional = true`。
- 访问可选模组字段或方法时使用反射，并在失败时安全返回原逻辑，避免缺少可选模组或版本差异导致启动崩溃。
- 不要在 `ModTheSpire.json` 的 `dependencies` 中加入可选模组依赖，除非产品需求明确要求该模组变成硬依赖。
- 新增的可选模组娘化应接入配置面板，默认值按现有风格设置。Downfall 当前使用 `activeDownfallFeminization`，运行时入口为 `DownfallCompatibilityPatches.activeFeminization`。
- 关闭配置开关时，资源路径重定向、渲染补丁、动画补丁、默认皮肤补丁都必须直接跳过或返回原路径。

## Downfall 皮肤保存 Bug

- Downfall 的 `reskinContent.reskinContent.loadSettings()` 会从 `reskinContentSaveData` 读取每个角色的 `reskinCount` 和 `reskinUnlock`。
- 不要在 `loadSettings` 的 postfix 里强制写 `reskinCount = 1` 或其它固定值。这样会覆盖玩家保存的角色皮肤选择，导致重启游戏后皮肤回到固定/默认选择。
- 如果需要让融合进来的皮肤默认可选，只解锁皮肤即可，例如设置 `reskinUnlock = true`。
- 当前修复方式是 `DownfallCompatibilityPatches.unlockAlternateSkins()` 只设置 `reskinUnlock`，不再修改 `reskinCount`。
- 如果未来必须设置默认皮肤，只能在明确确认没有已保存选择时设置，并且要先检查对应 `SpireConfig` key 是否存在；不能无条件覆盖 Downfall 已读取的值。

## 验证清单

- 运行 `./gradlew.bat build` 验证编译和打包。
- 检查最终 jar 中包含新增资源，但不包含外部模组原补丁类或外部 jar 本体。
- 检查可选模组补丁注解仍包含 `requiredModId` 和 `optional = true`。
- 在未安装可选模组时，启动不应因缺少类而崩溃。
- 在安装 Downfall 时，切换角色皮肤、退出并重启游戏后，选择的皮肤应保持不变。
