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
 * $Id: spSLOInit.php,v 1.1 2007/05/22 05:38:39 andreas1980 Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

    error_log("Entering spSingleLogoutInit.php");

	// Loading configuration
    require_once('config/config.php');
    require_once($LIGHTBULB_CONFIG['basedir'] . 'config/saml-metadata-SP.php');
    require_once($LIGHTBULB_CONFIG['basedir'] . 'config/saml-metadata-IdP.php');
  
	require_once($LIGHTBULB_CONFIG['basedir'] . 'spi/sessionhandling/' . $LIGHTBULB_CONFIG['spi-sessionhandling'] . '.php');


    // Loading libraries
    require 'lib/saml-lib.php';



//    require 'samlSpMetadata.php';
//    require 'samlIdpMetadata.php';
//    require 'saml-lib.php';
//    require 'localUserManagement.php';

    $token = spi_sessionhandling_getResponse();

    $binding = $_GET["binding"];
    $RelayStateURL = $_GET["RelayState"];

    error_log("binding = " . $binding);
    error_log("RelayState = " . $RelayStateURL);

    $idpEntityID = getIssuer($token);

    if (!isset($idpMetadata[$idpEntityID])) {
        $error = "400 No IdP configured for " . $idpEntityID;
        header($_SERVER["SERVER_PROTOCOL"] . " " . $error );
        echo ($error);
        exit;
    }

    $nameId = getNameId($token);
    $sessionIndex = getSessionIndex($token);
    

    $idpTargetUrl = $idpMetadata[$idpEntityID]["SingleLogOutUrl"];

    $id = randomhex(42);
    $issueInstant = gmdate("Y-m-d\TH:i:s\Z");

    // Really simple impl for now - just use the URL itself
    $relayState = urlencode($RelayStateURL);

    $logoutRequest = "<samlp:LogoutRequest " .
      "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " . 
      "ID=\"" . $id . "\" " .
      "Version=\"2.0\" " .
      "IssueInstant=\"" . $issueInstant . "\"> " .
        "<saml:Issuer " . 
        "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">" .
          $nameId["SPNameQualifier"] .
        "</saml:Issuer>" .
        "<saml:NameID " . 
        "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " . 
        "NameQualifier=\"" . $nameId["NameQualifier"] . "\" " . 
        "SPNameQualifier=\"" . $nameId["SPNameQualifier"] . "\" " . 
        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\">" . 
          $nameId["NameID"] . 
        "</saml:NameID>" . 
        "<samlp:SessionIndex " .
        "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">" . 
          $sessionIndex .
        "</samlp:SessionIndex>" .
      "</samlp:LogoutRequest>";

    error_log("Logout request = " . $logoutRequest);

    $encodedLogoutRequest = urlencode( base64_encode( gzdeflate( $logoutRequest ) ));

    error_log("Encoded request = " . $encodedLogoutRequest);

    $redirectUrl = $idpTargetUrl . "?SAMLRequest=" . $encodedLogoutRequest . "&RelayState=" . $relayState;

    error_log("Redirect URL = " . $redirectUrl);

    header("Location: " . $redirectUrl);

    exit;
?>
