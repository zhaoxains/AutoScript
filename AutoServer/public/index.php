<?php

define('APP_ROOT', dirname(__DIR__));

require_once APP_ROOT . '/vendor/autoload.php';

$config = require APP_ROOT . '/config/app.php';

App\Core\Database::init($config['database']);

$app = new App\Core\Application();

$app->run();
