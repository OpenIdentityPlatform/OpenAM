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
 * $Id: logout.php,v 1.2 2007/05/25 00:16:12 superpat7 Exp $
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
	
	

    if ( federatedLogin() ) {
        header("Location: " . $LIGHTBULB_CONFIG['baseurl'] .  "spSLOInit.php?binding=urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST&RelayState=" . $_GET['RelayState'] );
    } else {
        spi_sessionhandling_clearUserId();
        header("Location: ". $_GET['RelayState']);
    }
    exit();
?>
