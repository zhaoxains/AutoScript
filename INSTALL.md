# 安装配置文档

本文档详细介绍 AutoScript 项目的安装和配置步骤。

---

## 目录

- [环境要求](#环境要求)
- [后端部署](#后端部署)
- [APP 编译安装](#app-编译安装)
- [配置说明](#配置说明)
- [宝塔面板部署](#宝塔面板部署)
- [常见问题](#常见问题)

---

## 环境要求

### 服务器端

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| PHP | >= 7.4 | 推荐 PHP 8.0+ |
| MySQL | >= 5.7 | 推荐 MySQL 8.0+ |
| Nginx/Apache | 任意版本 | 推荐 Nginx |
| Composer | >= 2.0 | PHP 依赖管理 |

### PHP 扩展

- pdo_mysql
- mysqli
- json
- mbstring
- openssl

### Android 端

| 要求 | 版本 |
|------|------|
| Android 版本 | >= 7.0 (API 24) |
| 编译 SDK | 34 |
| JDK | 17+ |
| Gradle | 8.5+ |

---

## 后端部署

### 方式一：手动部署

#### 1. 上传代码

将 `AutoServer` 目录上传到服务器：

```bash
# 创建目录
mkdir -p /var/www/autoscript

# 上传代码（使用 scp、ftp 或其他方式）
scp -r AutoServer/* user@server:/var/www/autoscript/
```

#### 2. 安装依赖

```bash
cd /var/www/autoscript
composer install --no-dev
```

如果未安装 Composer：

```bash
curl -sS https://getcomposer.org/installer | php
mv composer.phar /usr/local/bin/composer
```

#### 3. 创建数据库

```sql
CREATE DATABASE auto_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 4. 导入数据库结构

```bash
mysql -u root -p auto_app < database/auto_app.sql
```

#### 5. 配置数据库连接

编辑 `config/app.php`：

```php
<?php

return [
    'database' => [
        'host' => 'localhost',
        'database' => 'auto_app',
        'username' => 'root',
        'password' => 'your_password',
    ],
    'jwt' => [
        'secret' => 'your-jwt-secret-key-change-me',
    ],
    'app' => [
        'name' => 'AutoApp',
        'debug' => false,
    ],
];
```

#### 6. 设置目录权限

```bash
chmod -R 755 /var/www/autoscript
chown -R www-data:www-data /var/www/autoscript
```

### 方式二：宝塔面板部署

详细步骤请参考 [宝塔面板部署指南](宝塔面板部署指南.md)。

---

## APP 编译安装

### 环境准备

1. 安装 Android Studio
2. 安装 JDK 17+
3. 配置 ANDROID_HOME 环境变量

### 编译步骤

#### 1. 打开项目

使用 Android Studio 打开 `AutoApp` 目录。

#### 2. 配置服务器地址

编辑 `app/src/main/java/com/auto/app/data/remote/ApiService.kt`：

```kotlin
private const val BASE_URL = "https://your-server.com/"
```

或使用 BuildConfig 配置：

编辑 `app/build.gradle.kts`：

```kotlin
android {
    defaultConfig {
        buildConfigField("String", "API_BASE_URL", "\"https://your-server.com/\"")
    }
}
```

#### 3. 编译 APK

**命令行编译：**

```bash
# Debug 版本
./gradlew assembleDebug

# Release 版本
./gradlew assembleRelease
```

**Android Studio 编译：**

1. 点击菜单 Build → Generate Signed Bundle / APK
2. 选择 APK
3. 创建或选择签名密钥
4. 选择 release 构建变体
5. 点击 Finish

#### 4. 安装 APK

APK 输出位置：

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

安装方式：

```bash
# 使用 adb 安装
adb install app-debug.apk

# 或直接将 APK 传输到手机安装
```

---

## 配置说明

### 后端配置

#### config/app.php

```php
<?php

return [
    // 数据库配置
    'database' => [
        'host' => 'localhost',
        'database' => 'auto_app',
        'username' => 'root',
        'password' => 'password',
    ],
    
    // JWT 配置
    'jwt' => [
        'secret' => 'your-secret-key',
        'expires' => 86400 * 7, // 7天过期
    ],
    
    // 应用配置
    'app' => [
        'name' => 'AutoApp',
        'debug' => false,
        'timezone' => 'Asia/Shanghai',
    ],
];
```

#### config/routes.php

路由配置文件，定义 API 路由规则。

### APP 配置

#### 服务器地址

在登录界面输入服务器地址，或修改代码中的默认地址。

#### 无障碍服务

APP 需要开启无障碍服务才能执行自动化操作：

1. 打开 APP → 设置 → 无障碍服务
2. 跳转到系统无障碍设置
3. 找到 AutoScript 服务并开启

#### 悬浮窗权限

用于显示调试信息：

1. 打开 APP → 设置 → 悬浮窗权限
2. 授予悬浮窗权限

---

## 宝塔面板部署

### 1. 安装软件

在宝塔面板【软件商店】中安装：

- Nginx
- PHP 8.0
- MySQL 5.7+
- phpMyAdmin

### 2. PHP 扩展

安装以下扩展：

- pdo_mysql
- mysqli
- json
- mbstring
- openssl

### 3. PHP 禁用函数

删除以下禁用函数：

- putenv
- proc_open
- proc_get_status

### 4. 创建网站

1. 点击【网站】→【添加站点】
2. 设置域名和根目录
3. PHP 版本选择 PHP-80

### 5. 设置运行目录

1. 点击网站【设置】→【网站目录】
2. 运行目录设置为 `/public`

### 6. 配置伪静态

```nginx
location / {
    try_files $uri $uri/ /index.php?$query_string;
}

location ~ \.php$ {
    fastcgi_pass unix:/tmp/php-cgi-80.sock;
    fastcgi_index index.php;
    fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
    include fastcgi_params;
}

location ~ /\. {
    deny all;
}

location ~* ^/(config|app|database|vendor)/ {
    deny all;
}
```

### 7. SSL 证书

推荐开启 HTTPS：

1. 点击网站【设置】→【SSL】
2. 选择 Let's Encrypt 免费证书
3. 申请并部署
4. 开启强制 HTTPS

---

## Nginx 完整配置

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /var/www/autoscript/public;
    index index.php index.html;
    
    # 访问日志
    access_log /var/log/nginx/autoscript.access.log;
    error_log /var/log/nginx/autoscript.error.log;
    
    # 主配置
    location / {
        try_files $uri $uri/ /index.php?$query_string;
    }
    
    # PHP 处理
    location ~ \.php$ {
        fastcgi_pass unix:/run/php/php8.0-fpm.sock;
        fastcgi_index index.php;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
        include fastcgi_params;
        
        fastcgi_connect_timeout 300;
        fastcgi_send_timeout 300;
        fastcgi_read_timeout 300;
    }
    
    # 禁止访问隐藏文件
    location ~ /\. {
        deny all;
    }
    
    # 禁止访问敏感目录
    location ~* ^/(config|app|database|vendor)/ {
        deny all;
    }
    
    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}

# HTTP 跳转 HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

# HTTPS 配置
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    root /var/www/autoscript/public;
    
    ssl_certificate /etc/nginx/ssl/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/your-domain.com/privkey.pem;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    
    # ... 其他配置同上
}
```

---

## 常见问题

### 1. 500 错误

**原因**：PHP 配置或权限问题

**解决**：

```bash
# 检查日志
tail -f /var/log/nginx/autoscript.error.log

# 检查权限
chmod -R 755 /var/www/autoscript
chown -R www-data:www-data /var/www/autoscript

# 检查 PHP 配置
php -v
php -m | grep pdo_mysql
```

### 2. 数据库连接失败

**原因**：数据库配置错误或权限不足

**解决**：

```bash
# 测试数据库连接
mysql -u root -p auto_app

# 检查用户权限
GRANT ALL PRIVILEGES ON auto_app.* TO 'root'@'localhost';
FLUSH PRIVILEGES;
```

### 3. CORS 跨域问题

**解决**：在 `public/index.php` 开头添加：

```php
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}
```

### 4. APP 无法连接服务器

**原因**：网络问题或服务器地址错误

**解决**：

1. 检查服务器是否正常运行
2. 检查防火墙是否开放端口
3. 检查 APP 中服务器地址是否正确
4. 检查是否使用 HTTPS

### 5. 无障碍服务无法开启

**原因**：权限不足

**解决**：

1. 确保已授予 APP 所有必要权限
2. 重启手机后重试
3. 卸载重装 APP

### 6. Composer 安装失败

**解决**：

```bash
# 使用国内镜像
composer config -g repo.packagist composer https://mirrors.aliyun.com/composer/

# 清除缓存
composer clear-cache

# 重新安装
composer install --no-dev
```

---

## 默认账号

| 账号类型 | 用户名 | 密码 |
|---------|-------|------|
| 管理员 | admin | admin123 |

⚠️ **重要**：部署完成后请立即修改默认密码！

---

## 安全建议

1. **修改默认密码** - 立即修改 admin 密码
2. **关闭调试模式** - 生产环境设置 `debug => false`
3. **修改 JWT 密钥** - 使用随机字符串
4. **开启 HTTPS** - 保护数据传输安全
5. **定期备份** - 设置数据库自动备份
6. **防火墙配置** - 只开放必要端口

---

## 下一步

- 查看 [功能特性文档](FEATURES.md) 了解详细功能
- 查看 [脚本配置文档](SCRIPT_CONFIG.md) 学习编写脚本
- 查看 [API 文档](API.md) 了解接口详情
