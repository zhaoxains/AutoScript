<?php

namespace App\Controllers;

use App\Core\Database;
use App\Core\JWT;

class ReportController
{
    public function task()
    {
        $user = $this->requireAuth();
        $input = json_decode(file_get_contents('php://input'), true);
        
        $db = Database::getInstance();
        
        $recordId = $db->insert('auto_task_record', [
            'user_id' => $user['user_id'],
            'device_id' => $input['device_id'] ?? '',
            'script_id' => $input['script_id'] ?? 0,
            'task_name' => $input['task_name'] ?? '',
            'start_time' => $input['start_time'] ?? date('Y-m-d H:i:s'),
            'end_time' => $input['end_time'] ?? null,
            'status' => $input['status'] ?? 0,
            'duration' => $input['duration'] ?? 0,
            'error_code' => $input['error_code'] ?? null,
            'error_msg' => $input['error_msg'] ?? null,
            'task_extend' => isset($input['task_extend']) ? json_encode($input['task_extend']) : null,
            'create_time' => date('Y-m-d H:i:s')
        ]);
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => [
                'record_id' => $recordId
            ]
        ];
    }

    public function logs()
    {
        $user = $this->requireAuth();
        $input = json_decode(file_get_contents('php://input'), true);
        
        $logFile = dirname(__DIR__, 2) . '/public/logs/report_logs.log';
        $logDir = dirname($logFile);
        if (!is_dir($logDir)) {
            mkdir($logDir, 0755, true);
        }
        
        file_put_contents($logFile, date('Y-m-d H:i:s') . " - Logs report request from user: " . $user['user_id'] . "\n", FILE_APPEND);
        file_put_contents($logFile, date('Y-m-d H:i:s') . " - Input: " . json_encode($input) . "\n", FILE_APPEND);
        
        $logs = $input['logs'] ?? [];
        
        if (empty($logs)) {
            file_put_contents($logFile, date('Y-m-d H:i:s') . " - No logs to report\n", FILE_APPEND);
            return [
                'code' => 0,
                'msg' => 'success',
                'data' => [
                    'success_count' => 0,
                    'fail_count' => 0
                ]
            ];
        }
        
        $db = Database::getInstance();
        $successCount = 0;
        $failCount = 0;
        
        foreach ($logs as $log) {
            try {
                $db->insert('auto_execution_log', [
                    'user_id' => $user['user_id'],
                    'task_id' => $log['task_id'] ?? 0,
                    'log_type' => $log['log_type'] ?? '',
                    'log_time' => $log['log_time'] ?? date('Y-m-d H:i:s'),
                    'log_content' => $log['log_content'] ?? '',
                    'detail' => $log['detail'] ?? null,
                    'create_time' => date('Y-m-d H:i:s')
                ]);
                $successCount++;
            } catch (\Exception $e) {
                $failCount++;
                file_put_contents($logFile, date('Y-m-d H:i:s') . " - Error: " . $e->getMessage() . "\n", FILE_APPEND);
            }
        }
        
        file_put_contents($logFile, date('Y-m-d H:i:s') . " - Success: $successCount, Fail: $failCount\n", FILE_APPEND);
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => [
                'success_count' => $successCount,
                'fail_count' => $failCount
            ]
        ];
    }

    private function requireAuth()
    {
        $headers = $this->getHeaders();
        $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? null;
        
        if (!$authHeader || !preg_match('/Bearer\s+(.+)/', $authHeader, $matches)) {
            throw new \Exception('认证失败', 1002);
        }
        
        $token = $matches[1];
        $payload = JWT::decode($token);
        
        if (!$payload) {
            throw new \Exception('认证失败', 1002);
        }
        
        return $payload;
    }

    private function getHeaders()
    {
        if (function_exists('getallheaders')) {
            return getallheaders();
        }
        
        $headers = [];
        foreach ($_SERVER as $name => $value) {
            if (substr($name, 0, 5) == 'HTTP_') {
                $headers[str_replace(' ', '-', ucwords(strtolower(str_replace('_', ' ', substr($name, 5)))))] = $value;
            }
        }
        return $headers;
    }
}
