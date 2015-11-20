<?php

function get_item_with_location($recipient, $last_refresh, $latitude, $longitude, $radius) {
	global $pdo;
	
	$res = $pdo->prepare('SELECT 
				itm.`ID`, itm.`from`, itm.`to`, itm.`date`, cnd.`condition`, txt.`text`, "simpleText" as "type"
    		FROM 
				tb_item as itm, tb_item_text as txt, tb_condition as cnd, tb_metadata as mtd, tb_metadata_position as pos
    		WHERE
				pos.latitude <= :latitude
			AND pos.longitude <= :longitude
			AND pos.ID = mtd.ID
			AND mtd.ID = cnd.ID
			AND itm.condition = cnd.ID
			AND	itm.to = :to
    		AND itm.date > :last_refresh
			AND itm.ID = txt.ID');
	
	$res->bindParam(':latitude', $latitude, PDO::PARAM_STR);
	$res->bindParam(':longitude', $latitude, PDO::PARAM_STR);
	$res->bindParam(':to', $recipient['ID'], PDO::PARAM_INT);
	$res->bindParam(':last_refresh', $last_refresh, PDO::PARAM_STR);
	$res->execute();
	
	$ret = [];
	
	// Fill the "from" and "to" columns with the users data instead of their ID
	while ($data = $res->fetch()) {
		$data['from'] = get_user($data['from']);
		$data['to'] = get_user($data['to']);
		$ret[] = $data;
	}
	
	return $ret;
}

/**
 * Get a user from the DB using his ID
 * @param int $user_id
 */
function get_user($user_id) {
	global $pdo;
	$res = $pdo->prepare('SELECT *, "user" as "type"
			FROM view_user as usr
    		WHERE usr.ID = :user_id');

	$res->bindParam(':user_id', $user_id, PDO::PARAM_INT);
	$res->execute();

	if (!($res = $res->fetchAll())) {
		die();
	}

	return $res[0];
}

/**
 * Get items sent to a specific recipient from a given date
 * @param Recipient.User (JSON) $recipient
 * @param posix_time $last_refresh
 * @return array of items (indexed by column name)
 */
function get_items($recipient, $last_refresh) {
	global $pdo;
	
	$res = $pdo->prepare('SELECT *, "simpleText" as "type"
    		FROM view_text_message as msg
    		WHERE msg.to = :to
    		AND msg.date > :last_refresh');
	
	$res->bindParam(':to', $recipient['ID'], PDO::PARAM_INT);
	$res->bindParam(':last_refresh', $last_refresh, PDO::PARAM_STR);
	$res->execute();
	
	$ret = [];
	
	// Fill the "from" and "to" columns with the users data instead of their ID
	while ($data = $res->fetch()) {
		$data['from'] = get_user($data['from']);
		$data['to'] = get_user($data['to']);
		$ret[] = $data;
	}
	
	return $ret;
}