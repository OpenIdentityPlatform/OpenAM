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
 * $Id: login.php,v 1.1 2007/05/22 05:38:38 andreas1980 Exp $
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
		"metaAlias=/sp&" .
		"RelayState=" . urlencode($return_url);
	
	// Logout URL. Also a openssophp service with some parameters and a return url.
	$logout_url = $LIGHTBULB_CONFIG['baseurl'] . "spSLOInit.php?" . 
		"metaAlias=/sp&" .
		"RelayState=" . urlencode($return_url);
	


    $userid = $_POST["username"];
    $password = $_POST["password"];
	
    $username = authenticateLocalUser($userid, $password);

    if ( is_null( $username ) ) {
		echo "Error login, probably bad credentials. Sorry."; exit();
        
    } else {
    	if (!is_null(spi_sessionhandling_getNameID() )) {
    		// The user is already authenticated to an IdP so we federate the accounts..
    		$nameId = getNameID(spi_sessionhandling_getResponse());
    		spi_namemapping_mapNameIdToLocalId($nameId["NameQualifier"], $nameId["SPNameQualifier"], spi_sessionhandling_getNameID(), $userid);
    		
    	}
        spi_sessionhandling_setUserId($userid);

        header("Location: " . urldecode($_POST["goto"]));
        exit;
    }
?>
