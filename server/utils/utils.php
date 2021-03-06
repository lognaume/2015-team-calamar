<?php
	require_once('config.php');

    global $pdo;
    try {
	    $pdo = new PDO('mysql:host='.MYSQL_HOST.';port='.MYSQL_PORT.';dbname='.MYSQL_DBNAME, MYSQL_USER, MYSQL_PASSWORD,array(PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION));
	    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
	    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    } catch (Exception $e) {
    	http_response_code(500);
    	die("Database error : ".$e->getMessage());
    }
    
    function get_post_JSON() {
	    // Retrieve post data
	    $content = urldecode(file_get_contents('php://input'));
	    // decode the json
	    $data = json_decode($content, true);
	    //check if there is an error
	    if($data == null)
	    {
	    	http_response_code(400);
	    	die("Error : json data not found");
	    }
	    return $data;
    }
