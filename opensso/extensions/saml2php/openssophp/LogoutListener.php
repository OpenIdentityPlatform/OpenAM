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
 * $Id: LogoutListener.php,v 1.2 2007/06/11 17:33:13 superpat7 Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */




require_once('config/config.php');
require_once('lib/saml-lib.php');

require_once($LIGHTBULB_CONFIG['basedir'] . 'config/saml-metadata-SP.php');
require_once($LIGHTBULB_CONFIG['basedir'] . 'config/saml-metadata-IdP.php');
    

require_once('spi/sessionhandling/' . $LIGHTBULB_CONFIG['spi-sessionhandling'] . '.php');
require_once('spi/namemapping/' . $LIGHTBULB_CONFIG['spi-namemapping'] . '.php');



function getDefaultSPEntityID($spmeta) {
	foreach ($spmeta AS $key => $value) {
		return $value['issuer'];
	}
}



if (empty($_GET['SAMLRequest'])) {
	
	echo '<p>Unable to process the submission.<br />
	No SAMLRequest in the redirect</p>';
	
} else {
	error_log("Entering LogoutListener");

	$RelayStateURL = $_GET["RelayState"];
	error_log("RelayState = " . $RelayStateURL);
	
	//echo $samlRequest;
	
	$domrequest = processResponse($_GET, false, false, "SAMLRequest");
	
#	echo "domrequest: " . $domrequest;
	
	$issuer = getIssuerFromRequest($domrequest);
	
	//echo "Issuer: " . $issuer;

	$requestid = getLogoutRequestID($domrequest);
	
	//echo "Request ID :  [" . $requestid . "]";

	$destination = $idpMetadata[$issuer]['SingleLogOutUrl'];

	# Clear the current session! We are logging out.. Good bye...
	spi_sessionhandling_clearUserId();

/*
	echo "SP entity id: " . $spMetadata[1]['issuer'] . ":";
	echo '<pre>';
	print_r($spMetadata);
	echo '</pre>';
	*/
	
    $id = randomhex(42);
    $issueInstant = gmdate("Y-m-d\TH:i:s\Z");


	$samlResponse = '<samlp:LogoutResponse  xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
ID="_' . $id . '" Version="2.0" IssueInstant="' . $issueInstant . '" Destination="'. $destination. '" InResponseTo="' . $requestid . '">
<saml:Issuer xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion">' . getDefaultSPEntityID($spMetadata) . '</saml:Issuer>
<samlp:Status xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol">
<samlp:StatusCode  xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
Value="urn:oasis:names:tc:SAML:2.0:status:Success">
</samlp:StatusCode>
<samlp:StatusMessage xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol">
Request is done successfully
</samlp:StatusMessage>
</samlp:Status>
</samlp:LogoutResponse>';

	
	//echo "SAMLREsponseL: " . $samlResponse; 

    error_log("Logout request = " . $samlResponse);

    $encodedLogoutResponse = urlencode( base64_encode( gzdeflate( $samlResponse ) ));

    error_log("Encoded request = " . $encodedLogoutResponse);

    $redirectUrl = $destination . "?SAMLResponse=" . $encodedLogoutResponse . (isset($RelayStateURL) ? "&RelayState=" . urlencode($RelayStateURL) : '');

    error_log("Redirect URL = " . $redirectUrl);

    header("Location: " . $redirectUrl);

    exit;


}
    
    
?>
