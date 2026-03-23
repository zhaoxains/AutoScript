<?php

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

define('APP_ROOT', dirname(__DIR__));

$action = $_GET['action'] ?? 'status';

try {
    require_once APP_ROOT . '/vendor/autoload.php';
    
    $config = require APP_ROOT . '/config/app.php';
    App\Core\Database::init($config['database']);
    
    switch ($action) {
        case 'status':
            echo json_encode([
                'code' => 0,
                'msg' => 'success',
                'data' => [
                    'status' => 'ok',
                    'php_version' => PHP_VERSION,
                    'time' => date('Y-m-d H:i:s')
                ]
            ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
            break;
            
        case 'login':
            handleLogin();
            break;
            
        case 'dashboard':
            handleDashboard();
            break;
            
        case 'check_token':
            handleCheckToken();
            break;
            
        case 'db_test':
            handleDbTest();
            break;
            
        default:
            echo json_encode([
                'code' => 1001,
                'msg' => '未知操作: ' . $action,
                'data' => null
            ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    }
    
} catch (Exception $e) {
    echo json_encode([
        'code' => 500,
        'msg' => $e->getMessage(),
        'data' => null,
        'debug' => [
            'file' => $e->getFile(),
            'line' => $e->getLine()
        ]
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
}

function getHeaders() {
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

function handleLogin() {
    $rawInput = file_get_contents('php://input');
    $input = json_decode($rawInput, true);
    
    if (!$input) {
        echo json_encode([
            'code' => 1001,
            'msg' => '无效的请求数据',
            'data' => null,
            'debug' => ['raw_input' => $rawInput]
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        return;
    }
    
    $username = trim($input['username'] ?? '');
    $password = $input['password'] ?? '';
    $deviceId = $input['device_id'] ?? 'debug-device';
    
    if (empty($username) || empty($password)) {
        echo json_encode([
            'code' => 1001,
            'msg' => '用户名和密码不能为空',
            'data' => null
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        return;
    }
    
    $db = App\Core\Database::getInstance();
    
    $user = $db->fetch(
        "SELECT * FROM auto_user WHERE username = :username",
        ['username' => $username]
    );
    
    if (!$user) {
        echo json_encode([
            'code' => 1005,
            'msg' => '用户不存在',
            'data' => null
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        return;
    }
    
    if ($user['status'] != 1) {
        echo json_encode([
            'code' => 1008,
            'msg' => '账号已被禁用',
            'data' => null
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        return;
    }
    
    if ($user['expire_time'] && strtotime($user['expire_time']) < time()) {
        echo json_encode([
            'code' => 1009,
            'msg' => '账号已过期',
            'data' => null,
            'debug' => [
                'expire_time' => $user['expire_time'],
                'current_time' => date('Y-m-d H:i:s')
            ]
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        return;
    }
    
    $passwordField = $user['password'] ?? $user['password_hash'] ?? null;
    $salt = $user['salt'] ?? '';
    
    $passwordValid = false;
    if (!empty($salt)) {
        $passwordValid = hash_equals($passwordField, hash('sha256', $password . $salt));
    }
    if (!$passwordValid && password_get_info($passwordField)['algo'] > 0) {
        $passwordValid = password_verify($password, $passwordField);
    }
    
    if (!$passwordValid) {
        echo json_encode([
            'code' => 1006,
            'msg' => '密码错误',
            'data' => null
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        return;
    }
    
    $token = App\Core\JWT::encode([
        'user_id' => $user['user_id'],
        'username' => $user['username'],
        'role' => (int)$user['role'],
        'device_id' => $deviceId
    ]);
    
    $db->update(
        'auto_user',
        ['device_id' => $deviceId, 'update_time' => date('Y-m-d H:i:s')],
        'user_id = :user_id',
        ['user_id' => $user['user_id']]
    );
    
    echo json_encode([
        'code' => 0,
        'msg' => 'success',
        'data' => [
            'token' => $token,
            'user_info' => [
                'user_id' => $user['user_id'],
                'username' => $user['username'],
                'role' => (int)$user['role'],
                'status' => (int)$user['status'],
                'expire_time' => $user['expire_time'],
                'device_id' => $deviceId
            ]
        ]
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
}

function handleDashboard() {
    $headers = getHeaders();
    $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? null;
    
    if (!$authHeader || !preg_match('/Bearer\s+(.+)/', $authHeader, $matches)) {
        echo json_encode([
            'code' => 1002,
            'msg' => '缺少 Authorization 头',
            'data' => null,
            'debug' => ['headers' => $headers]
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        return;
    }
    
    $token = $matches[1];
    $payload = App\Core\JWT::decode($token);
    
    if (!$payload) {
        echo json_encode([
            'code' => 1002,
            'msg' => 'Token 解码失败',
            'data' => null
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        return;
    }
    
    if (!isset($payload['role']) || !in_array($payload['role'], [1, 9])) {
        echo json_encode([
            'code' => 1003,
            'msg' => '权限不足',
            'data' => null,
            'debug' => ['payload' => $payload]
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        return;
    }
    
    $db = App\Core\Database::getInstance();
    
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
    
    echo json_encode([
        'code' => 0,
        'msg' => 'success',
        'data' => [
            'user_count' => (int)$userCount,
            'script_count' => (int)$scriptCount,
            'today_tasks' => (int)$todayRecords,
            'success_rate' => $successRate['total'] > 0 
                ? round($successRate['success'] / $successRate['total'] * 100, 1) 
                : 0
        ],
        'debug' => ['payload' => $payload]
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
}

function handleCheckToken() {
    $headers = getHeaders();
    $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? null;
    
    if (!$authHeader || !preg_match('/Bearer\s+(.+)/', $authHeader, $matches)) {
        echo json_encode([
            'code' => 1002,
            'msg' => '缺少 Authorization 头',
            'data' => null
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        return;
    }
    
    $token = $matches[1];
    $payload = App\Core\JWT::decode($token);
    
    if (!$payload) {
        echo json_encode([
            'code' => 1002,
            'msg' => 'Token 无效或已过期',
            'data' => null
        ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
        return;
    }
    
    echo json_encode([
        'code' => 0,
        'msg' => 'success',
        'data' => [
            'valid' => true,
            'payload' => $payload
        ]
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
}

function handleDbTest() {
    $db = App\Core\Database::getInstance();
    
    $tables = $db->fetchAll("SHOW TABLES");
    $userCount = $db->fetch("SELECT COUNT(*) as count FROM auto_user")['count'];
    $scriptCount = $db->fetch("SELECT COUNT(*) as count FROM auto_script")['count'];
    
    echo json_encode([
        'code' => 0,
        'msg' => 'success',
        'data' => [
            'tables' => count($tables),
            'user_count' => (int)$userCount,
            'script_count' => (int)$scriptCount
        ]
    ], JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
}
