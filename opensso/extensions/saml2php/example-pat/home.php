<?php
/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: home.php,v 1.1 2007/05/22 05:38:38 andreas1980 Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

	// Loading SAML library
	require_once('../openssophp/config/config.php');
	require_once('../openssophp/lib/saml-lib.php');
	
	// Needs a function to get the token from the php session
	require_once('../openssophp/spi/sessionhandling/' . $LIGHTBULB_CONFIG['spi-sessionhandling'] . '.php');
	
	// Needs a function to get the token from the php session
	require_once('../openssophp/spi/namemapping/' . $LIGHTBULB_CONFIG['spi-namemapping'] . '.php');
	
	// Load functions...
	require_once("example-lib.php");
	
	
	// Checking configuration...
	
	if ($LIGHTBULB_CONFIG['spi-namemapping'] != "database" ) {
		echo "This example requires that you are using the <tt>namemapping/database</tt> plugin. Now your configuration points at the <tt>namemapping/" . $LIGHTBULB_CONFIG['spi-namemapping'] . "</tt>.";
		exit();
	}
	
	
	// URL to return user to after authentication. Will be this page :D
	$return_url = selfURL();
	
	// URL initiating SSO with lighbulb, contains some configuration parameters.
	$ssoinit_url = $LIGHTBULB_CONFIG['baseurl'] . "spSSOInit.php?" . 
		"metaAlias=/sp&" .
		"RelayState=" . urlencode($return_url);
	
	// Logout URL. Also a openssophp service with some parameters and a return url.
	$logout_url = $LIGHTBULB_CONFIG['baseurl'] . "spSLOInit.php?" . 
		"metaAlias=/sp&" .
		"RelayState=" . urlencode($return_url);

	$prompt_url = "prompt.php?goto=" . urlencode($return_url);

    $localID = spi_sessionhandling_getUserID();


	if (! isset($localID) && !is_null(spi_sessionhandling_getNameID()) ) {
		// The user is successfully authenticated, but not mapped to a local user account, so will will ask the user to
		// perform a local login.
		
		header("Location: " . $prompt_url);
		exit();
	}
    
    
?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<title>Pat's Example</title>
</head>
<body>
<h1>Welcome to Pat's Example to OpenSSO PHP Extension</h1>

<p>This example is using both local usermanagement as well as federated login through an IdP.</p>

<!-- The following section is differing whether you are logged in or not -->
<?php if (isset( $localID)) { ?>

	<p>You are now successfully authenticated as user <?php echo getUserName($localID); ?>.</p>
	<p><a href="logout.php?RelayState=<?php echo urlencode($return_url); ?>">Logout</a></p>
	
<?php } else { ?>

	<p>You are currently not logged in. <a href="<?php echo $prompt_url?>">Please log in</a>.</p>
	
<?php } ?>



</body>
</html>