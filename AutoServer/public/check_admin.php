<?php
require_once __DIR__ . '/../vendor/autoload.php';

$config = require __DIR__ . '/../config/app.php';
App\Core\Database::init($config['database']);

$db = App\Core\Database::getInstance();

echo "=== 查询 admin 用户信息 ===\n\n";

$user = $db->fetch(
    "SELECT user_id, username, password, salt, role, status FROM auto_user WHERE username = :username",
    ['username' => 'admin']
);

if ($user) {
    echo "用户ID: " . $user['user_id'] . "\n";
    echo "用户名: " . $user['username'] . "\n";
    echo "密码哈希: " . $user['password'] . "\n";
    echo "密码哈希长度: " . strlen($user['password']) . "\n";
    echo "盐值: " . $user['salt'] . "\n";
    echo "盐值长度: " . strlen($user['salt']) . "\n";
    echo "角色: " . $user['role'] . "\n";
    echo "状态: " . $user['status'] . "\n\n";
    
    // 验证密码
    $testPassword = "admin123";
    $expectedHash = hash('sha256', $testPassword . $user['salt']);
    echo "=== 密码验证 ===\n";
    echo "测试密码: " . $testPassword . "\n";
    echo "计算的哈希: " . $expectedHash . "\n";
    echo "存储的哈希: " . $user['password'] . "\n";
    echo "匹配结果: " . ($expectedHash === $user['password'] ? "匹配" : "不匹配") . "\n";
    
    if ($expectedHash !== $user['password']) {
        echo "\n=== 修复密码 ===\n";
        echo "执行以下 SQL 修复密码:\n";
        echo "UPDATE `auto_user` SET `password` = '" . $expectedHash . "' WHERE `username` = 'admin';\n";
    }
} else {
    echo "未找到 admin 用户！\n";
    echo "\n创建 admin 用户:\n";
    $salt = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
    $password = "admin123";
    $hash = hash('sha256', $password . $salt);
    echo "INSERT INTO `auto_user` (`username`, `password`, `salt`, `role`, `status`) VALUES\n";
    echo "('admin', '" . $hash . "', '" . $salt . "', 9, 1);\n";
}
