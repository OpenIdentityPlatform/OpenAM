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
 * $Id $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

session_start();

function spi_sessionhandling_setUserID($userID)
{
    $_SESSION['UserID'] = $userID;
    error_log("Setting UserID to [" . $userID . "]");
}

function spi_sessionhandling_getUserID()
{
    return $_SESSION['UserID'];
}

function spi_sessionhandling_setNameID($nameID) {	
	$_SESSION['NameID'] = $nameID;
	error_log("Setting NameID to [" . $nameID . "]");
}

function spi_sessionhandling_getNameID() {
	return $_SESSION['NameID'];
}

function spi_sessionhandling_clearUserId()
{
    unset($_SESSION['NameID']);
    unset($_SESSION['UserID']);
    unset($_SESSION['SamlResponse']);
}

function spi_sessionhandling_setResponse($token)
{
    $_SESSION['SamlResponse'] = $token->saveXML();
}

function spi_sessionhandling_getResponse()
{
    $token = new DOMDocument();
    $token->loadXML($_SESSION['SamlResponse']);
    return $token;
}

function spi_sessionhandling_federatedLogin()
{
    return isset($_SESSION['SamlResponse']);
}


?>
