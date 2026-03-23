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
    <title>清除设备绑定</title>
    <style>
        body { font-family: Arial, sans-serif; padding: 20px; background: #f5f5f5; }
        .box { background: white; padding: 20px; margin: 10px 0; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .success { color: green; }
        .error { color: red; }
        .btn { display: inline-block; padding: 10px 20px; background: #1976D2; color: white; text-decoration: none; border-radius: 4px; }
        .btn:hover { background: #1565C0; }
    </style>
</head>
<body>
    <div class="box">
        <h1>清除设备绑定</h1>
        <?php
        // 清除所有用户的设备绑定
        $result = $db->update('auto_user', ['device_id' => null], '1=1', []);
        
        echo "<p class='success'>✓ 已清除所有用户的设备绑定！</p>";
        echo "<p>现在可以在新设备上登录了。</p>";
        
        // 显示更新后的用户状态
        $users = $db->fetchAll("SELECT user_id, username, device_id, role FROM auto_user");
        echo "<h3>用户列表：</h3>";
        echo "<table border='1' cellpadding='8' style='border-collapse: collapse;'>";
        echo "<tr><th>ID</th><th>用户名</th><th>设备ID</th><th>角色</th></tr>";
        foreach ($users as $u) {
            echo "<tr>";
            echo "<td>" . $u['user_id'] . "</td>";
            echo "<td>" . $u['username'] . "</td>";
            echo "<td>" . ($u['device_id'] ?: '未绑定') . "</td>";
            echo "<td>" . $u['role'] . "</td>";
            echo "</tr>";
        }
        echo "</table>";
        ?>
        <p style="margin-top: 20px;">
            <a href="admin/index.html" class="btn">前往登录</a>
        </p>
    </div>
</body>
</html>
