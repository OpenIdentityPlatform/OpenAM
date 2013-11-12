<!--
 * The contents of this file are subject to the terms
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
 * $Id: test.php,v 1.1 2007/03/09 21:13:07 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 -->
<html>
        <head>
                <title>Test AMSDK</title>
        </head>
        <body>
                <h1>Test AMSDK</h1>
<?php
require_once 'php/io/DataInputStream.php';
require_once 'php/net/HttpClient.php';
require_once 'php/util/Properties.php';
require_once 'com/iplanet/am/util/SystemProperties.php';
require_once 'com/iplanet/services/comm/client/PLLClient.php';
require_once 'com/iplanet/services/comm/share/Request.php';
require_once 'com/iplanet/services/comm/share/RequestSet.php';
require_once 'com/iplanet/services/comm/share/Response.php';
require_once 'com/iplanet/services/comm/share/ResponseSet.php';
require_once 'com/iplanet/services/comm/share/ResponseSetParser.php';
require_once 'com/iplanet/services/naming/share/NamingRequest.php';
require_once 'com/iplanet/services/naming/share/NamingResponse.php';
require_once 'com/iplanet/services/naming/share/NamingResponseParser.php';
require_once 'com/iplanet/sso/SSOException.php';
require_once 'com/iplanet/sso/SSOProvider.php';
require_once 'com/iplanet/sso/SSOToken.php';
require_once 'com/iplanet/sso/SSOTokenID.php';
require_once 'com/iplanet/sso/providers/dpro/SSOProviderImpl.php';
require_once 'com/iplanet/sso/providers/dpro/SSOTokenImpl.php';
require_once 'com/iplanet/sso/providers/dpro/SSOTokenIDImpl.php';
require_once 'com/iplanet/dpro/session/Session.php';
require_once 'com/iplanet/dpro/session/SessionException.php';
require_once 'com/iplanet/dpro/session/SessionID.php';
require_once 'com/iplanet/dpro/session/share/SessionEncodeURL.php';
require_once 'com/iplanet/dpro/session/share/SessionInfo.php';
require_once 'com/iplanet/dpro/session/share/SessionRequest.php';
require_once 'com/iplanet/dpro/session/share/SessionRequestParser.php';
require_once 'com/iplanet/dpro/session/share/SessionResponse.php';
require_once 'com/iplanet/dpro/session/share/SessionResponseParser.php';
require_once 'com/iplanet/services/naming/WebTopNaming.php';
require_once 'com/sun/identity/session/util/RestrictedTokenContext.php';
require_once 'com/sun/identity/shared/Constants.php';


SystemProperties::mergePropertiesFromFile("/Users/ilgrosso/Sites/AMConfig.properties");
WebtopNaming::__init();
Session::__init();

$provider = new SSOProviderImpl();
try {
	$token = $provider->createSSOTokenFromRequest($_REQUEST);
	echo "<h2>Token valido: " . $token->getTokenID()->__toString() . "</h2>";
    echo "<h2>" . $token->getPrincipal() . "</h2>";
} catch (SSOException $e) {
	echo "<pre>";
	echo $e->getMessage() . "\n";
	echo $e->getTraceAsString();
	echo "</pre>";
}
?>
        <body>
</html>
