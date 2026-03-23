<?php
require_once __DIR__ . '/../vendor/autoload.php';

$config = require __DIR__ . '/../config/app.php';
App\Core\Database::init($config['database']);

$db = App\Core\Database::getInstance();

header('Content-Type: text/plain; charset=utf-8');

echo "=== AutoAdmin 密码诊断工具 ===\n\n";

// 1. 查询数据库中的 admin 用户
$user = $db->fetch(
    "SELECT * FROM auto_user WHERE username = :username",
    ['username' => 'admin']
);

if ($user) {
    echo "【数据库中的 admin 用户信息】\n";
    echo "用户ID: " . $user['user_id'] . "\n";
    echo "用户名: " . $user['username'] . "\n";
    echo "密码哈希: " . $user['password'] . "\n";
    echo "密码哈希长度: " . strlen($user['password']) . " (应为64)\n";
    echo "盐值: " . $user['salt'] . "\n";
    echo "盐值长度: " . strlen($user['salt']) . " (应为32)\n";
    echo "角色: " . $user['role'] . "\n";
    echo "状态: " . ($user['status'] == 1 ? "启用" : "禁用") . "\n\n";
    
    // 2. 测试密码验证
    $testPassword = "admin123";
    $expectedHash = hash('sha256', $testPassword . $user['salt']);
    
    echo "【密码验证测试】\n";
    echo "测试密码: " . $testPassword . "\n";
    echo "数据库盐值: " . $user['salt'] . "\n";
    echo "组合字符串: " . $testPassword . $user['salt'] . "\n";
    echo "计算的哈希: " . $expectedHash . "\n";
    echo "数据库哈希: " . $user['password'] . "\n";
    echo "哈希匹配: " . ($expectedHash === $user['password'] ? "✓ 匹配" : "✗ 不匹配") . "\n\n";
    
    if ($expectedHash !== $user['password']) {
        echo "【修复 SQL】\n";
        echo "执行以下 SQL 修复密码:\n\n";
        echo "UPDATE `auto_user` SET `password` = '" . $expectedHash . "' WHERE `username` = 'admin';\n\n";
        
        // 自动修复
        echo "【自动修复】\n";
        $db->update('auto_user', 
            ['password' => $expectedHash],
            'username = :username',
            ['username' => 'admin']
        );
        echo "密码已自动修复！请重新尝试登录。\n";
    }
} else {
    echo "【admin 用户不存在】\n";
    echo "正在创建 admin 用户...\n";
    
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
    
    echo "admin 用户已创建！\n";
    echo "用户ID: " . $userId . "\n";
    echo "用户名: admin\n";
    echo "密码: admin123\n";
}
