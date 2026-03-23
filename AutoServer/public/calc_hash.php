<?php
$password = "admin123";
$salt = "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6";
$combined = $password . $salt;
$hash = hash('sha256', $combined);
echo "Password: " . $password . "\n";
echo "Salt: " . $salt . "\n";
echo "Combined: " . $combined . "\n";
echo "SHA256 Hash: " . $hash . "\n";
echo "Hash Length: " . strlen($hash) . "\n";
