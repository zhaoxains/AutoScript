<?php

namespace App\Controllers;

use App\Core\Controller;
use App\Core\Database;

class HeartbeatController extends Controller
{
    public function beat()
    {
        $user = $this->requireAuth();
        $input = $this->getJsonInput();
        
        $deviceId = $input['device_id'] ?? '';
        $status = $input['status'] ?? 0;
        $currentTask = $input['current_task'] ?? null;
        $userId = $user['user_id'];
        
        $db = Database::getInstance();
        
        $existingDevice = $db->fetch(
            "SELECT user_id FROM auto_device_heartbeat WHERE device_id = :device_id",
            ['device_id' => $deviceId]
        );
        
        if ($existingDevice && $existingDevice['user_id'] != $userId) {
            $db->query(
                "UPDATE auto_device_heartbeat SET user_id = :user_id, status = :status, current_task = :current_task, last_heartbeat = NOW() WHERE device_id = :device_id",
                [
                    'device_id' => $deviceId,
                    'user_id' => $userId,
                    'status' => $status,
                    'current_task' => $currentTask
                ]
            );
        } else {
            $db->query(
                "INSERT INTO auto_device_heartbeat (device_id, user_id, status, current_task, last_heartbeat) 
                 VALUES (:device_id, :user_id, :status, :current_task, NOW())
                 ON DUPLICATE KEY UPDATE 
                 user_id = VALUES(user_id), 
                 status = VALUES(status), 
                 current_task = VALUES(current_task), 
                 last_heartbeat = NOW()",
                [
                    'device_id' => $deviceId,
                    'user_id' => $userId,
                    'status' => $status,
                    'current_task' => $currentTask
                ]
            );
        }
        
        $commands = $db->fetchAll(
            "SELECT * FROM auto_remote_command 
             WHERE user_id = :user_id AND executed = 0 
             ORDER BY create_time ASC",
            ['user_id' => $userId]
        );
        
        $responseCommands = [];
        foreach ($commands as $cmd) {
            $responseCommands[] = [
                'command' => $cmd['command'],
                'params' => json_decode($cmd['params'], true)
            ];
            
            $db->update('auto_remote_command',
                ['executed' => 1, 'execute_time' => date('Y-m-d H:i:s')],
                'id = :id',
                ['id' => $cmd['id']]
            );
        }
        
        return $this->success([
            'server_time' => date('Y-m-d H:i:s'),
            'commands' => $responseCommands
        ]);
    }

    public function uploadScreenshot()
    {
        $user = $this->requireAuth();
        $userId = $user['user_id'];
        
        $logFile = dirname(__DIR__, 2) . '/public/logs/screenshot.log';
        $logDir = dirname($logFile);
        if (!is_dir($logDir)) {
            mkdir($logDir, 0755, true);
        }
        
        file_put_contents($logFile, date('Y-m-d H:i:s') . " - Screenshot upload request from user: $userId\n", FILE_APPEND);
        file_put_contents($logFile, date('Y-m-d H:i:s') . " - FILES: " . print_r($_FILES, true) . "\n", FILE_APPEND);
        file_put_contents($logFile, date('Y-m-d H:i:s') . " - POST: " . print_r($_POST, true) . "\n", FILE_APPEND);
        file_put_contents($logFile, date('Y-m-d H:i:s') . " - HEADERS: " . print_r(getallheaders(), true) . "\n", FILE_APPEND);
        
        if (!isset($_FILES['screenshot']) || $_FILES['screenshot']['error'] !== UPLOAD_ERR_OK) {
            $errorMsg = isset($_FILES['screenshot']) ? "Upload error code: " . $_FILES['screenshot']['error'] : "No screenshot file";
            file_put_contents($logFile, date('Y-m-d H:i:s') . " - ERROR: $errorMsg\n", FILE_APPEND);
            return $this->error('截图上传失败: ' . $errorMsg, 1001);
        }
        
        $uploadDir = dirname(__DIR__, 2) . '/public/uploads/screenshots/';
        if (!is_dir($uploadDir)) {
            mkdir($uploadDir, 0755, true);
            file_put_contents($logFile, date('Y-m-d H:i:s') . " - Created upload directory: $uploadDir\n", FILE_APPEND);
        }
        
        $filename = 'screenshot_' . $userId . '_' . time() . '.jpg';
        $filepath = $uploadDir . $filename;
        
        file_put_contents($logFile, date('Y-m-d H:i:s') . " - Target filepath: $filepath\n", FILE_APPEND);
        
        if (!move_uploaded_file($_FILES['screenshot']['tmp_name'], $filepath)) {
            file_put_contents($logFile, date('Y-m-d H:i:s') . " - ERROR: Failed to move uploaded file\n", FILE_APPEND);
            return $this->error('截图保存失败', 1002);
        }
        
        file_put_contents($logFile, date('Y-m-d H:i:s') . " - Screenshot saved successfully\n", FILE_APPEND);
        
        $db = Database::getInstance();
        
        $db->delete('auto_screenshot', 'user_id = :user_id', ['user_id' => $userId]);
        
        $screenshotId = $db->insert('auto_screenshot', [
            'user_id' => $userId,
            'image_path' => 'uploads/screenshots/' . $filename,
            'create_time' => date('Y-m-d H:i:s')
        ]);
        
        file_put_contents($logFile, date('Y-m-d H:i:s') . " - Database record created, ID: $screenshotId\n", FILE_APPEND);
        
        return $this->success([
            'screenshot_id' => $screenshotId,
            'image_path' => 'uploads/screenshots/' . $filename
        ]);
    }
}
