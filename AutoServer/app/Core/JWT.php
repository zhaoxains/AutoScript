<?php

namespace App\Core;

class JWT
{
    private static $secret = null;
    private static $algorithm = 'HS256';
    
    private static function getSecret()
    {
        if (self::$secret === null) {
            $config = require dirname(__DIR__, 2) . '/config/app.php';
            self::$secret = $config['jwt']['secret'] ?? 'default-secret-change-in-production';
            
            if (self::$secret === 'default-secret-change-in-production') {
                error_log('WARNING: Using default JWT secret. Please configure jwt.secret in config/app.php');
            }
        }
        return self::$secret;
    }

    public static function encode($payload)
    {
        $header = json_encode(['typ' => 'JWT', 'alg' => self::$algorithm]);
        $payload = json_encode($payload);
        
        $base64Header = self::base64UrlEncode($header);
        $base64Payload = self::base64UrlEncode($payload);
        
        $signature = self::sign("{$base64Header}.{$base64Payload}");
        $base64Signature = self::base64UrlEncode($signature);
        
        return "{$base64Header}.{$base64Payload}.{$base64Signature}";
    }

    public static function decode($token)
    {
        $parts = explode('.', $token);
        if (count($parts) !== 3) {
            return null;
        }

        list($base64Header, $base64Payload, $base64Signature) = $parts;
        
        $signature = self::base64UrlDecode($base64Signature);
        $expectedSignature = self::sign("{$base64Header}.{$base64Payload}");
        
        if (!hash_equals($expectedSignature, $signature)) {
            return null;
        }

        $payload = self::base64UrlDecode($base64Payload);
        return json_decode($payload, true);
    }

    private static function sign($data)
    {
        return hash_hmac('sha256', $data, self::getSecret(), true);
    }

    private static function base64UrlEncode($data)
    {
        return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
    }

    private static function base64UrlDecode($data)
    {
        return base64_decode(strtr($data, '-_', '+/'));
    }
}
