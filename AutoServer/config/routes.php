<?php

return [
    ['POST', '/auth/register', 'AuthController@register'],
    ['POST', '/auth/login', 'AuthController@login'],
    ['POST', '/auth/logout', 'AuthController@logout'],
    
    ['GET', '/config/scripts', 'ConfigController@scripts'],
    ['GET', '/config/global', 'ConfigController@globalConfig'],
    
    ['POST', '/report/task', 'ReportController@task'],
    ['POST', '/report/logs', 'ReportController@logs'],
    
    ['POST', '/heartbeat', 'HeartbeatController@beat'],
    ['POST', '/screenshot/upload', 'HeartbeatController@uploadScreenshot'],
    
    ['GET', '/admin/dashboard', 'AdminController@dashboard'],
    ['GET', '/admin/users', 'AdminController@users'],
    ['GET', '/admin/users/{id}/records', 'AdminController@userRecords'],
    ['POST', '/admin/users', 'AdminController@createUser'],
    ['PUT', '/admin/users/{id}', 'AdminController@updateUser'],
    ['DELETE', '/admin/users/{id}', 'AdminController@deleteUser'],
    
    ['GET', '/admin/scripts', 'AdminController@scripts'],
    ['GET', '/admin/scripts/{id}/records', 'AdminController@scriptRecords'],
    ['POST', '/admin/scripts', 'AdminController@createScript'],
    ['PUT', '/admin/scripts/{id}', 'AdminController@updateScript'],
    ['DELETE', '/admin/scripts/{id}', 'AdminController@deleteScript'],
    
    ['GET', '/admin/records', 'AdminController@records'],
    ['GET', '/admin/records/export', 'AdminController@exportRecords'],
    
    ['GET', '/admin/online-users', 'AdminController@getOnlineUsers'],
    ['POST', '/admin/command', 'AdminController@sendCommand'],
    ['POST', '/admin/execute', 'AdminController@executeScripts'],
    ['POST', '/admin/stop', 'AdminController@stopExecution'],
    
    ['POST', '/admin/screenshot/request', 'AdminController@requestScreenshot'],
    ['GET', '/admin/screenshot/{id}', 'AdminController@getScreenshot'],
    ['DELETE', '/admin/screenshot/{id}', 'AdminController@deleteScreenshot'],
    
    ['GET', '/admin/records/{id}/logs', 'AdminController@getExecutionLogs'],
];
