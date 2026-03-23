<?php

namespace App\Controllers;

use App\Core\Controller;
use App\Core\Database;
use App\Core\JWT;

class AuthController extends Controller
{
    public function register()
    {
        $input = json_decode(file_get_contents('php://input'), true);
        
        $username = trim($input['username'] ?? '');
        $password = $input['password'] ?? '';
        $confirmPassword = $input['confirm_password'] ?? '';
        $deviceId = $input['device_id'] ?? '';
        $referrer = trim($input['referrer'] ?? '');
        
        if (empty($username) || empty($password) || empty($deviceId)) {
            return [
                'code' => 1001,
                'msg' => '用户名、密码和设备ID不能为空'
            ];
        }
        
        if ($password !== $confirmPassword) {
            return [
                'code' => 1001,
                'msg' => '两次密码输入不一致'
            ];
        }
        
        if (!preg_match('/^[a-zA-Z0-9_]{3,20}$/', $username)) {
            return [
                'code' => 1001,
                'msg' => '用户名只能包含字母、数字和下划线，长度3-20个字符'
            ];
        }
        
        if (strlen($password) < 6) {
            return [
                'code' => 1001,
                'msg' => '密码长度不能少于6个字符'
            ];
        }
        
        $db = Database::getInstance();
        
        $existingUser = $db->fetch(
            "SELECT user_id FROM auto_user WHERE username = :username",
            ['username' => $username]
        );
        
        if ($existingUser) {
            return [
                'code' => 1007,
                'msg' => '用户名已存在'
            ];
        }
        
        $existingDevice = $db->fetch(
            "SELECT user_id FROM auto_user WHERE device_id = :device_id",
            ['device_id' => $deviceId]
        );
        
        if ($existingDevice) {
            return [
                'code' => 1008,
                'msg' => '该设备已注册'
            ];
        }
        
        $salt = bin2hex(random_bytes(16));
        $passwordHash = hash('sha256', $password . $salt);
        
        $referrerId = null;
        if (!empty($referrer)) {
            $referrerUser = $db->fetch(
                "SELECT user_id FROM auto_user WHERE username = :username",
                ['username' => $referrer]
            );
            if ($referrerUser) {
                $referrerId = $referrerUser['user_id'];
            }
        }
        
        $userId = $db->insert('auto_user', [
            'username' => $username,
            'password' => $passwordHash,
            'salt' => $salt,
            'device_id' => $deviceId,
            'role' => 1,
            'status' => 1,
            'expire_time' => date('Y-m-d H:i:s', strtotime('+1 year')),
            'referrer_id' => $referrerId,
            'create_time' => date('Y-m-d H:i:s')
        ]);
        
        $token = JWT::encode([
            'user_id' => $userId,
            'username' => $username,
            'device_id' => $deviceId
        ]);
        
        $user = $db->fetch(
            "SELECT user_id, username, role, status, expire_time, device_id FROM auto_user WHERE user_id = :user_id",
            ['user_id' => $userId]
        );
        
        return [
            'code' => 0,
            'msg' => 'success',
            'data' => [
                'token' => $token,
                'user_info' => $user
            ]
        ];
    }
    
    public function login()
    {
        $input = json_decode(file_get_contents('php://input'), true);
        
        $username = trim($input['username'] ?? '');
        $password = $input['password'] ?? '';
        $deviceId = $input['device_id'] ?? '';
        
        if (empty($username) || empty($password) || empty($deviceId)) {
            return [
                'code' => 1001,
                'msg' => '用户名、密码和设备ID不能为空'
            ];
        }
        
        $db = Database::getInstance();
        
        $user = $db->fetch(
            "SELECT * FROM auto_user WHERE username = :username",
            ['username' => $username]
        );
        
        if (!$user) {
            return [
                'code' => 1005,
                'msg' => '用户不存在'
            ];
        }
        
        if ($user['status'] != 1) {
            return [
                'code' => 1008,
                'msg' => '账号已被禁用'
            ];
        }
        
        if ($user['expire_time'] && strtotime($user['expire_time']) < time()) {
            return [
                'code' => 1009,
                'msg' => '账号已过期，请联系管理员续期'
            ];
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
            return [
                'code' => 1006,
                'msg' => '密码错误'
            ];
        }
        
        $token = JWT::encode([
            'user_id' => $user['user_id'],
            'username' => $user['username'],
            'role' => (int)$user['role'],
            'device_id' => $deviceId
        ]);
        
        $db->update(
            'auto_user',
            [
                'device_id' => $deviceId,
                'update_time' => date('Y-m-d H:i:s')
            ],
            'user_id = :user_id',
            ['user_id' => $user['user_id']]
        );
        
        return [
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
        ];
    }
    
    public function logout()
    {
        $user = $this->requireAuth();
        if (!$user) {
            return [
                'code' => 1002,
                'msg' => '未授权'
            ];
        }
        
        return [
            'code' => 0,
            'msg' => 'success'
        ];
    }
}
