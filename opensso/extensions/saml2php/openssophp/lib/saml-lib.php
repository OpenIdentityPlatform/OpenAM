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
 * $Id: saml-lib.php,v 1.3 2007/11/06 16:58:33 superpat7 Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */


require_once($LIGHTBULB_CONFIG['basedir'] . 'lib/xmlseclibs.php');
require_once($LIGHTBULB_CONFIG['basedir'] . 'config/saml-metadata-IdP.php');
		
		define('SAML2_ASSERT_NS', 'urn:oasis:names:tc:SAML:2.0:assertion');
		define('SAML2_PROTOCOL_NS', 'urn:oasis:names:tc:SAML:2.0:protocol');

		define('SAML2_BINDINGS_POST', 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST');
		define('SAML2_BINDINGS_POST_SIMPLESIGN', 'urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST-SimpleSign');

		define('SAML2_STATUS_SUCCESS', 'urn:oasis:names:tc:SAML:2.0:status:Success');

	function processLogoutRequest($samlRequest,$validate=TRUE) {
		try {
			$token = new DOMDocument();
			$token->loadXML(str_replace ("\r", "", $samlResponse));
			if (empty($token)) {
				throw new Exception("Unable to load token");
			}
	
			return $token;
		} catch (DOMException $domE) {
			print "DOM Error: ".$domE->getMessage();
		} catch (Exception $e) {
			print 'Error: '.$e->getMessage();
		}
		return NULL;
	}

	function signXML($token, $privkey) {
        $sigdoc = new DOMDocument();
        if(!$sigdoc->loadXML($token)){
            throw new Exception("Invalid XML!");
        }
        $sigNode = $sigdoc->firstChild;

        $enc = new XMLSecurityDSig();
        $enc->idKeys[] = 'ID';
        $enc->setCanonicalMethod(XMLSecurityDSig::EXC_C14N);
        $enc->addReference(
            $sigNode,
            XMLSecurityDSig::SHA1,
            array(
                'http://www.w3.org/2000/09/xmldsig#enveloped-signature',
                XMLSecurityDSig::EXC_C14N
            )
        );

        $key = new XMLSecurityKey(
            XMLSecurityKey::RSA_SHA1,
            array(
                'type' => 'private',
                'library' => 'openssl'
            )
        );

        $key->loadKey($privkey, false, false);

        $enc->sign($key);
        $enc->appendSignature($sigNode);
        return $sigdoc->saveXML();
    }

	function processResponse($params, $validate=TRUE, $postBinding=TRUE, $samlParam="SAMLResponse") {
		global $idpMetadata;

        $rawData = $params[$samlParam];
        error_log("Raw SAML data: " . $rawData );

        // $rawData is ready URL decoded...
		if ( $postBinding )
		{
	        error_log("Decoding POST binding" );
	        $samlData = base64_decode( $rawData );
		}
		else
		{
	        error_log("Decoding Redirect binding" );
	        $samlData = gzinflate(base64_decode($rawData));
		}
        error_log("Decoded SAML data = " . $samlData );

		try {
			$token = new DOMDocument();
			$token->loadXML(str_replace ("\r", "", $samlData));
			if (empty($token)) {
				throw new Exception("Unable to load token");
			}
	
			if ( $validate ) {
				if ($issuerNodes = $token->getElementsByTagName('Issuer')) {
					if ($issuerNodes->length > 0) {
						$issuer = $issuerNodes->item(0)->textContent;
					}
				}
		        error_log("Issuer = " . $issuer );
				$binding = $idpMetadata[$issuer]['SingleSignOnBinding'];
		        error_log("Binding = " . $binding );
				if (isset($binding) && (strcmp($binding, SAML2_BINDINGS_POST_SIMPLESIGN) == 0 ))
				{
			        error_log("Checking simple signature");
					$valid = checkSimpleSignature($params,$idpMetadata[$issuer]['certificate']);
				}
				else
				{
			        error_log("Checking XML signature");
					$valid = checkXMLSignature($token);
				}

				if ( ! $valid ) {
					throw new Exception("Unable to validate Signature");
				}
			}

			return $token;
		} catch (DOMException $domE) {
			print "DOM Error: ".$domE->getMessage();
		} catch (Exception $e) {
			print 'Error: '.$e->getMessage();
		}
		return NULL;
	}

	function checkXMLSignature($token) {
		$objXMLSecDSig = new XMLSecurityDSig();
		$objXMLSecDSig->idKeys[] = 'ID';
		$objDSig = $objXMLSecDSig->locateSignature($token);

		/* Must check certificate fingerprint now - validateReference removes it */        
		if ( ! validateCertFingerprint($token) )
		{
			throw new Exception("Fingerprint Validation Failed");
		}

		/* Canonicalize the signed info */
		$objXMLSecDSig->canonicalizeSignedInfo();

		$retVal = NULL;
		if ($objDSig) {
			$retVal = $objXMLSecDSig->validateReference();
		}
		if (! $retVal) {
			throw new Exception("SAML Validation Failed");
		}

		$key = NULL;
		$objKey = $objXMLSecDSig->locateKey();
	
		if ($objKey) {
			if ($objKeyInfo = XMLSecEnc::staticLocateKeyInfo($objKey, $objDSig)) {
				/* Handle any additional key processing such as encrypted keys here */
			}
		}
	
		if (empty($objKey)) {
			throw new Exception("Error loading key to handle Signature");
		}

		return ($objXMLSecDSig->verify($objKey)==1);
	}

	function checkSimpleSignature($params,$cert) {
        $samlResponse = base64_decode( $params['SAMLResponse'] );

		$rawSignature = $params['Signature'];
        error_log("Raw Signature: " . $rawSignature );

		$signature = base64_decode($rawSignature);

		$relayState = $params['RelayState'];
		error_log("RelayState: " . $relayState );

		$sigAlg = $params['SigAlg'];
		error_log("SigAlg: " . $sigAlg );

		if (strcmp($sigAlg,XMLSecurityKey::RSA_SHA1) != 0) {
			throw new Exception("Signature algorithm ".$sigAlg." is not supported");
		}

		if ( isset($params['RelayState'] ) ) {
			$signedData = "SAMLResponse=".$samlResponse."&RelayState=".$relayState."&SigAlg=".$sigAlg;
		} else {
			$signedData = "SAMLResponse=".$samlResponse."&SigAlg=".$sigAlg;
		}
		error_log("Signed data: " . $signedData );

		return (openssl_verify($signedData, $signature, $cert) == 1);
	}

	function checkDateConditions($start=NULL, $end=NULL) {
		$currentTime = time();
	
		if (! empty($start)) {
			$startTime = strtotime($start);
			/* Allow for a 10 minute difference in Time */
			if (($startTime < 0) || (($startTime - 600) > $currentTime)) {
				return FALSE;
			}
		}
		if (! empty($end)) {
			$endTime = strtotime($end);
			if (($endTime < 0) || ($endTime <= $currentTime)) {
				return FALSE;
			}
		}
		return TRUE;
	}
	
	// getAttributes parses the SAML 2.0 AuthNResponse and validates and returns all the 
	// attributes in the first statement
	function getAttributes($token) {
		
		$attributes = array();
		
		if ($token instanceof DOMDocument) {
		
			/*
			echo "<PRE>token:";
			echo htmlentities($token->saveXML());
			echo ":</PRE>";
			*/
			
			$xPath = new DOMXpath($token);
			$xPath->registerNamespace("mysaml", SAML2_ASSERT_NS);
			$xPath->registerNamespace("mysamlp", SAML2_PROTOCOL_NS);
			$query = "/mysamlp:Response/mysaml:Assertion/mysaml:Conditions";
			$nodelist = $xPath->query($query);
		
			if ($node = $nodelist->item(0)) {
		
				$start = $node->getAttribute("NotBefore");
				$end = $node->getAttribute("NotOnOrAfter");
	
				if (! checkDateConditions($start, $end)) {
					echo " Date check failed ... (from $start to $end)";
		
					return $attributes;
				}
			}
	
			$query = "/mysamlp:Response/mysaml:Assertion/mysaml:AttributeStatement/mysaml:Attribute";
			$nodelist = $xPath->query($query);
			
			//echo "Before iterate";
			foreach ($nodelist AS $node) {
				//echo "Node $node ";
				if ($name = $node->getAttribute("Name")) {
					//echo "Name ";
					$value = "";
					foreach ($node->childNodes AS $child) {
						if ($child->localName == "AttributeValue") {
							$value = $child->textContent;
							break;
						}
					}
					$attributes[$name] = $value;
				}
			}
		}
		return $attributes;
	}
	
	function getNameId($token) {
		$nameID = array();
		if ($token instanceof DOMDocument) {
			$xPath = new DOMXpath($token);
			$xPath->registerNamespace('mysaml', SAML2_ASSERT_NS);
			$xPath->registerNamespace('mysamlp', SAML2_PROTOCOL_NS);
	
			$query = '/mysamlp:Response/mysaml:Assertion/mysaml:Subject/mysaml:NameID';
			$nodelist = $xPath->query($query);
			if ($node = $nodelist->item(0)) {
				$nameID["NameID"] = $node->nodeValue;
				$nameID["NameQualifier"] = $node->getAttribute('NameQualifier');
				$nameID["SPNameQualifier"] = $node->getAttribute('SPNameQualifier');
			}
		}
		return $nameID;
	}
	
	function getIssuer($token) {
		if ($token instanceof DOMDocument) {
			$xPath = new DOMXpath($token);
			$xPath->registerNamespace('mysaml', SAML2_ASSERT_NS);
			$xPath->registerNamespace('mysamlp', SAML2_PROTOCOL_NS);
	
			$query = '/mysamlp:Response/mysaml:Issuer';
			$nodelist = $xPath->query($query);
			if ($node = $nodelist->item(0)) {
				return $node->nodeValue;
			}
		}
		return NULL;
	}
	
	function getIssuerFromRequest($token) {
		if ($token instanceof DOMDocument) {
			$xPath = new DOMXpath($token);
			$xPath->registerNamespace('mysaml', SAML2_ASSERT_NS);
			$xPath->registerNamespace('mysamlp', SAML2_PROTOCOL_NS);
	
			$query = '//mysaml:Issuer';
			$nodelist = $xPath->query($query);
			if ($node = $nodelist->item(0)) {
				return $node->nodeValue;
			}
		}
		return NULL;
	}
	
	
	function getLogoutRequestID($token) {
		if ($token instanceof DOMDocument) {
			$xPath = new DOMXpath($token);
			$xPath->registerNamespace('mysaml', SAML2_ASSERT_NS);
			$xPath->registerNamespace('mysamlp', SAML2_PROTOCOL_NS);
	
			$query = '/mysamlp:LogoutRequest';
			$nodelist = $xPath->query($query);
			if ($node = $nodelist->item(0)) {
				return $node->getAttribute('ID');
			}
		}
		return NULL;
	}
	
	
	
	function getSessionIndex($token) {
		if ($token instanceof DOMDocument) {
			$xPath = new DOMXpath($token);
			$xPath->registerNamespace('mysaml', SAML2_ASSERT_NS);
			$xPath->registerNamespace('mysamlp', SAML2_PROTOCOL_NS);
	
			$query = '/mysamlp:Response/mysaml:Assertion/mysaml:AuthnStatement';
			$nodelist = $xPath->query($query);
			if ($node = $nodelist->item(0)) {
				return $node->getAttribute('SessionIndex');
			}
		}
		return NULL;
	}
	
	function getLogoutResponseStatus($token) {
		if ($token instanceof DOMDocument) {
			$xPath = new DOMXpath($token);
			$xPath->registerNamespace('mysamlp', SAML2_PROTOCOL_NS);
	
			$query = '/mysamlp:LogoutResponse/mysamlp:Status/mysamlp:StatusCode';
			$nodelist = $xPath->query($query);
			if ($node = $nodelist->item(0)) {
				return $node->getAttribute('Value');
			}
		}
		return NULL;
	}
	
	function validateCertFingerprint($token) {
		global $idpMetadata;
		$fingerprint = "";
		if ($x509certNodes = $token->getElementsByTagName('X509Certificate')) {
			if ($x509certNodes->length > 0) {
				$x509cert = $x509certNodes->item(0)->textContent;
				$x509data = base64_decode( $x509cert );
				$fingerprint = strtolower( sha1( $x509data ) );
			}
		}
	
		if ($issuerNodes = $token->getElementsByTagName('Issuer')) {
			if ($issuerNodes->length > 0) {
				$issuer = $issuerNodes->item(0)->textContent;
			}
		}
	
		// Accept fingerprints with or without colons, case insensitive
		$issuerFingerprint = strtolower( str_replace(":", "", $idpMetadata[$issuer]['certFingerprint']) );
	
		if ($fingerprint != $issuerFingerprint) {
			echo "Expecting fingerprint $issuerFingerprint but got fingerprint $fingerprint .st";
		}
	
		return ($fingerprint == $issuerFingerprint);
	}
	
	function randomhex($length)
	{
		$key = "";
		for ( $i=0; $i < $length; $i++ )
		{
			 $key .= dechex( rand(0,15) );
		}
		return $key;
	}

?>
