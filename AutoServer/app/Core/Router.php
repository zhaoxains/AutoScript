<?php

namespace App\Core;

class Router
{
    private $routes = [];

    public function add($method, $path, $handler)
    {
        $this->routes[] = [
            'method' => $method,
            'path' => $path,
            'handler' => $handler
        ];
    }

    public function dispatch()
    {
        $method = $_SERVER['REQUEST_METHOD'];
        $uri = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
        $basePath = '/api';
        
        if (strpos($uri, $basePath) === 0) {
            $uri = substr($uri, strlen($basePath));
        }

        foreach ($this->routes as $route) {
            if ($route['method'] !== $method) {
                continue;
            }

            $pattern = $this->convertToRegex($route['path']);
            if (preg_match($pattern, $uri, $matches)) {
                array_shift($matches);
                
                $handler = $route['handler'];
                
                if (is_string($handler)) {
                    list($controller, $action) = explode('@', $handler);
                    $controllerClass = "App\\Controllers\\{$controller}";
                    $instance = new $controllerClass();
                    return call_user_func_array([$instance, $action], $matches);
                }
            }
        }

        throw new \Exception('接口不存在', 404);
    }

    private function convertToRegex($path)
    {
        $pattern = preg_replace('/\{([a-zA-Z]+)\}/', '([^/]+)', $path);
        return '#^' . $pattern . '$#';
    }
}
