<?php
require_once __DIR__ . '/vendor/autoload.php';

use App\Core\Database;

$db = Database::getInstance();

try {
    $result = $db->fetch('SHOW TABLES LIKE "auto_screenshot"');
    if ($result) {
        echo "Table auto_screenshot exists\n\n";
        $columns = $db->fetchAll('DESCRIBE auto_screenshot');
        echo "Columns:\n";
        foreach ($columns as $col) {
            echo "  - {$col['Field']} ({$col['Type']})\n";
        }
        
        $count = $db->fetch('SELECT COUNT(*) as count FROM auto_screenshot');
        echo "\nRecord count: {$count['count']}\n";
        
        $records = $db->fetchAll('SELECT * FROM auto_screenshot ORDER BY create_time DESC LIMIT 5');
        if ($records) {
            echo "\nRecent records:\n";
            foreach ($records as $r) {
                print_r($r);
            }
        }
    } else {
        echo "Table auto_screenshot does NOT exist!\n";
        echo "Creating table...\n";
        
        $db->query("CREATE TABLE `auto_screenshot` (
          `screenshot_id` int NOT NULL AUTO_INCREMENT,
          `user_id` int NOT NULL,
          `image_path` varchar(255) NOT NULL,
          `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
          PRIMARY KEY (`screenshot_id`),
          KEY `idx_user_id` (`user_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        
        echo "Table created successfully!\n";
    }
    
    echo "\n\nChecking auto_remote_command table:\n";
    $commands = $db->fetchAll('SELECT * FROM auto_remote_command WHERE command = "take_screenshot" ORDER BY create_time DESC LIMIT 5');
    if ($commands) {
        echo "Screenshot commands:\n";
        foreach ($commands as $cmd) {
            print_r($cmd);
        }
    } else {
        echo "No screenshot commands found.\n";
    }
    
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
