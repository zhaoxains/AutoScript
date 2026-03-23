<?php

namespace App\Controllers;

use App\Core\Controller;
use App\Core\Database;
use App\Core\JWT;

class ConfigController extends Controller
{
    public function scripts()
    {
        $user = $this->requireAuth();
        
        $version = $_GET['version'] ?? null;
        
        $db = Database::getInstance();
        
        $scripts = $db->fetchAll(
            "SELECT s.* FROM auto_script s
             LEFT JOIN auto_script_bind sb ON s.script_id = sb.script_id
             WHERE s.status = 1 
             AND (sb.script_id IS NULL OR sb.bind_type = 1 AND sb.bind_value = :user_id)
             ORDER BY s.priority ASC, s.script_id ASC",
            ['user_id' => $user['user_id']]
        );
        
        $globalConfig = $db->fetchAll("SELECT * FROM auto_system_config");
        $config = [];
        foreach ($globalConfig as $row) {
            $config[$row['config_key']] = $row['config_value'];
        }
        
        $scriptsList = [];
        foreach ($scripts as $script) {
            $scriptsList[] = [
                'script_id' => (int)$script['script_id'],
                'script_name' => $script['script_name'],
                'script_desc' => $script['script_desc'],
                'target_package' => $script['target_package'],
                'config' => json_decode($script['config'], true),
                'version' => $script['version'],
                'status' => (int)$script['status'],
                'priority' => (int)$script['priority'],
                'execution_frequency' => $script['execution_frequency'] ?? 'once',
                'timeout' => (int)($script['timeout'] ?? 300)
            ];
        }
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => [
                'version' => date('YmdHis'),
                'need_update' => true,
                'scripts' => $scriptsList,
                'global_config' => [
                    'operation_delay_min' => (int)($config['operation_delay_min'] ?? 500),
                    'operation_delay_max' => (int)($config['operation_delay_max'] ?? 2000),
                    'retry_count' => (int)($config['retry_count'] ?? 3),
                    'retry_interval' => (int)($config['retry_interval'] ?? 1000),
                    'ad_default_duration' => (int)($config['ad_default_duration'] ?? 15),
                    'page_load_timeout' => (int)($config['page_load_timeout'] ?? 10),
                    'task_max_duration' => (int)($config['task_max_duration'] ?? 7200),
                    'daily_task_limit' => (int)($config['daily_task_limit'] ?? 100)
                ]
            ]
        ];
    }

    public function globalConfig()
    {
        $user = $this->requireAuth();
        
        $db = Database::getInstance();
        $config = $db->fetchAll("SELECT * FROM auto_system_config");
        
        $result = [];
        foreach ($config as $row) {
            $result[$row['config_key']] = $row['config_value'];
        }
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => [
                'operation_delay_min' => (int)($result['operation_delay_min'] ?? 500),
                'operation_delay_max' => (int)($result['operation_delay_max'] ?? 2000),
                'retry_count' => (int)($result['retry_count'] ?? 3),
                'retry_interval' => (int)($result['retry_interval'] ?? 1000),
                'ad_default_duration' => (int)($result['ad_default_duration'] ?? 15),
                'page_load_timeout' => (int)($result['page_load_timeout'] ?? 10)
            ]
        ];
    }
}
