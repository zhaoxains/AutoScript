# 贡献指南

感谢您对 AutoScript 项目的关注！本文档将帮助您了解如何参与项目开发。

---

## 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
- [开发环境搭建](#开发环境搭建)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [Pull Request 流程](#pull-request-流程)
- [项目架构](#项目架构)
- [开发指南](#开发指南)

---

## 行为准则

### 我们的承诺

为了营造一个开放和友好的环境，我们承诺：

- 尊重所有贡献者
- 接受建设性批评
- 关注对社区最有利的事情
- 对其他社区成员表示同理心

### 不可接受的行为

- 使用性化的语言或图像
- 侮辱性/贬损性评论
- 公开或私下的骚扰
- 未经许可发布他人私人信息
- 其他在专业环境中被认为不适当的行为

---

## 如何贡献

### 报告 Bug

如果您发现了 Bug，请通过 [GitHub Issues](https://github.com/zhaoxains/autoscript/issues) 提交。

**Bug 报告应包含：**

1. **标题** - 简明扼要地描述问题
2. **描述** - 详细描述问题
3. **复现步骤** - 如何复现该问题
4. **预期行为** - 您期望发生什么
5. **实际行为** - 实际发生了什么
6. **环境信息**：
   - Android 版本
   - APP 版本
   - 设备型号
   - 服务器环境
7. **日志/截图** - 相关的日志或截图

**Bug 报告模板：**

```markdown
## Bug 描述

[清晰简洁地描述问题]

## 复现步骤

1. 打开 APP
2. 点击 '...'
3. 滚动到 '...'
4. 出现错误

## 预期行为

[描述您期望发生什么]

## 实际行为

[描述实际发生了什么]

## 环境信息

- Android 版本: [例如: Android 12]
- APP 版本: [例如: 1.0.0]
- 设备型号: [例如: Xiaomi 13]
- 服务器 PHP 版本: [例如: PHP 8.0]

## 日志/截图

[粘贴相关日志或添加截图]
```

### 建议新功能

如果您有新功能的建议，请通过 [GitHub Issues](https://github.com/zhaoxains/autoscript/issues) 提交。

**功能请求应包含：**

1. **标题** - 简明扼要地描述功能
2. **描述** - 详细描述该功能
3. **用例** - 该功能解决什么问题
4. **建议方案** - 如果有实现思路

### 提交代码

1. Fork 本仓库
2. 创建特性分支
3. 编写代码
4. 提交 Pull Request

---

## 开发环境搭建

### 后端开发环境

#### 1. 安装依赖

```bash
# 安装 PHP (Windows)
# 下载 https://windows.php.net/download/

# 安装 Composer
curl -sS https://getcomposer.org/installer | php
mv composer.phar /usr/local/bin/composer

# 安装项目依赖
cd AutoServer
composer install
```

#### 2. 配置数据库

```sql
CREATE DATABASE auto_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```bash
mysql -u root -p auto_app < database/auto_app.sql
```

#### 3. 配置项目

编辑 `config/app.php`：

```php
return [
    'database' => [
        'host' => 'localhost',
        'database' => 'auto_app',
        'username' => 'root',
        'password' => 'your_password',
    ],
    'app' => [
        'debug' => true,  // 开发环境开启调试
    ],
];
```

#### 4. 启动开发服务器

```bash
cd AutoServer/public
php -S localhost:8000
```

### Android 开发环境

#### 1. 安装工具

- [Android Studio](https://developer.android.com/studio) (最新版本)
- JDK 17+
- Android SDK (API 34)

#### 2. 配置环境变量

```bash
# Windows
setx ANDROID_HOME "C:\Users\YourName\AppData\Local\Android\Sdk"
setx PATH "%PATH%;%ANDROID_HOME%\platform-tools"

# Linux/Mac
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

#### 3. 打开项目

1. 启动 Android Studio
2. 选择 "Open an Existing Project"
3. 选择 `AutoApp` 目录
4. 等待 Gradle 同步完成

#### 4. 配置服务器地址

编辑 `app/src/main/java/com/auto/app/data/remote/ApiService.kt`：

```kotlin
private const val BASE_URL = "http://10.0.2.2:8000/"  // 模拟器访问本地
// 或
private const val BASE_URL = "http://192.168.1.100:8000/"  // 真机访问
```

#### 5. 运行项目

1. 连接 Android 设备或启动模拟器
2. 点击 Run 按钮 (绿色三角形)
3. 或使用命令行：`./gradlew installDebug`

---

## 代码规范

### Kotlin 代码规范

#### 命名规范

```kotlin
// 类名：大驼峰
class MainActivity : AppCompatActivity()

// 函数名：小驼峰
fun executeScript() { }

// 变量名：小驼峰
val scriptList = mutableListOf<Script>()

// 常量：全大写下划线分隔
const val MAX_RETRY_COUNT = 3

// 私有变量：下划线前缀
private var _isRunning = MutableStateFlow(false)
```

#### 文件结构

```kotlin
// 1. 文件头注释
// 2. package 语句
package com.auto.app.engine

// 3. import 语句（按字母排序）
import android.content.Context
import com.auto.app.data.model.Script
import kotlinx.coroutines.Dispatchers

// 4. 类/接口定义
class ExecutionEngine @Inject constructor(
    private val context: Context,
    private val repository: AppRepository
) {
    // 伴生对象
    companion object {
        private const val TAG = "ExecutionEngine"
    }
    
    // 属性
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    // 初始化块
    init {
        // 初始化代码
    }
    
    // 公共方法
    fun executeScript(script: Script) { }
    
    // 私有方法
    private fun executeControl(config: ControlConfig) { }
    
    // 内部类
    inner class ScriptExecutor { }
}
```

### PHP 代码规范

#### 命名规范

```php
// 类名：大驼峰
class AdminController extends Controller {}

// 方法名：小驼峰
public function getUserList() {}

// 变量名：小驼峰
$userList = [];

// 常量：全大写下划线分隔
const MAX_PAGE_SIZE = 100;

// 数据库表名：小写下划线
auto_user
auto_script
```

---

## 提交规范

### Commit Message 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

| 类型 | 说明 |
|------|------|
| feat | 新功能 |
| fix | Bug 修复 |
| docs | 文档更新 |
| style | 代码格式（不影响功能） |
| refactor | 重构 |
| perf | 性能优化 |
| test | 测试相关 |
| chore | 构建/工具相关 |
| revert | 回滚 |

### 示例

```bash
# 新功能
feat(app): 添加脚本导入功能

# Bug 修复
fix(engine): 修复位置点击坐标计算错误

# 文档更新
docs(api): 更新 API 文档

# 重构
refactor(server): 重构用户认证逻辑
```

---

## Pull Request 流程

### 1. Fork 仓库

点击 GitHub 页面右上角的 "Fork" 按钮。

### 2. 克隆仓库

```bash
git clone https://github.com/zhaoxains/autoscript.git
cd autoscript
```

### 3. 创建分支

```bash
git checkout -b feature/your-feature-name
```

### 4. 编写代码

- 遵循代码规范
- 添加必要的注释
- 编写单元测试（如适用）

### 5. 提交更改

```bash
git add .
git commit -m "feat(app): 添加新功能"
```

### 6. 推送分支

```bash
git push origin feature/your-feature-name
```

### 7. 创建 Pull Request

1. 访问您的 Fork 仓库
2. 点击 "New Pull Request"
3. 填写 PR 标题和描述
4. 等待审核

---

## 项目架构

### Android APP 架构

```
AutoApp/
├── app/src/main/java/com/auto/app/
│   ├── AutoApp.kt                 # Application 类
│   │
│   ├── data/                      # 数据层
│   │   ├── local/                 # 本地数据
│   │   ├── model/                 # 数据模型
│   │   ├── remote/                # 远程数据
│   │   └── repository/            # 数据仓库
│   │
│   ├── di/                        # 依赖注入
│   │
│   ├── engine/                    # 执行引擎
│   │   ├── ExecutionEngine.kt     # 主执行引擎
│   │   ├── ControlEngine.kt       # 控件操作引擎
│   │   └── AdEngine.kt            # 广告处理引擎
│   │
│   ├── service/                   # 后台服务
│   │   ├── AutoAccessibilityService.kt
│   │   ├── HeartbeatService.kt
│   │   └── ScriptScheduler.kt
│   │
│   ├── ui/                        # 界面层
│   │   ├── login/                 # 登录模块
│   │   ├── main/                  # 主页模块
│   │   └── profile/               # 个人中心
│   │
│   └── util/                      # 工具类
```

### PHP 后端架构

```
AutoServer/
├── app/
│   ├── Controllers/               # 控制器
│   │   ├── AdminController.php    # 管理接口
│   │   ├── AuthController.php     # 认证接口
│   │   └── ReportController.php   # 上报接口
│   │
│   └── Core/                      # 核心类
│       ├── Application.php        # 应用类
│       ├── Controller.php         # 控制器基类
│       ├── Database.php           # 数据库类
│       └── JWT.php                # JWT 工具类
│
├── config/
│   ├── app.php                    # 应用配置
│   └── routes.php                 # 路由配置
│
├── database/
│   └── auto_app.sql               # 数据库结构
│
└── public/
    ├── admin/                     # 后台管理页面
    └── index.php                  # 入口文件
```

---

## 开发指南

### 添加新的操作类型

#### 1. 定义配置模型

在 `Script.kt` 中添加配置字段：

```kotlin
@Parcelize
data class ControlConfig(
    // 现有字段...
    
    @SerializedName("new_operation_config")
    val newOperationConfig: NewOperationConfig? = null
) : Parcelable
```

#### 2. 实现执行逻辑

在 `ControlEngine.kt` 中添加：

```kotlin
suspend fun executeSpecialOperation(config: ControlConfig): Boolean {
    // ...
    config.operation == "new_operation" -> {
        executeNewOperation(service, config)
    }
    // ...
}
```

### 添加新的 API 接口

#### 1. 定义路由

在 `config/routes.php` 中添加：

```php
return [
    'GET /api/new-endpoint' => [NewController::class, 'newMethod'],
];
```

#### 2. 创建控制器

```php
<?php

namespace App\Controllers;

use App\Core\Controller;

class NewController extends Controller
{
    public function newMethod()
    {
        $user = $this->requireAuth();
        
        // 处理逻辑
        
        return $this->success($data);
    }
}
```

---

## 联系方式

- **Issues**: [GitHub Issues](https://github.com/zhaoxains/autoscript/issues)
- **Discussions**: [GitHub Discussions](https://github.com/zhaoxains/autoscript/discussions)

---

## 许可证

通过贡献代码，您同意您的贡献将根据 MIT 许可证授权。
