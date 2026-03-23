<?php
// 此文件用于生成正确的管理员密码哈希
// 访问: https://auto.cptb.cn/generate_admin_hash.php

header('Content-Type: text/plain; charset=utf-8');

$password = "admin123";
$salt = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
$combined = $password . $salt;
$hash = hash('sha256', $combined);

echo "=== 管理员密码哈希生成 ===\n\n";
echo "密码: " . $password . "\n";
echo "盐值: " . $salt . "\n";
echo "组合: " . $combined . "\n";
echo "SHA256 哈希: " . $hash . "\n";
echo "哈希长度: " . strlen($hash) . "\n\n";

echo "=== SQL 更新语句 ===\n";
echo "UPDATE `auto_user` SET `password` = '" . $hash . "', `salt` = '" . $salt . "' WHERE `username` = 'admin';\n\n";

echo "=== SQL 插入语句 ===\n";
echo "INSERT INTO `auto_user` (`username`, `password`, `salt`, `role`, `status`) VALUES\n";
echo "('admin', '" . $hash . "', '" . $salt . "', 9, 1);\n";
