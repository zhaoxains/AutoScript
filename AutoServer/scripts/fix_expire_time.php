<?php

require_once __DIR__ . '/../app/Core/Database.php';

use App\Core\Database;

$db = Database::getInstance();

$users = $db->fetchAll("SELECT user_id, username, expire_time FROM auto_user");

echo "当前用户列表:\n";
foreach ($users as $user) {
    echo "ID: {$user['user_id']}, 用户名: {$user['username']}, 到期时间: {$user['expire_time']}\n";
}

echo "\n是否要更新所有用户的到期时间为一年后? (y/n): ";
$handle = fopen("php://stdin", "r");
$line = fgets($handle);
if (trim(strtolower($line)) === 'y') {
    $result = $db->update(
        'auto_user',
        ['expire_time' => date('Y-m-d H:i:s', strtotime('+1 year'))],
        '1=1',
        []
    );
    echo "已更新 {$result} 个用户的到期时间\n";
    
    $users = $db->fetchAll("SELECT user_id, username, expire_time FROM auto_user");
    echo "\n更新后的用户列表:\n";
    foreach ($users as $user) {
        echo "ID: {$user['user_id']}, 用户名: {$user['username']}, 到期时间: {$user['expire_time']}\n";
    }
} else {
    echo "已取消更新\n";
}

fclose($handle);
