<?php

namespace App\Core;

class Application
{
    private $router;

    public function __construct()
    {
        $this->initRouter();
    }

    private function initRouter()
    {
        $this->router = new Router();
        
        $routes = require APP_ROOT . '/config/routes.php';
        foreach ($routes as $route) {
            $this->router->add($route[0], $route[1], $route[2]);
        }
    }

    public function run()
    {
        header('Content-Type: application/json; charset=utf-8');
        header('Access-Control-Allow-Origin: *');
        header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
        header('Access-Control-Allow-Headers: Content-Type, Authorization');

        if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
            http_response_code(200);
            exit;
        }

        try {
            $response = $this->router->dispatch();
            echo json_encode($response, JSON_UNESCAPED_UNICODE);
        } catch (\Exception $e) {
            http_response_code(200);
            echo json_encode([
                'code' => $e->getCode() ?: 500,
                'msg' => $e->getMessage(),
                'data' => null
            ], JSON_UNESCAPED_UNICODE);
        }
    }
}
