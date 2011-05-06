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
 * $Id: prompt.php,v 1.1 2007/05/22 05:38:39 andreas1980 Exp $
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
	
	// URL to return user to after authentication. Will be this page :D
	$return_url = selfURL();
	
	// URL initiating SSO with lighbulb, contains some configuration parameters.
	$ssoinit_url = $LIGHTBULB_CONFIG['baseurl'] . "spSSOInit.php?" . 
		"metaAlias=/sp&idpEntityID=sam.feide.no&" .
		"RelayState=" . $_GET['goto'];
	
	// Logout URL. Also a openssophp service with some parameters and a return url.
	$logout_url = $LIGHTBULB_CONFIG['baseurl'] . "spSLOInit.php?" . 
		"metaAlias=/sp&" .
		"RelayState=" . $_GET['goto'];



 
?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<title>Pat's Example</title>
</head>
<body>

<h1>Login at OpenSSO PHP Extension</h1>

<p>This example is using both local usermanagement as well as federated login through an IdP.</p>

	<div style="margin: 2em; padding: 1em; border: 1px solid #eee">
	
	<?php 
		if (!is_null(spi_sessionhandling_getNameID())) {
			?><p>You are already authenticated to an IdP but your identity does not map to a local identity. Please login using a local account below, and your account will be federated with that from the IdP. The next time you login with your IdP account, you will not need to perform a local login.</p><?php
		}
	?>
	
	<form action="login.php" method="post">
		<fieldset style="border: 1px solid #999; background: #ffa"><legend>Local authentication</legend>
		<p>Username: <input name="username"></p>
		<p>Password: <input type="password" name="password"></p>
		<input type="hidden" name="goto" value="<?php echo urlencode($_GET["goto"]); ?>">
		<p><input type="submit" Value="Login"></p>
		</fieldset>
	</form>
	
	<?php 
		if (is_null(spi_sessionhandling_getNameID())) {
			?><p><a href="<?php echo $ssoinit_url; ?>">Login via IDP</a></p><?php
		}
	?>

    </body>
</html>
