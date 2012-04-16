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
 * $Id: spSSOInit.php,v 1.2 2007/06/11 17:33:14 superpat7 Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

    error_log("Entering spSSOInit.php");

	// Loading configuration
    require_once('config/config.php');
    require_once($LIGHTBULB_CONFIG['basedir'] . 'config/saml-metadata-SP.php');
    require_once($LIGHTBULB_CONFIG['basedir'] . 'config/saml-metadata-IdP.php');
    
    // Loading libraries
    require 'lib/saml-lib.php';

    $metaAlias = $_GET["metaAlias"];

    if (!isset($metaAlias)) {
        // If no metaalias specified then just use the first SP defined in the list
        foreach ( $spMetadata as $spKey => $spValue ) {
            $metaAlias = $spKey;
            break;
        }
        if (!isset($metaAlias)) {
            $error = "400 No SPs are configured!";
            header($_SERVER["SERVER_PROTOCOL"] . " " . $error );
            echo ($error);
            exit;
        }
    }

    $idpEntityID = $_GET["idpEntityID"];

    if (!isset($idpEntityID)) {
        // If no idpEntityID specified then just use the first IdP defined in the list
        foreach ( $idpMetadata as $idpKey => $idpValue ) {
            $idpEntityID = $idpKey;
            break;
        }
        if (!isset($idpEntityID)) {
            $error = "400 No IdPs are configured!";
            header($_SERVER["SERVER_PROTOCOL"] . " " . $error );
            echo ($error);
            exit;
        }
    }

    $binding = $_GET["binding"];
    if (!isset($binding)) {
        // Default to POST binding for now
        $binding = SAML2_BINDINGS_POST;
    } else if (($binding != SAML2_BINDINGS_POST) 
	  && ($binding != SAML2_BINDINGS_POST_SIMPLESIGN)) {
        $error = "400 The only currently supported bindings are " 
        . SAML2_BINDINGS_POST . " and " . SAML2_BINDINGS_POST_SIMPLESIGN;
        header($_SERVER["SERVER_PROTOCOL"] . " " . $error );
        echo ($error);
        exit;
    }

    $RelayStateURL = $_GET["RelayState"];

    error_log("metaAlias = " . $metaAlias);
    error_log("idpEntityID = " . $idpEntityID);
    error_log("binding = " . $binding);
    error_log("RelayState = " . $RelayStateURL);

    if (!isset($spMetadata[$metaAlias])) {
        $error = "400 No SP configured for " . $metaAlias;
        header($_SERVER["SERVER_PROTOCOL"] . " " . $error );
        echo ($error);
        exit;
    }

    if (!isset($idpMetadata[$idpEntityID])) {
        $error = "400 No IdP configured for " . $idpEntityID;
        header($_SERVER["SERVER_PROTOCOL"] . " " . $error );
        echo ($error);
        exit;
    }

    $assertionConsumerServiceURL = $spMetadata[$metaAlias]["assertionConsumerServiceURL"];
    $issuer = $spMetadata[$metaAlias]["issuer"];
    $spNameQualifier = $spMetadata[$metaAlias]["spNameQualifier"];

    $idpTargetUrl = $idpMetadata[$idpEntityID]["SingleSignOnUrl"];

    $id = randomhex(42);
    $issueInstant = gmdate("Y-m-d\TH:i:s\Z");

    // Really simple impl for now - just use the URL itself
    $relayState = urlencode($RelayStateURL);

    $authnRequest = "<samlp:AuthnRequest  " .
      "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" .
      "ID=\"_" . $id . "\" " .
      "Version=\"2.0\" " .
      "IssueInstant=\"" . $issueInstant . "\" " .
      "ForceAuthn=\"false\" " .
      "IsPassive=\"false\" " .
      "ProtocolBinding=\"" . $binding . "\" " .
      "AssertionConsumerServiceURL=\"" . $assertionConsumerServiceURL . "\">\n" .
        "<saml:Issuer " .
        "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">" .
          $issuer .
        "</saml:Issuer>\n" .
        "<samlp:NameIDPolicy  " .
        "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " .
        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\" " .
        "SPNameQualifier=\"" . $spNameQualifier . "\" " .
        "AllowCreate=\"true\" />\n" . 
        "<samlp:RequestedAuthnContext " .
        "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " .
        "Comparison=\"exact\">" .
          "<saml:AuthnContextClassRef " .
          "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">" .
            "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport" .
          "</saml:AuthnContextClassRef>" .
        "</samlp:RequestedAuthnContext>\n" .
      "</samlp:AuthnRequest>";


    error_log("Authn request = " . $authnRequest);

    $encodedAuthnRequest = urlencode( base64_encode( gzdeflate( $authnRequest ) ));

    error_log("Encoded request = " . $encodedAuthnRequest);

    $redirectUrl = $idpTargetUrl . "?SAMLRequest=" . $encodedAuthnRequest . "&RelayState=" . $relayState;
   // $redirectUrl = $idpTargetUrl . "?SAMLRequest=" . $encodedAuthnRequest . "&"; #RelayState=" . $relayState;
    
    error_log("Redirect URL = " . $redirectUrl);

    header("Location: " . $redirectUrl);

    exit;
?>
