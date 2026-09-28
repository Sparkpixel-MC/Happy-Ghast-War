# Happy Ghast War

一个基于 Paper 1.21.10 的乐魂战小游戏，使用 Advanced Slime Paper API 管理游戏世界，支持单服多竞技场并发与 TAB 计分板。

## ✨ 特性

- 🎮 多人在线竞技
- 🐸 Ghast 战斗玩法
- 👥 队伍系统
- 🎁 随机战利品箱
- 🏆 排名和积分
- 🗺️ 动态场地缩小
- 💪 角色升级系统

## 🚀 快速开始

### 1. 构建插件

```bash
mvn clean package
```

### 2. 准备服务器

- Paper 1.21.10
- Advanced Slime Paper
- SlimeWorldManager
- TAB（可选，安装后计分板由 TAB 渲染）

### 3. 配置 Arena

将 `.slime` 文件和 YAML 配置放入 `arenas/` 目录。

每个场地的 `arenas/<世界名>.yml` 支持以下玩法开关（新建场地自动写入默认值，旧场地缺省时同样生效）：

| 配置项 | 默认 | 说明 |
|---|---|---|
| `game.fall-damage` | `true` | 玩家是否有摔落伤害 |
| `game.pvp` | `true` | 是否允许玩家互攻（写入世界 PVP 属性，slime 文件里 pvp=false 也会被覆盖） |
| `game.resource-respawn` | `45` | 资源点恢复时间（秒），中立区减半 |
| `game.private-zone-radius` | `30` | 距队出生点该距离内的矿为该队私有资源区，0=全部按中立区 |

全部矿物（煤/铁/铜/金/红石/青金石/钻石/绿宝石/石英，含深板岩与下界变体）、粗金属块和原木均为自动注册的资源点：挖取后掉落对应资源，方块在 `resource-respawn` 秒后自动恢复原状。

游戏音效均可在 `config.yml` 的 `sounds:` 段自定义（支持命名空间键或旧枚举名，可附加 `,音量,音调`），未配置时使用内置默认值。

详细说明请查看 [BUILD_GUIDE.md](BUILD_GUIDE.md) 和 [MIGRATION_TO_SLIMEWORLD.md](MIGRATION_TO_SLIMEWORLD.md)。

## 📖 文档

- [构建指南](BUILD_GUIDE.md) - 如何构建和部署
- [迁移指南](MIGRATION_TO_SLIMEWORLD.md) - 从 Multiverse 迁移到 SlimeWorld
- [API 文档](docs/index.md) - SlimeWorld API 使用说明
- [加载世界](docs/loading_worlds.md) - 如何加载世界
- [属性配置](docs/properties.md) - 世界属性设置

## 🛠️ 命令

### 主命令 `/gw` 或 `/ghastwar`

- `/gw admin` - 管理员命令
  - `/gw admin create <name>` - 创建新场地
  - `/gw admin setcenter <x> <y> <z>` - 设置中心点
  - `/gw admin setradius <r>` - 设置场地半径
  - `/gw admin addchest` - 添加战利品箱

- `/gw join` - 加入游戏
- `/gw leave` - 离开游戏
- `/gw status` - 查看场地状态
- `/gw stats` - 查看个人统计
- `/gw gui` - 打开游戏菜单
- `/gw debug info|start` - 调试命令

### 队伍命令 `/party` 或 `/p`

- `/party create` - 创建队伍
- `/party invite <player>` - 邀请玩家
- `/party accept` - 接受邀请
- `/party leave` - 离开队伍
- `/party chat <消息>` - 队伍聊天/游戏内队伍警报

## 📦 项目结构

```
Happy-Ghast-War/
├── src/main/java/          # Java 源代码
├── src/main/resources/     # 配置文件和资源
├── docs/                   # 项目文档
│   ├── index.md           # API 说明
│   ├── loading_worlds.md  # 世界加载
│   └── properties.md      # 属性配置
├── arenas/                 # 场地配置和 SlimeWorld 文件
├── pom.xml                 # Maven 配置
├── README.md               # 项目说明
└── BUILD_GUIDE.md          # 构建指南
```

## 🔧 技术栈

- **服务器**: Paper 1.21.10
- **API**: Advanced Slime Paper 4.0.0-SNAPSHOT + TAB-API 6.1.2
- **构建工具**: Maven
- **Java 版本**: 21

## 🎯 游戏玩法

### 基本规则

1. **准备阶段**: 玩家加入场地，等待游戏开始
2. **开发阶段**: 收集资源和升级装备
3. **战斗阶段**: 场地缩小，对抗 Ghast
4. **终极阶段**: 最终决战，争夺胜利

### 升级系统

- 铁匠台制作合金
- 升级装备属性
- 提升战斗力

### 战利品

- 随机生成的战利品箱
- 稀有掉落物
- 资源收集

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目仅供学习和研究使用。

## 🔗 相关链接

- [PaperMC](https://papermc.io/)
- [Advanced Slime Paper](https://docs.infernalsuite.com/)
- [SlimeWorld 格式](https://github.com/InfernalSuite/AdvancedSlimePaper)

---

**版本**: 1.0-beta
**更新日期**: 2026-08-13
**服务器 IP**: ngup.eu.org
