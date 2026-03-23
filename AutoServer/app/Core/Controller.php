<?php

namespace App\Core;

class Controller
{
    protected function success($data = null, $msg = 'success')
    {
        return [
            'code' => 0,
            'msg' => $msg,
            'data' => $data
        ];
    }

    protected function error($msg, $code = 500, $data = null)
    {
        return [
            'code' => $code,
            'msg' => $msg,
            'data' => $data
        ];
    }

    protected function getJsonInput()
    {
        $input = file_get_contents('php://input');
        return json_decode($input, true) ?: [];
    }

    protected function getAuthUser()
    {
        $headers = getallheaders();
        $authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? null;
        
        if (!$authHeader || !preg_match('/Bearer\s+(.+)/', $authHeader, $matches)) {
            return null;
        }
        
        $token = $matches[1];
        $payload = JWT::decode($token);
        
        if (!$payload) {
            return null;
        }
        
        return $payload;
    }

    protected function requireAuth()
    {
        $user = $this->getAuthUser();
        if (!$user) {
            throw new \Exception('认证失败', 1002);
        }
        return $user;
    }
}
