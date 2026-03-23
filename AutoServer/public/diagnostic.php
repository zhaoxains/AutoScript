<?php
require_once __DIR__ . '/../vendor/autoload.php';

$config = require __DIR__ . '/../config/app.php';
App\Core\Database::init($config['database']);

$db = App\Core\Database::getInstance();

header('Content-Type: text/html; charset=utf-8');
?>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>AutoApp 密码诊断</title>
    <style>
        body { font-family: monospace; padding: 20px; background: #f5f5f5; }
        .box { background: white; padding: 20px; margin: 10px 0; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .success { color: green; }
        .error { color: red; }
        .info { color: blue; }
        pre { background: #f0f0f0; padding: 10px; overflow-x: auto; }
        h2 { border-bottom: 2px solid #1976D2; padding-bottom: 10px; }
    </style>
</head>
<body>
    <h1>AutoApp 密码诊断工具</h1>
    
    <div class="box">
        <h2>1. 数据库连接测试</h2>
        <?php
        try {
            $db->fetch("SELECT 1");
            echo "<p class='success'>✓ 数据库连接成功</p>";
        } catch (Exception $e) {
            echo "<p class='error'>✗ 数据库连接失败: " . $e->getMessage() . "</p>";
        }
        ?>
    </div>
    
    <div class="box">
        <h2>2. 检查 auto_user 表</h2>
        <?php
        try {
            $tables = $db->fetchAll("SHOW TABLES LIKE 'auto_user'");
            if (count($tables) > 0) {
                echo "<p class='success'>✓ auto_user 表存在</p>";
            } else {
                echo "<p class='error'>✗ auto_user 表不存在！需要导入 database/auto_app.sql</p>";
            }
        } catch (Exception $e) {
            echo "<p class='error'>✗ 查询失败: " . $e->getMessage() . "</p>";
        }
        ?>
    </div>
    
    <div class="box">
        <h2>3. 查询所有用户</h2>
        <?php
        try {
            $users = $db->fetchAll("SELECT user_id, username, password, salt, role, status FROM auto_user");
            if (count($users) > 0) {
                echo "<p class='info'>找到 " . count($users) . " 个用户:</p>";
                echo "<table border='1' cellpadding='5' style='border-collapse: collapse;'>";
                echo "<tr><th>ID</th><th>用户名</th><th>密码哈希</th><th>盐值</th><th>角色</th><th>状态</th></tr>";
                foreach ($users as $u) {
                    echo "<tr>";
                    echo "<td>" . $u['user_id'] . "</td>";
                    echo "<td>" . $u['username'] . "</td>";
                    echo "<td style='font-size:10px;'>" . substr($u['password'], 0, 20) . "...(" . strlen($u['password']) . "字符)</td>";
                    echo "<td style='font-size:10px;'>" . $u['salt'] . "(" . strlen($u['salt']) . "字符)</td>";
                    echo "<td>" . $u['role'] . "</td>";
                    echo "<td>" . ($u['status'] == 1 ? '启用' : '禁用') . "</td>";
                    echo "</tr>";
                }
                echo "</table>";
            } else {
                echo "<p class='error'>✗ 没有找到任何用户！</p>";
            }
        } catch (Exception $e) {
            echo "<p class='error'>✗ 查询失败: " . $e->getMessage() . "</p>";
        }
        ?>
    </div>
    
    <div class="box">
        <h2>4. admin 用户密码验证</h2>
        <?php
        $user = $db->fetch("SELECT * FROM auto_user WHERE username = 'admin'");
        if ($user) {
            $testPassword = "admin123";
            $expectedHash = hash('sha256', $testPassword . $user['salt']);
            $match = ($expectedHash === $user['password']);
            
            echo "<p><b>用户名:</b> " . $user['username'] . "</p>";
            echo "<p><b>状态:</b> " . ($user['status'] == 1 ? "<span class='success'>启用</span>" : "<span class='error'>禁用</span>") . "</p>";
            echo "<p><b>测试密码:</b> " . $testPassword . "</p>";
            echo "<p><b>数据库盐值:</b> " . $user['salt'] . "</p>";
            echo "<p><b>组合字符串:</b> " . $testPassword . $user['salt'] . "</p>";
            echo "<p><b>计算的哈希:</b><br><code>" . $expectedHash . "</code></p>";
            echo "<p><b>数据库哈希:</b><br><code>" . $user['password'] . "</code></p>";
            
            if ($match) {
                echo "<p class='success'>✓ 密码匹配！登录应该成功。</p>";
            } else {
                echo "<p class='error'>✗ 密码不匹配！</p>";
                echo "<p>正在自动修复...</p>";
                
                $db->update('auto_user', 
                    ['password' => $expectedHash],
                    'user_id = :id',
                    ['id' => $user['user_id']]
                );
                echo "<p class='success'>✓ 密码已修复！请重新尝试登录。</p>";
            }
        } else {
            echo "<p class='error'>✗ admin 用户不存在！正在创建...</p>";
            
            $salt = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
            $password = "admin123";
            $hash = hash('sha256', $password . $salt);
            
            $userId = $db->insert('auto_user', [
                'username' => 'admin',
                'password' => $hash,
                'salt' => $salt,
                'role' => 9,
                'status' => 1
            ]);
            
            echo "<p class='success'>✓ admin 用户已创建！用户ID: " . $userId . "</p>";
            echo "<p>用户名: admin</p>";
            echo "<p>密码: admin123</p>";
        }
        ?>
    </div>
    
    <div class="box">
        <h2>5. 登录信息</h2>
        <p><b>后台地址:</b> <a href="admin/index.html">/admin/index.html</a></p>
        <p><b>用户名:</b> admin</p>
        <p><b>密码:</b> admin123</p>
    </div>
</body>
</html>
