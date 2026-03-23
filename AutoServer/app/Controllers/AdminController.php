<?php

namespace App\Controllers;

use App\Core\Controller;
use App\Core\Database;
use App\Core\JWT;

class AdminController extends Controller
{
    public function users()
    {
        $user = $this->requireAdmin();
        
        $page = (int)($_GET['page'] ?? 1);
        $pageSize = (int)($_GET['page_size'] ?? 20);
        $offset = ($page - 1) * $pageSize;
        
        $db = Database::getInstance();
        
        $where = "1=1";
        $params = [];
        
        if (!empty($_GET['username'])) {
            $where .= " AND username LIKE :username";
            $params['username'] = '%' . $_GET['username'] . '%';
        }
        
        if (isset($_GET['status']) && $_GET['status'] !== '') {
            $where .= " AND status = :status";
            $params['status'] = (int)$_GET['status'];
        }
        
        $total = $db->fetch(
            "SELECT COUNT(*) as count FROM auto_user WHERE {$where}",
            $params
        )['count'];
        
        $users = $db->fetchAll(
            "SELECT u.user_id, u.username, u.device_id, u.expire_time, u.status, u.role, u.create_time, u.update_time, u.referrer_id, r.username as referrer_name
             FROM auto_user u 
             LEFT JOIN auto_user r ON u.referrer_id = r.user_id
             WHERE {$where} 
             ORDER BY u.user_id DESC 
             LIMIT {$offset}, {$pageSize}",
            $params
        );
        
        $usersWithData = array_map(function($u) use ($db) {
            $stats = $db->fetch(
                "SELECT 
                    COUNT(*) as total_tasks,
                    SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as success_count,
                    SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as fail_count,
                    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as running_count
                FROM auto_task_record WHERE user_id = :user_id",
                ['user_id' => $u['user_id']]
            );
            
            return [
                'user_id' => (int)$u['user_id'],
                'username' => $u['username'],
                'device_id' => $u['device_id'],
                'expire_time' => $u['expire_time'],
                'status' => (int)$u['status'],
                'role' => (int)$u['role'],
                'referrer_id' => $u['referrer_id'] ? (int)$u['referrer_id'] : null,
                'referrer_name' => $u['referrer_name'],
                'create_time' => $u['create_time'],
                'update_time' => $u['update_time'],
                'task_stats' => [
                    'total' => (int)($stats['total_tasks'] ?? 0),
                    'success' => (int)($stats['success_count'] ?? 0),
                    'fail' => (int)($stats['fail_count'] ?? 0),
                    'running' => (int)($stats['running_count'] ?? 0)
                ]
            ];
        }, $users);
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => [
                'total' => (int)$total,
                'page' => $page,
                'page_size' => $pageSize,
                'list' => $usersWithData
            ]
        ];
    }

    public function userRecords($userId)
    {
        $this->requireAdmin();
        
        $page = (int)($_GET['page'] ?? 1);
        $pageSize = (int)($_GET['page_size'] ?? 20);
        $offset = ($page - 1) * $pageSize;
        $scriptId = $_GET['script_id'] ?? null;
        $status = $_GET['status'] ?? null;
        
        $db = Database::getInstance();
        
        $where = "r.user_id = :user_id";
        $params = ['user_id' => (int)$userId];
        
        if ($scriptId) {
            $where .= " AND r.script_id = :script_id";
            $params['script_id'] = (int)$scriptId;
        }
        
        if ($status !== null && $status !== '') {
            $where .= " AND r.status = :status";
            $params['status'] = (int)$status;
        }
        
        $total = $db->fetch(
            "SELECT COUNT(*) as count FROM auto_task_record r WHERE {$where}",
            $params
        )['count'];
        
        $records = $db->fetchAll(
            "SELECT r.*, s.script_name 
             FROM auto_task_record r
             LEFT JOIN auto_script s ON r.script_id = s.script_id
             WHERE {$where}
             ORDER BY r.id DESC
             LIMIT {$offset}, {$pageSize}",
            $params
        );
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => [
                'total' => (int)$total,
                'page' => $page,
                'page_size' => $pageSize,
                'list' => array_map(function($r) {
                    return [
                        'id' => (int)$r['id'],
                        'script_id' => (int)$r['script_id'],
                        'script_name' => $r['script_name'],
                        'task_name' => $r['task_name'],
                        'start_time' => $r['start_time'],
                        'end_time' => $r['end_time'],
                        'status' => (int)$r['status'],
                        'duration' => (int)$r['duration'],
                        'error_msg' => $r['error_msg']
                    ];
                }, $records)
            ]
        ];
    }

    public function createUser()
    {
        $this->requireAdmin();
        $input = json_decode(file_get_contents('php://input'), true);
        
        $username = $input['username'] ?? '';
        $password = $input['password'] ?? '';
        $role = (int)($input['role'] ?? 1);
        $expireTime = $input['expire_time'] ?? null;
        
        if (empty($username) || empty($password)) {
            return ['code' => 1001, 'msg' => '用户名和密码不能为空', 'data' => null];
        }
        
        $db = Database::getInstance();
        
        $exists = $db->fetch(
            "SELECT user_id FROM auto_user WHERE username = :username",
            ['username' => $username]
        );
        
        if ($exists) {
            return ['code' => 1001, 'msg' => '用户名已存在', 'data' => null];
        }
        
        $salt = bin2hex(random_bytes(16));
        $passwordHash = hash('sha256', $password . $salt);
        
        $userId = $db->insert('auto_user', [
            'username' => $username,
            'password' => $passwordHash,
            'salt' => $salt,
            'role' => $role,
            'expire_time' => $expireTime,
            'status' => 1,
            'create_time' => date('Y-m-d H:i:s')
        ]);
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => ['user_id' => $userId]
        ];
    }

    public function updateUser($id)
    {
        $this->requireAdmin();
        $input = json_decode(file_get_contents('php://input'), true);
        
        $db = Database::getInstance();
        $data = ['update_time' => date('Y-m-d H:i:s')];
        
        if (isset($input['username']) && !empty($input['username'])) {
            $existingUser = $db->fetch(
                "SELECT user_id FROM auto_user WHERE username = :username AND user_id != :id",
                ['username' => $input['username'], 'id' => $id]
            );
            if ($existingUser) {
                return ['code' => 1006, 'msg' => '用户名已存在', 'data' => null];
            }
            $data['username'] = $input['username'];
        }
        
        if (isset($input['password']) && !empty($input['password'])) {
            $salt = bin2hex(random_bytes(16));
            $passwordToHash = $input['password'];
            if (strlen($passwordToHash) !== 32 || !ctype_xdigit($passwordToHash)) {
                $passwordToHash = md5($passwordToHash);
            }
            $data['password'] = hash('sha256', $passwordToHash . $salt);
            $data['salt'] = $salt;
            
            $logFile = dirname(__DIR__, 2) . '/public/logs/password_update.log';
            $logDir = dirname($logFile);
            if (!is_dir($logDir)) {
                mkdir($logDir, 0755, true);
            }
            file_put_contents($logFile, date('Y-m-d H:i:s') . " - Update password for user $id\n", FILE_APPEND);
            file_put_contents($logFile, "Input password: " . $input['password'] . "\n", FILE_APPEND);
            file_put_contents($logFile, "MD5 password: $passwordToHash\n", FILE_APPEND);
            file_put_contents($logFile, "Salt: $salt\n", FILE_APPEND);
            file_put_contents($logFile, "Hash: " . $data['password'] . "\n\n", FILE_APPEND);
        }
        
        if (isset($input['role'])) $data['role'] = (int)$input['role'];
        if (isset($input['status'])) $data['status'] = (int)$input['status'];
        if (isset($input['expire_time'])) $data['expire_time'] = $input['expire_time'];
        if (isset($input['device_id'])) $data['device_id'] = $input['device_id'];
        
        $db->update('auto_user', $data, 'user_id = :id', ['id' => $id]);
        
        return ['code' => 0, 'msg' => 'success', 'data' => null];
    }

    public function deleteUser($id)
    {
        $this->requireAdmin();
        $db = Database::getInstance();
        $db->delete('auto_user', 'user_id = :id', ['id' => $id]);
        return ['code' => 0, 'msg' => 'success', 'data' => null];
    }

    public function scripts()
    {
        $this->requireAdmin();
        
        $page = (int)($_GET['page'] ?? 1);
        $pageSize = (int)($_GET['page_size'] ?? 20);
        $offset = ($page - 1) * $pageSize;
        
        $db = Database::getInstance();
        
        $total = $db->fetch("SELECT COUNT(*) as count FROM auto_script")['count'];
        
        $scripts = $db->fetchAll(
            "SELECT * FROM auto_script ORDER BY script_id DESC LIMIT {$offset}, {$pageSize}"
        );
        
        $scriptsWithData = array_map(function($s) use ($db) {
            $stats = $db->fetch(
                "SELECT 
                    COUNT(*) as total_tasks,
                    SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as success_count,
                    SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as fail_count,
                    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as running_count,
                    COUNT(DISTINCT user_id) as user_count
                FROM auto_task_record WHERE script_id = :script_id",
                ['script_id' => $s['script_id']]
            );
            
            return [
                'script_id' => (int)$s['script_id'],
                'script_name' => $s['script_name'],
                'script_desc' => $s['script_desc'],
                'target_package' => $s['target_package'],
                'config' => json_decode($s['config'], true),
                'version' => $s['version'],
                'status' => (int)$s['status'],
                'priority' => (int)$s['priority'],
                'create_time' => $s['create_time'],
                'update_time' => $s['update_time'],
                'task_stats' => [
                    'total' => (int)($stats['total_tasks'] ?? 0),
                    'success' => (int)($stats['success_count'] ?? 0),
                    'fail' => (int)($stats['fail_count'] ?? 0),
                    'running' => (int)($stats['running_count'] ?? 0),
                    'user_count' => (int)($stats['user_count'] ?? 0)
                ]
            ];
        }, $scripts);
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => [
                'total' => (int)$total,
                'page' => $page,
                'page_size' => $pageSize,
                'list' => $scriptsWithData
            ]
        ];
    }

    public function scriptRecords($scriptId)
    {
        $this->requireAdmin();
        
        $page = (int)($_GET['page'] ?? 1);
        $pageSize = (int)($_GET['page_size'] ?? 20);
        $offset = ($page - 1) * $pageSize;
        $userId = $_GET['user_id'] ?? null;
        $status = $_GET['status'] ?? null;
        
        $db = Database::getInstance();
        
        $where = "r.script_id = :script_id";
        $params = ['script_id' => (int)$scriptId];
        
        if ($userId) {
            $where .= " AND r.user_id = :user_id";
            $params['user_id'] = (int)$userId;
        }
        
        if ($status !== null && $status !== '') {
            $where .= " AND r.status = :status";
            $params['status'] = (int)$status;
        }
        
        $total = $db->fetch(
            "SELECT COUNT(*) as count FROM auto_task_record r WHERE {$where}",
            $params
        )['count'];
        
        $records = $db->fetchAll(
            "SELECT r.*, u.username 
             FROM auto_task_record r
             LEFT JOIN auto_user u ON r.user_id = u.user_id
             WHERE {$where}
             ORDER BY r.id DESC
             LIMIT {$offset}, {$pageSize}",
            $params
        );
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => [
                'total' => (int)$total,
                'page' => $page,
                'page_size' => $pageSize,
                'list' => array_map(function($r) {
                    return [
                        'id' => (int)$r['id'],
                        'user_id' => (int)$r['user_id'],
                        'username' => $r['username'],
                        'device_id' => $r['device_id'],
                        'task_name' => $r['task_name'],
                        'start_time' => $r['start_time'],
                        'end_time' => $r['end_time'],
                        'status' => (int)$r['status'],
                        'duration' => (int)$r['duration'],
                        'error_msg' => $r['error_msg']
                    ];
                }, $records)
            ]
        ];
    }

    public function createScript()
    {
        $this->requireAdmin();
        $input = json_decode(file_get_contents('php://input'), true);
        
        $scriptName = trim($input['script_name'] ?? '');
        $targetPackage = trim($input['target_package'] ?? '');
        
        if (empty($scriptName)) {
            return ['code' => 1001, 'msg' => '脚本名称不能为空', 'data' => null];
        }
        
        if (empty($targetPackage)) {
            return ['code' => 1001, 'msg' => '目标包名不能为空', 'data' => null];
        }
        
        if (!preg_match('/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/i', $targetPackage)) {
            return ['code' => 1001, 'msg' => '目标包名格式不正确', 'data' => null];
        }
        
        $config = $input['config'] ?? [];
        if (!is_array($config)) {
            return ['code' => 1001, 'msg' => '脚本配置格式不正确', 'data' => null];
        }
        
        $db = Database::getInstance();
        
        $existingScript = $db->fetch(
            "SELECT script_id FROM auto_script WHERE script_name = :name",
            ['name' => $scriptName]
        );
        
        if ($existingScript) {
            return ['code' => 1007, 'msg' => '脚本名称已存在', 'data' => null];
        }
        
        $scriptId = $db->insert('auto_script', [
            'script_name' => $scriptName,
            'script_desc' => trim($input['script_desc'] ?? ''),
            'target_package' => $targetPackage,
            'config' => json_encode($config, JSON_UNESCAPED_UNICODE),
            'version' => '1.0.0',
            'status' => (int)($input['status'] ?? 0),
            'priority' => max(1, min(5, (int)($input['priority'] ?? 2))),
            'execution_frequency' => in_array($input['execution_frequency'] ?? 'once', ['once', 'interval', 'daily']) 
                ? $input['execution_frequency'] 
                : 'once',
            'timeout' => max(60, min(3600, (int)($input['timeout'] ?? 300))),
            'create_time' => date('Y-m-d H:i:s')
        ]);
        
        return ['code' => 0, 'msg' => 'success', 'data' => ['script_id' => $scriptId]];
    }

    public function updateScript($id)
    {
        $this->requireAdmin();
        $input = json_decode(file_get_contents('php://input'), true);
        
        $id = (int)$id;
        if ($id <= 0) {
            return ['code' => 1001, 'msg' => '无效的脚本ID', 'data' => null];
        }
        
        $db = Database::getInstance();
        $data = ['update_time' => date('Y-m-d H:i:s')];
        
        if (isset($input['script_name'])) {
            $scriptName = trim($input['script_name']);
            if (empty($scriptName)) {
                return ['code' => 1001, 'msg' => '脚本名称不能为空', 'data' => null];
            }
            $existingScript = $db->fetch(
                "SELECT script_id FROM auto_script WHERE script_name = :name AND script_id != :id",
                ['name' => $scriptName, 'id' => $id]
            );
            if ($existingScript) {
                return ['code' => 1007, 'msg' => '脚本名称已存在', 'data' => null];
            }
            $data['script_name'] = $scriptName;
        }
        
        if (isset($input['target_package'])) {
            $targetPackage = trim($input['target_package']);
            if (!preg_match('/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/i', $targetPackage)) {
                return ['code' => 1001, 'msg' => '目标包名格式不正确', 'data' => null];
            }
            $data['target_package'] = $targetPackage;
        }
        
        if (isset($input['script_desc'])) $data['script_desc'] = trim($input['script_desc']);
        if (isset($input['status'])) $data['status'] = (int)$input['status'];
        if (isset($input['priority'])) $data['priority'] = (int)$input['priority'];
        
        if (isset($input['config'])) {
            $data['config'] = json_encode($input['config']);
            $current = $db->fetch("SELECT version FROM auto_script WHERE script_id = :id", ['id' => $id]);
            $data['version'] = $this->incrementVersion($current['version'] ?? '1.0.0');
        }
        
        $db->update('auto_script', $data, 'script_id = :id', ['id' => $id]);
        
        return ['code' => 0, 'msg' => 'success', 'data' => null];
    }

    public function deleteScript($id)
    {
        $this->requireAdmin();
        $db = Database::getInstance();
        $db->delete('auto_script', 'script_id = :id', ['id' => $id]);
        return ['code' => 0, 'msg' => 'success', 'data' => null];
    }

    public function records()
    {
        $this->requireAdmin();
        
        $page = (int)($_GET['page'] ?? 1);
        $pageSize = (int)($_GET['page_size'] ?? 20);
        $offset = ($page - 1) * $pageSize;
        
        $db = Database::getInstance();
        
        $where = "1=1";
        $params = [];
        
        if (!empty($_GET['username'])) {
            $where .= " AND u.username LIKE :username";
            $params['username'] = '%' . $_GET['username'] . '%';
        }
        
        if (!empty($_GET['user_id'])) {
            $where .= " AND r.user_id = :user_id";
            $params['user_id'] = (int)$_GET['user_id'];
        }
        
        if (!empty($_GET['script_id'])) {
            $where .= " AND r.script_id = :script_id";
            $params['script_id'] = (int)$_GET['script_id'];
        }
        
        if (isset($_GET['status']) && $_GET['status'] !== '') {
            $where .= " AND r.status = :status";
            $params['status'] = (int)$_GET['status'];
        }
        
        if (!empty($_GET['start_time'])) {
            $where .= " AND r.start_time >= :start_time";
            $params['start_time'] = $_GET['start_time'];
        }
        
        if (!empty($_GET['end_time'])) {
            $where .= " AND r.start_time <= :end_time";
            $params['end_time'] = $_GET['end_time'];
        }
        
        if (!empty($_GET['keyword'])) {
            $where .= " AND (r.task_name LIKE :keyword OR r.error_msg LIKE :keyword)";
            $params['keyword'] = '%' . $_GET['keyword'] . '%';
        }
        
        $total = $db->fetch(
            "SELECT COUNT(*) as count FROM auto_task_record r 
             LEFT JOIN auto_user u ON r.user_id = u.user_id
             WHERE {$where}",
            $params
        )['count'];
        
        $records = $db->fetchAll(
            "SELECT r.*, u.username, s.script_name 
             FROM auto_task_record r
             LEFT JOIN auto_user u ON r.user_id = u.user_id
             LEFT JOIN auto_script s ON r.script_id = s.script_id
             WHERE {$where}
             ORDER BY r.id DESC
             LIMIT {$offset}, {$pageSize}",
            $params
        );
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => [
                'total' => (int)$total,
                'page' => $page,
                'page_size' => $pageSize,
                'list' => array_map(function($r) {
                    return [
                        'id' => (int)$r['id'],
                        'user_id' => (int)$r['user_id'],
                        'username' => $r['username'],
                        'device_id' => $r['device_id'],
                        'script_id' => (int)$r['script_id'],
                        'script_name' => $r['script_name'],
                        'task_name' => $r['task_name'],
                        'start_time' => $r['start_time'],
                        'end_time' => $r['end_time'],
                        'status' => (int)$r['status'],
                        'duration' => (int)$r['duration'],
                        'error_code' => $r['error_code'],
                        'error_msg' => $r['error_msg'],
                        'task_extend' => json_decode($r['task_extend'], true),
                        'create_time' => $r['create_time']
                    ];
                }, $records)
            ]
        ];
    }

    public function exportRecords()
    {
        $this->requireAdmin();
        
        header('Content-Type: text/csv; charset=utf-8');
        header('Content-Disposition: attachment; filename=records_' . date('YmdHis') . '.csv');
        
        echo chr(0xEF).chr(0xBB).chr(0xBF);
        echo "ID,用户名,设备ID,脚本名称,任务名称,开始时间,结束时间,状态,耗时(秒),错误信息\n";
        
        $db = Database::getInstance();
        
        $where = "1=1";
        $params = [];
        
        if (!empty($_GET['user_id'])) {
            $where .= " AND r.user_id = :user_id";
            $params['user_id'] = (int)$_GET['user_id'];
        }
        if (!empty($_GET['script_id'])) {
            $where .= " AND r.script_id = :script_id";
            $params['script_id'] = (int)$_GET['script_id'];
        }
        if (isset($_GET['status']) && $_GET['status'] !== '') {
            $where .= " AND r.status = :status";
            $params['status'] = (int)$_GET['status'];
        }
        
        $records = $db->fetchAll(
            "SELECT r.*, u.username, s.script_name 
             FROM auto_task_record r
             LEFT JOIN auto_user u ON r.user_id = u.user_id
             LEFT JOIN auto_script s ON r.script_id = s.script_id
             WHERE {$where}
             ORDER BY r.id DESC LIMIT 10000",
            $params
        );
        
        foreach ($records as $r) {
            echo implode(',', [
                $r['id'],
                $r['username'],
                $r['device_id'],
                $r['script_name'],
                $r['task_name'],
                $r['start_time'],
                $r['end_time'],
                $this->getStatusText($r['status']),
                $r['duration'],
                $r['error_msg']
            ]) . "\n";
        }
        exit;
    }

    public function dashboard()
    {
        $this->requireAdmin();
        
        $db = Database::getInstance();
        
        $userCount = $db->fetch("SELECT COUNT(*) as count FROM auto_user")['count'];
        $scriptCount = $db->fetch("SELECT COUNT(*) as count FROM auto_script WHERE status = 1")['count'];
        $todayRecords = $db->fetch(
            "SELECT COUNT(*) as count FROM auto_task_record WHERE DATE(start_time) = CURDATE()"
        )['count'];
        $successRate = $db->fetch(
            "SELECT 
                SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as success,
                COUNT(*) as total
            FROM auto_task_record WHERE DATE(start_time) = CURDATE()"
        );
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => [
                'user_count' => (int)$userCount,
                'script_count' => (int)$scriptCount,
                'today_tasks' => (int)$todayRecords,
                'success_rate' => $successRate['total'] > 0 
                    ? round($successRate['success'] / $successRate['total'] * 100, 1) 
                    : 0
            ]
        ];
    }

    private function requireAdmin()
    {
        $headers = $this->getHeaders();
        $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? null;
        
        if (!$authHeader || !preg_match('/Bearer\s+(.+)/', $authHeader, $matches)) {
            throw new \Exception('认证失败', 1002);
        }
        
        $token = $matches[1];
        $payload = JWT::decode($token);
        
        if (!$payload) {
            throw new \Exception('无效的Token', 1002);
        }
        
        if (!isset($payload['role']) || !in_array($payload['role'], [1, 9])) {
            throw new \Exception('权限不足', 1003);
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

    private function incrementVersion($version)
    {
        $parts = explode('.', $version);
        $parts[2] = (int)($parts[2] ?? 0) + 1;
        return implode('.', $parts);
    }

    private function getStatusText($status)
    {
        $texts = [0 => '待执行', 1 => '执行中', 2 => '成功', 3 => '失败', 4 => '中断'];
        return $texts[$status] ?? '未知';
    }

    public function sendCommand()
    {
        $this->requireAdmin();
        $input = json_decode(file_get_contents('php://input'), true);
        
        $userId = $input['user_id'] ?? null;
        $command = $input['command'] ?? null;
        $params = $input['params'] ?? [];
        
        if (!$userId || !$command) {
            return ['code' => 1, 'msg' => '参数不完整'];
        }
        
        $db = Database::getInstance();
        
        $db->insert('auto_remote_command', [
            'user_id' => $userId,
            'device_id' => '',
            'command' => $command,
            'params' => json_encode($params),
            'executed' => 0,
            'create_time' => date('Y-m-d H:i:s')
        ]);
        
        return ['code' => 0, 'msg' => 'success'];
    }

    public function executeScripts()
    {
        $this->requireAdmin();
        $input = json_decode(file_get_contents('php://input'), true);
        
        $scriptIds = $input['script_ids'] ?? [];
        $userId = $input['user_id'] ?? null;
        
        if (empty($scriptIds)) {
            return ['code' => 1, 'msg' => '请选择要执行的脚本'];
        }
        
        if (!$userId) {
            return ['code' => 1, 'msg' => '请选择用户'];
        }
        
        $db = Database::getInstance();
        
        $db->query(
            "UPDATE auto_remote_command SET executed = 1 WHERE user_id = :user_id AND command = 'execute_script' AND executed = 0",
            ['user_id' => $userId]
        );
        
        foreach ($scriptIds as $scriptId) {
            $db->insert('auto_remote_command', [
                'user_id' => $userId,
                'device_id' => '',
                'command' => 'execute_script',
                'params' => json_encode(['script_id' => $scriptId]),
                'executed' => 0,
                'create_time' => date('Y-m-d H:i:s')
            ]);
        }
        
        return ['code' => 0, 'msg' => 'success', 'data' => ['count' => count($scriptIds)]];
    }

    public function stopExecution()
    {
        $this->requireAdmin();
        $input = json_decode(file_get_contents('php://input'), true);
        
        $userId = $input['user_id'] ?? null;
        
        if (!$userId) {
            return ['code' => 1, 'msg' => '用户ID不能为空'];
        }
        
        $db = Database::getInstance();
        
        $db->insert('auto_remote_command', [
            'user_id' => $userId,
            'device_id' => '',
            'command' => 'stop_execution',
            'params' => json_encode([]),
            'executed' => 0,
            'create_time' => date('Y-m-d H:i:s')
        ]);
        
        return ['code' => 0, 'msg' => 'success'];
    }

    public function getOnlineUsers()
    {
        $this->requireAdmin();
        
        $db = Database::getInstance();
        
        $users = $db->fetchAll(
            "SELECT h.*, u.username, u.role, u.expire_time
             FROM auto_device_heartbeat h 
             LEFT JOIN auto_user u ON h.user_id = u.user_id 
             WHERE h.last_heartbeat > DATE_SUB(NOW(), INTERVAL 5 MINUTE)
             ORDER BY h.last_heartbeat DESC"
        );
        
        $result = array_map(function($u) {
            $isExpired = $u['expire_time'] && strtotime($u['expire_time']) < time();
            return [
                'user_id' => $u['user_id'],
                'username' => $u['username'],
                'device_id' => $u['device_id'],
                'status' => $u['status'],
                'current_task' => $u['current_task'],
                'last_heartbeat' => $u['last_heartbeat'],
                'role' => $u['role'],
                'is_online' => true,
                'is_expired' => $isExpired
            ];
        }, $users);
        
        return ['code' => 0, 'msg' => 'success', 'data' => ['list' => $result]];
    }

    public function requestScreenshot()
    {
        $this->requireAdmin();
        $input = json_decode(file_get_contents('php://input'), true);
        
        $userId = $input['user_id'] ?? null;
        
        if (!$userId) {
            return ['code' => 1, 'msg' => '用户ID不能为空'];
        }
        
        $db = Database::getInstance();
        
        $db->insert('auto_remote_command', [
            'user_id' => $userId,
            'device_id' => '',
            'command' => 'take_screenshot',
            'params' => json_encode([]),
            'executed' => 0,
            'create_time' => date('Y-m-d H:i:s')
        ]);
        
        return ['code' => 0, 'msg' => 'success'];
    }

    public function getScreenshot($userId)
    {
        $this->requireAdmin();
        
        $db = Database::getInstance();
        
        $screenshot = $db->fetch(
            "SELECT * FROM auto_screenshot 
             WHERE user_id = :user_id 
             ORDER BY create_time DESC 
             LIMIT 1",
            ['user_id' => $userId]
        );
        
        if (!$screenshot) {
            return ['code' => 1, 'msg' => '暂无截屏', 'data' => null];
        }
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => [
                'screenshot_id' => (int)$screenshot['screenshot_id'],
                'user_id' => (int)$screenshot['user_id'],
                'image_path' => $screenshot['image_path'],
                'create_time' => $screenshot['create_time']
            ]
        ];
    }

    public function deleteScreenshot($screenshotId)
    {
        $this->requireAdmin();
        
        $db = Database::getInstance();
        
        $screenshot = $db->fetch(
            "SELECT * FROM auto_screenshot WHERE screenshot_id = :id",
            ['id' => $screenshotId]
        );
        
        if ($screenshot) {
            $imagePath = __DIR__ . '/../../public/' . $screenshot['image_path'];
            if (file_exists($imagePath)) {
                unlink($imagePath);
            }
            
            $db->delete('auto_screenshot', 'screenshot_id = :id', ['id' => $screenshotId]);
        }
        
        return ['code' => 0, 'msg' => 'success'];
    }
    
    public function getExecutionLogs($recordId)
    {
        $this->requireAdmin();
        
        $db = Database::getInstance();
        
        $logs = $db->fetchAll(
            "SELECT * FROM auto_execution_log 
             WHERE task_id = :task_id 
             ORDER BY log_time ASC",
            ['task_id' => $recordId]
        );
        
        $result = array_map(function($log) {
            return [
                'id' => (int)$log['id'],
                'log_type' => $log['log_type'],
                'log_time' => $log['log_time'],
                'log_content' => $log['log_content'],
                'detail' => $log['detail']
            ];
        }, $logs);
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => $result
        ];
    }
}
