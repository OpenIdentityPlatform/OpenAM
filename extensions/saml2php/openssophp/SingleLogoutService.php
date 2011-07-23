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
 * $Id: SingleLogoutService.php,v 1.2 2007/06/11 17:33:13 superpat7 Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */


	// Loading configuration
    require_once('config/config.php');
    require_once($LIGHTBULB_CONFIG['basedir'] . 'config/saml-metadata-SP.php');
    require_once($LIGHTBULB_CONFIG['basedir'] . 'config/saml-metadata-IdP.php');
  
	require_once($LIGHTBULB_CONFIG['basedir'] . 'spi/sessionhandling/' . $LIGHTBULB_CONFIG['spi-sessionhandling'] . '.php');
	require_once($LIGHTBULB_CONFIG['basedir'] . 'spi/namemapping/' . $LIGHTBULB_CONFIG['spi-namemapping'] . '.php');


    // Loading libraries
    require 'lib/saml-lib.php';

    error_log("Entering SingleLogoutService.php");

    if ($_SERVER['REQUEST_METHOD'] == 'GET')
    {
        error_log("SingleLogoutService.php: Redirect binding");

        if (empty($_GET['SAMLResponse'])) {
            echo ("<p>Unable to process the submission.<br />No SAMLResponse in HTTP parameters</p>");
            exit();
        } else {
            $postBinding = false;
			$params = $_GET;
        }
    }
    else if ($_SERVER['REQUEST_METHOD'] == 'POST')
    {
        // NOTE - Logout POST binding untested at current time!!!
        error_log("SingleLogoutService.php: POST binding");

        if (empty($_POST['SAMLResponse'])) {
            echo ("<p>Unable to process the submission.<br />No SAMLResponse in posted data</p>");
            exit();
        } else {
            $postBinding = true;
            $params = $_POST;
        }
    }

    $RelayStateURL = $params["RelayState"];
    error_log("RelayState = " . $RelayStateURL);

    if ($token = processResponse($params,FALSE,$postBinding,"SAMLResponse")) {
        $status = getLogoutResponseStatus($token);
	    error_log("Status = " . $status);
        if ( $status == SAML2_STATUS_SUCCESS ) {
            spi_sessionhandling_clearUserId();
            header("Location: " . $RelayStateURL);
        } else {
            echo ("<p>IdP reported error processing single logout.<br />" . $status . "</p><pre>");
            print_r($samlResponse);
            echo "</pre>";
            exit();
        }
    }
?>
