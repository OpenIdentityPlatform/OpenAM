<?php

// We need session management (separate module in PHP5 - needs to be installed)
session_start();


// Loading SAML library
//require_once('lightbulb/lib/Lightbulb.class.php');
require_once('../openssophp/config/config.php');
require_once('../openssophp/lib/saml-lib.php');

// Needs a function to get the token from the php session
require_once('../openssophp/spi/sessionhandling/' . $LIGHTBULB_CONFIG['spi-sessionhandling'] . '.php');



/*
 * This is a function that returns the URL that you are on right now.
 */
function selfURL() {
	$s = empty($_SERVER["HTTPS"]) ? ''
		: ($_SERVER["HTTPS"] == "on") ? "s"
		: "";
	$protocol = strleft(strtolower($_SERVER["SERVER_PROTOCOL"]), "/").$s;
	$port = ($_SERVER["SERVER_PORT"] == "80") ? ""
		: (":".$_SERVER["SERVER_PORT"]);
	return $protocol."://".$_SERVER['SERVER_NAME'].$port.$_SERVER['REQUEST_URI'];
}
// Helper function used in selfURL()
function strleft($s1, $s2) {
	return substr($s1, 0, strpos($s1, $s2));
}





// URL to return user to after authentication. Will be this page :D
$return_url = selfURL();

// URL initiating SSO with lighbulb, contains some configuration parameters.
$ssoinit_url = $LIGHTBULB_CONFIG['baseurl'] . "spSSOInit.php?" . 
//	"metaAlias=/sp&" .
	"RelayState=" . urlencode($return_url);

// Logout URL. Also a lightbulb service with some parameters and a return url.
$logout_url = $LIGHTBULB_CONFIG['baseurl'] . "spSLOInit.php?" . 
	"metaAlias=/sp&" .
	"RelayState=" . urlencode($return_url);

?>


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<meta http-equiv="content-type" content="text/html; charset=utf-8" />
	<title>Test Service</title>
</head>
<body>
	<h1>Andreas' test service</h1>
	<p>Welcome to this test service</p>
		<p>Plugin: <?php echo $LIGHTBULB_CONFIG['spi-sessionhandling']; ?>
	<?php if (isset($_SESSION['UserID'])) { ?>
		<p>Your transient nameidentifier is <?php echo $_SESSION['UserID']; ?></p>
		<p><a href="<?php echo (htmlspecialchars($logout_url)); ?>">Logout</a></p>

		<p>Session:</p><pre>
			<?php 
				/*
				ob_start();
				print_r($_SESSION);
				$debugsession = ob_get_flush();
			
				print(htmlentities($debugsession)); 
				*/
				//print_r($_SESSION);
			?></pre>
		
	<p>Attributes:</p><pre><?php 
	
		$token = spi_sessionhandling_getResponse();
		$attributes = getAttributes($token);

// If attributes are base64 encoded. Some setups use base64 encoding of attributes, becasue of the UTF-8 bug in
// Sun Access Manager, Federation Manager and OpenSSO.
//
// 		foreach ($attributes AS $key => $a) {
// 			$attributes[$key] = base64_decode($a);
// 		}
 
		print_r($attributes);

	?></pre>
	<?php } else { ?>
		<p><a href="<?php echo (htmlspecialchars($ssoinit_url)); ?>">Login via IdP</a></p>
	<?php } ?>
</body>
</html>


