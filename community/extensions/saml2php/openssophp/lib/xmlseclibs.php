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
 * $Id: xmlseclibs.php,v 1.2 2007/11/06 16:58:33 superpat7 Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

/*
Functions to generate simple cases of Exclusive Canonical XML - Callable function is C14NGeneral() 
i.e.: $canonical = C14NGeneral($domelement, TRUE);
*/

/* helper function */
function sortAndAddAttrs($element, $arAtts) {
   $newAtts = array();
   foreach ($arAtts AS $attnode) {
      $newAtts[$attnode->nodeName] = $attnode;
   }
   ksort($newAtts);
   foreach ($newAtts as $attnode) {
      $element->setAttribute($attnode->nodeName, $attnode->nodeValue);
   }
}

/* helper function */
function canonical($tree, $element, $withcomments) {
    if ($tree->nodeType != XML_DOCUMENT_NODE) {
        $dom = $tree->ownerDocument;
    } else {
        $dom = $tree;
    }
    if ($element->nodeType != XML_ELEMENT_NODE) {
        if ($element->nodeType == XML_COMMENT_NODE && ! $withcomments) {
            return;
        }
        $tree->appendChild($dom->importNode($element, TRUE));
        return;
    }
    $arNS = array();
    if ($element->namespaceURI != "") {
        if ($element->prefix == "") {
            $elCopy = $dom->createElementNS($element->namespaceURI, $element->nodeName);
        } else {
            $prefix = $tree->lookupPrefix($element->namespaceURI);
            if ($prefix == $element->prefix) {
                $elCopy = $dom->createElementNS($element->namespaceURI, $element->nodeName);
            } else {
                $elCopy = $dom->createElement($element->nodeName);
                $arNS[$element->namespaceURI] = $element->prefix;
            }
        }
    } else {
        $elCopy = $dom->createElement($element->nodeName);
    }
    $tree->appendChild($elCopy);

    /* Create DOMXPath based on original document */
    $xPath = new DOMXPath($element->ownerDocument);

    /* Get namespaced attributes */
    $arAtts = $xPath->query('attribute::*[namespace-uri(.) != ""]', $element);

    /* Create an array with namespace URIs as keys, and sort them */
    foreach ($arAtts AS $attnode) {
        if (array_key_exists($attnode->namespaceURI, $arNS) && 
            ($arNS[$attnode->namespaceURI] == $attnode->prefix)) {
            continue;
        }
        $prefix = $tree->lookupPrefix($attnode->namespaceURI);
        if ($prefix != $attnode->prefix) {
           $arNS[$attnode->namespaceURI] = $attnode->prefix;
        } else {
            $arNS[$attnode->namespaceURI] = NULL;
        }
    }
    if (count($arNS) > 0) {
        asort($arNS);
    }

    /* Add namespace nodes */
    foreach ($arNS AS $namespaceURI=>$prefix) {
        if ($prefix != NULL) {
              $elCopy->setAttributeNS("http://www.w3.org/2000/xmlns/",
                               "xmlns:".$prefix, $namespaceURI);
        }
    }
    if (count($arNS) > 0) {
        ksort($arNS);
    }

    /* Get attributes not in a namespace, and then sort and add them */
    $arAtts = $xPath->query('attribute::*[namespace-uri(.) = ""]', $element);
    sortAndAddAttrs($elCopy, $arAtts);

    /* Loop through the URIs, and then sort and add attributes within that namespace */
    foreach ($arNS as $nsURI=>$prefix) {
       $arAtts = $xPath->query('attribute::*[namespace-uri(.) = "'.$nsURI.'"]', $element);
       sortAndAddAttrs($elCopy, $arAtts);
    }

    foreach ($element->childNodes AS $node) {
        canonical($elCopy, $node, $withcomments);
    }
}

/*
$element - DOMElement for which to produce the canonical version of
$exclusive - boolean to indicate exclusive canonicalization (must pass TRUE)
$withcomments - boolean indicating wether or not to include comments in canonicalized form
*/
function C14NGeneral($element, $exclusive=FALSE, $withcomments=FALSE) {
    /* IF PHP 5.2+ then use built in canonical functionality */
    $php_version = explode('.', PHP_VERSION);
    if (($php_version[0] > 5) || ($php_version[0] == 5 && $php_version[1] >= 2) ) {
        return $element->C14N($exclusive, $withcomments);
    }

    /* Must be element */
    if (! $element instanceof DOMElement) {
        return NULL;
    }
    /* Currently only exclusive XML is supported */
    if ($exclusive == FALSE) {
        throw new Exception("Only exclusive canonicalization is supported in this version of PHP");
    }
    
    $copyDoc = new DOMDocument();
    canonical($copyDoc, $element, $withcomments);
    return $copyDoc->saveXML($copyDoc->documentElement, LIBXML_NOEMPTYTAG);
}

class XMLSecurityKey {
    const TRIPLEDES_CBC = 'http://www.w3.org/2001/04/xmlenc#tripledes-cbc';
    const AES128_CBC = 'http://www.w3.org/2001/04/xmlenc#aes128-cbc';
    const AES192_CBC = 'http://www.w3.org/2001/04/xmlenc#aes192-cbc';
    const AES256_CBC = 'http://www.w3.org/2001/04/xmlenc#aes256-cbc';
    const RSA_1_5 = 'http://www.w3.org/2001/04/xmlenc#rsa-1_5';
    const RSA_OAEP_MGF1P = 'http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p';
    const RSA_SHA1 = 'http://www.w3.org/2000/09/xmldsig#rsa-sha1';
    const DSA_SHA1 = 'http://www.w3.org/2000/09/xmldsig#dsa-sha1';

    private $cryptParams = array();
    public $type = 0;
    public $key = NULL;
    public $passphrase = "";
    public $iv = NULL;
    public $name = NULL;
    public $keyChain = NULL;
    public $isEncrypted = FALSE;
    public $encryptedCtx = NULL;

    public function __construct($type, $params=NULL) {
        switch ($type) {
            case (XMLSecurityKey::TRIPLEDES_CBC):
                $this->cryptParams['library'] = 'mcrypt';
                $this->cryptParams['cipher'] = MCRYPT_TRIPLEDES;
                $this->cryptParams['mode'] = MCRYPT_MODE_CBC;
                $this->cryptParams['method'] = 'http://www.w3.org/2001/04/xmlenc#tripledes-cbc';
                break;
            case (XMLSecurityKey::AES128_CBC):
                $this->cryptParams['library'] = 'mcrypt';
                $this->cryptParams['cipher'] = MCRYPT_RIJNDAEL_128;
                $this->cryptParams['mode'] = MCRYPT_MODE_CBC;
                $this->cryptParams['method'] = 'http://www.w3.org/2001/04/xmlenc#aes128-cbc';
                break;
            case (XMLSecurityKey::AES192_CBC):
                $this->cryptParams['library'] = 'mcrypt';
                $this->cryptParams['cipher'] = MCRYPT_RIJNDAEL_128;
                $this->cryptParams['mode'] = MCRYPT_MODE_CBC;
                $this->cryptParams['method'] = 'http://www.w3.org/2001/04/xmlenc#aes192-cbc';
                break;
            case (XMLSecurityKey::AES256_CBC):
                $this->cryptParams['library'] = 'mcrypt';
                $this->cryptParams['cipher'] = MCRYPT_RIJNDAEL_128;
                $this->cryptParams['mode'] = MCRYPT_MODE_CBC;
                $this->cryptParams['method'] = 'http://www.w3.org/2001/04/xmlenc#aes256-cbc';
                break;
            case (XMLSecurityKey::RSA_1_5):
                $this->cryptParams['library'] = 'openssl';
                $this->cryptParams['padding'] = OPENSSL_PKCS1_PADDING;
                $this->cryptParams['method'] = 'http://www.w3.org/2001/04/xmlenc#rsa-1_5';
                if (is_array($params) && ! empty($params['type'])) {
                    if ($params['type'] == 'public' || $params['type'] == 'private') {
                        $this->cryptParams['type'] = $params['type'];
                        break;
                    }
                }
                throw new Exception('Certificate "type" (private/public) must be passed via parameters');
                return;
            case (XMLSecurityKey::RSA_OAEP_MGF1P):
                $this->cryptParams['library'] = 'openssl';
                $this->cryptParams['padding'] = OPENSSL_PKCS1_OAEP_PADDING;
                $this->cryptParams['method'] = 'http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p';
                $this->cryptParams['hash'] = NULL;
                if (is_array($params) && ! empty($params['type'])) {
                    if ($params['type'] == 'public' || $params['type'] == 'private') {
                        $this->cryptParams['type'] = $params['type'];
                        break;
                    }
                }
                throw new Exception('Certificate "type" (private/public) must be passed via parameters');
                return;
            case (XMLSecurityKey::RSA_SHA1):
                $this->cryptParams['library'] = 'openssl';
                $this->cryptParams['method'] = 'http://www.w3.org/2000/09/xmldsig#rsa-sha1';
                if (is_array($params) && ! empty($params['type'])) {
                    if ($params['type'] == 'public' || $params['type'] == 'private') {
                        $this->cryptParams['type'] = $params['type'];
                        break;
                    }
                }
                throw new Exception('Certificate "type" (private/public) must be passed via parameters');
                break;
            default:
                throw new Exception('Invalid Key Type');
                return;
        }
        $this->type = $type;
    }

    public function generateSessionKey() {
        $key = '';
        if (! empty($this->cryptParams['cipher']) && ! empty($this->cryptParams['mode'])) {
            $keysize = mcrypt_module_get_algo_key_size($this->cryptParams['cipher']);
            /* Generating random key using iv generation routines */
            if (($keysize > 0) && ($td = mcrypt_module_open(MCRYPT_RIJNDAEL_256, '',$this->cryptParams['mode'], ''))) {
                if ($this->cryptParams['cipher'] == MCRYPT_RIJNDAEL_128) {
                    $keysize = 16;
                    if ($this->type == XMLSecurityKey::AES256_CBC) {
                        $keysize = 32;
                    } elseif ($this->type == XMLSecurityKey::AES192_CBC) {
                        $keysize = 24;
                    }
                }
                while (strlen($key) < $keysize) {
                    $key .= mcrypt_create_iv(mcrypt_enc_get_iv_size ($td),MCRYPT_RAND);
                }
                mcrypt_module_close($td);
                $key = substr($key, 0, $keysize);
                $this->key = $key;
            }
        }
        return $key;
    }

    public function loadKey($key, $isFile=FALSE, $isCert = FALSE) {
        if ($isFile) {
            $this->key = file_get_contents($key);
        } else {
            $this->key = $key;
        }
        if ($isCert) {
            $this->key = openssl_x509_read($this->key);
            openssl_x509_export($this->key, $str_cert);
            $this->key = $str_cert;
        }
        if ($this->cryptParams['library'] == 'openssl') {
            if ($this->cryptParams['type'] == 'public') {
                $this->key = openssl_get_publickey($this->key);
            } else {
                $this->key = openssl_get_privatekey($this->key, $this->passphrase);
            }
        } else if ($this->cryptParams['cipher'] == MCRYPT_RIJNDAEL_128) {
            /* Check key length */
            switch ($this->type) {
                case (XMLSecurityKey::AES256_CBC):
                    if (strlen($this->key) < 25) {
                        throw new Exception('Key must contain at least 25 characters for this cipher');
                    }
                    break;
                case (XMLSecurityKey::AES192_CBC):
                    if (strlen($this->key) < 17) {
                        throw new Exception('Key must contain at least 17 characters for this cipher');
                    }
                    break;
            }
        }
    }

    private function encryptMcrypt($data) {
        $td = mcrypt_module_open($this->cryptParams['cipher'], '', $this->cryptParams['mode'], '');
        $this->iv = mcrypt_create_iv (mcrypt_enc_get_iv_size($td), MCRYPT_RAND);
        mcrypt_generic_init($td, $this->key, $this->iv);
        $encrypted_data = $this->iv.mcrypt_generic($td, $data);
        mcrypt_generic_deinit($td);
        mcrypt_module_close($td);
        return $encrypted_data;
    }

    private function decryptMcrypt($data) {
        $td = mcrypt_module_open($this->cryptParams['cipher'], '', $this->cryptParams['mode'], '');
        $iv_length = mcrypt_enc_get_iv_size($td);

        $this->iv = substr($data, 0, $iv_length);
        $data = substr($data, $iv_length);

        mcrypt_generic_init($td, $this->key, $this->iv);
        $decrypted_data = mdecrypt_generic($td, $data);
        mcrypt_generic_deinit($td);
        mcrypt_module_close($td);
        if ($this->cryptParams['mode'] == MCRYPT_MODE_CBC) {
            $dataLen = strlen($decrypted_data);
            $paddingLength = substr($decrypted_data, $dataLen - 1, 1);
            $decrypted_data = substr($decrypted_data, 0, $dataLen - ord($paddingLength));
        }
        return $decrypted_data;
    }

    private function encryptOpenSSL($data) {
        if ($this->cryptParams['type'] == 'public') {
            if (! openssl_public_encrypt($data, $encrypted_data, $this->key, $this->cryptParams['padding'])) {
                throw new Exception('Failure encrypting Data');
                return;
            }
        } else {
            if (! openssl_private_encrypt($data, $encrypted_data, $this->key, $this->cryptParams['padding'])) {
                throw new Exception('Failure encrypting Data');
                return;
            }
        }
        return $encrypted_data;
    }

    private function decryptOpenSSL($data) {
        if ($this->cryptParams['type'] == 'public') {
            if (! openssl_public_decrypt($data, $decrypted, $this->key, $this->cryptParams['padding'])) {
                throw new Exception('Failure decrypting Data');
                return;
            }
        } else {
            if (! openssl_private_decrypt($data, $decrypted, $this->key, $this->cryptParams['padding'])) {
                throw new Exception('Failure decrypting Data');
                return;
            }
        }
        return $decrypted;
    }

    private function signOpenSSL($data) {
        if (! openssl_sign ($data, $signature, $this->key)) {
            throw new Exception('Failure Signing Data');
            return;
        }
        return $signature;
    }

    private function verifyOpenSSL($data, $signature) {
        return openssl_verify ($data, $signature, $this->key);
    }

    public function encryptData($data) {
        switch ($this->cryptParams['library']) {
            case 'mcrypt':
                return $this->encryptMcrypt($data);
                break;
            case 'openssl':
                return $this->encryptOpenSSL($data);
                break;
        }
    }

    public function decryptData($data) {
        switch ($this->cryptParams['library']) {
            case 'mcrypt':
                return $this->decryptMcrypt($data);
                break;
            case 'openssl':
                return $this->decryptOpenSSL($data);
                break;
        }
    }

    public function signData($data) {
        switch ($this->cryptParams['library']) {
            case 'openssl':
                return $this->signOpenSSL($data);
                break;
        }
    }

    public function verifySignature($data, $signature) {
        switch ($this->cryptParams['library']) {
            case 'openssl':
                return $this->verifyOpenSSL($data, $signature);
                break;
        }
    }

    public function getAlgorith() {
        return $this->cryptParams['method'];
    }

    static function makeAsnSegment($type, $string) {
        switch ($type){
            case 0x02:
                if (ord($string) > 0x7f)
                    $string = chr(0).$string;
                break;
            case 0x03:
                $string = chr(0).$string;
                break;
        }
    
        $length = strlen($string);
    
        if ($length < 128){
           $output = sprintf("%c%c%s", $type, $length, $string);
        } else if ($length < 0x0100){
           $output = sprintf("%c%c%c%s", $type, 0x81, $length, $string);
        } else if ($length < 0x010000) {
           $output = sprintf("%c%c%c%c%s", $type, 0x82, $length/0x0100, $length%0x0100, $string);
        } else {
            $output = NULL;
        }
        return($output);
    }

    /* Modulus and Exponent must already be base64 decoded */
    static function convertRSA($modulus, $exponent) {
        /* make an ASN publicKeyInfo */
        $exponentEncoding = XMLSecurityKey::makeAsnSegment(0x02, $exponent);    
        $modulusEncoding = XMLSecurityKey::makeAsnSegment(0x02, $modulus);    
        $sequenceEncoding = XMLSecurityKey:: makeAsnSegment(0x30, $modulusEncoding.$exponentEncoding);
        $bitstringEncoding = XMLSecurityKey::makeAsnSegment(0x03, $sequenceEncoding);
        $rsaAlgorithmIdentifier = pack("H*", "300D06092A864886F70D0101010500"); 
        $publicKeyInfo = XMLSecurityKey::makeAsnSegment (0x30, $rsaAlgorithmIdentifier.$bitstringEncoding);

        /* encode the publicKeyInfo in base64 and add PEM brackets */
        $publicKeyInfoBase64 = base64_encode($publicKeyInfo);    
        $encoding = "-----BEGIN PUBLIC KEY-----\n";
        $offset = 0;
        while ($segment=substr($publicKeyInfoBase64, $offset, 64)){
           $encoding = $encoding.$segment."\n";
           $offset += 64;
        }
        return $encoding."-----END PUBLIC KEY-----\n";
    }
    
    public function serializeKey($parent) {
        
    }
}

class XMLSecurityDSig {
    const XMLDSIGNS = 'http://www.w3.org/2000/09/xmldsig#';
    const SHA1 = 'http://www.w3.org/2000/09/xmldsig#sha1';
    const SHA256 = 'http://www.w3.org/2001/04/xmlenc#sha256';
    const SHA512 = 'http://www.w3.org/2001/04/xmlenc#sha512';
    const RIPEMD160 = 'http://www.w3.org/2001/04/xmlenc#ripemd160';

    const C14N = 'http://www.w3.org/TR/2001/REC-xml-c14n-20010315';
    const C14N_COMMENTS = 'http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments';
    const EXC_C14N = 'http://www.w3.org/2001/10/xml-exc-c14n#';
    const EXC_C14N_COMMENTS = 'http://www.w3.org/2001/10/xml-exc-c14n#WithComments';

    const template = '<ds:Signature xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
  <ds:SignedInfo>
    <ds:SignatureMethod />
  </ds:SignedInfo>
</ds:Signature>';
    
    public $sigNode = NULL;
    public $idKeys = array();
    public $idNS = array();
    private $signedInfo = NULL;
    private $xPathCtx = NULL;
    private $canonicalMethod = NULL;
    private $prefix = 'ds';
    private $searchpfx = 'secdsig';

    public function __construct() {
        $sigdoc = new DOMDocument();
        $sigdoc->loadXML(XMLSecurityDSig::template);
        $this->sigNode = $sigdoc->documentElement;
    }

    private function getXPathObj() {
        if (empty($this->xPathCtx) && ! empty($this->sigNode)) {
            $xpath = new DOMXPath($this->sigNode->ownerDocument);
            $xpath->registerNamespace('secdsig', XMLSecurityDSig::XMLDSIGNS);
            $this->xPathCtx = $xpath;
        }
        return $this->xPathCtx;
    }

    static function generate_GUID($prefix=NULL) {
        $uuid = md5(uniqid(rand(), true));
        $guid =  $prefix.substr($uuid,0,8)."-".
                substr($uuid,8,4)."-".
                substr($uuid,12,4)."-".
                substr($uuid,16,4)."-".
                substr($uuid,20,12);
        return $guid;
    }

    public function locateSignature($objDoc) {
        if ($objDoc instanceof DOMDocument) {
            $doc = $objDoc;
        } else {
            $doc = $objDoc->ownerDocument;
        }
        if ($doc) {
            $xpath = new DOMXPath($doc);
            $xpath->registerNamespace('secdsig', XMLSecurityDSig::XMLDSIGNS);
            $query = ".//secdsig:Signature";
            $nodeset = $xpath->query($query, $objDoc);
            $this->sigNode = $nodeset->item(0);
            return $this->sigNode;
        }
        return NULL;
    }

    public function createNewSignNode($name, $value=NULL) {
        $doc = $this->sigNode->ownerDocument;
        if (! is_null($value)) {
            $node = $doc->createElementNS(XMLSecurityDSig::XMLDSIGNS, $this->prefix.':'.$name, $value);
        } else {
            $node = $doc->createElementNS(XMLSecurityDSig::XMLDSIGNS, $this->prefix.':'.$name);
        }
        return $node;
    }

    public function setCanonicalMethod($method) {
        switch ($method) {
            case 'http://www.w3.org/TR/2001/REC-xml-c14n-20010315':
            case 'http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments':
            case 'http://www.w3.org/2001/10/xml-exc-c14n#':
            case 'http://www.w3.org/2001/10/xml-exc-c14n#WithComments':
                $this->canonicalMethod = $method;
                break;
            default:
                throw new Exception('Invalid Canonical Method');
        }
        if ($xpath = $this->getXPathObj()) {
            $query = './'.$this->searchpfx.':SignedInfo';
            $nodeset = $xpath->query($query, $this->sigNode);
            if ($sinfo = $nodeset->item(0)) {
                $query = './'.$this->searchpfx.'CanonicalizationMethod';
                $nodeset = $xpath->query($query, $sinfo);
                if (! ($canonNode = $nodeset->item(0))) {
                    $canonNode = $this->createNewSignNode('CanonicalizationMethod');
                    // was $sinfo->appendChild($canonNode);
                    $sinfo->insertBefore($canonNode, $sinfo->firstChild);
                }
                $canonNode->setAttribute('Algorithm', $this->canonicalMethod);
            }
        }
    }

    private function canonicalizeData($node, $canonicalmethod) {
        $exclusive = FALSE;
        $withComments = FALSE;
        switch ($canonicalmethod) {
            case 'http://www.w3.org/TR/2001/REC-xml-c14n-20010315':
                $exclusive = FALSE;
                $withComments = FALSE;
                break;
            case 'http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments':
                $withComments = TRUE;
                break;
            case 'http://www.w3.org/2001/10/xml-exc-c14n#':
                $exclusive = TRUE;
                break;
            case 'http://www.w3.org/2001/10/xml-exc-c14n#WithComments':
                $exclusive = TRUE;
                $withComments = TRUE;
                break;
        }
/* Support PHP versions < 5.2 not containing C14N methods in DOM extension */
        $php_version = explode('.', PHP_VERSION);
        if (($php_version[0] < 5) || ($php_version[0] == 5 && $php_version[1] < 2) ) {
            return C14NGeneral($node, $exclusive, $withComments);
        }
        return $node->C14N($exclusive, $withComments);
    }

    public function canonicalizeSignedInfo() {

        $doc = $this->sigNode->ownerDocument;
        $canonicalmethod = NULL;
        if ($doc) {
            $xpath = $this->getXPathObj();
            $query = "./secdsig:SignedInfo";
            $nodeset = $xpath->query($query, $this->sigNode);
            if ($signInfoNode = $nodeset->item(0)) {
                $query = "./secdsig:CanonicalizationMethod";
                $nodeset = $xpath->query($query, $signInfoNode);
                if ($canonNode = $nodeset->item(0)) {
                    $canonicalmethod = $canonNode->getAttribute('Algorithm');
                }
                $this->signedInfo = $this->canonicalizeData($signInfoNode, $canonicalmethod);
                return $this->signedInfo;
            }
        }
        return NULL;
    }

    public function calculateDigest ($digestAlgorithm, $data) {
        switch ($digestAlgorithm) {
            case XMLSecurityDSig::SHA1:
                $alg = 'sha1';
                break;
            case XMLSecurityDSig::SHA256:
                $alg = 'sha256';
                break;
            case XMLSecurityDSig::SHA512:
                $alg = 'sha512';
                break;
            case XMLSecurityDSig::RIPEMD160:
                $alg = 'ripemd160';
                break;
            default:
                throw new Exception("Cannot validate digest: Unsupported Algorith <$digestAlgorithm>");
        }
        return base64_encode(hash($alg, $data, TRUE));
    }

    public function validateDigest($refNode, $data) {
        $xpath = new DOMXPath($refNode->ownerDocument);
        $xpath->registerNamespace('secdsig', XMLSecurityDSig::XMLDSIGNS);
        $query = 'string(./secdsig:DigestMethod/@Algorithm)';
        $digestAlgorithm = $xpath->evaluate($query, $refNode);
        $digValue = $this->calculateDigest($digestAlgorithm, $data);
        $query = 'string(./secdsig:DigestValue)';
        $digestValue = $xpath->evaluate($query, $refNode);
        return ($digValue == $digestValue);
    }

    public function processTransforms($refNode, $objData) {
        $data = $objData;
        $xpath = new DOMXPath($refNode->ownerDocument);
        $xpath->registerNamespace('secdsig', XMLSecurityDSig::XMLDSIGNS);
        $query = './secdsig:Transforms/secdsig:Transform';
        $nodelist = $xpath->query($query, $refNode);
        $canonicalMethod = 'http://www.w3.org/TR/2001/REC-xml-c14n-20010315';
        foreach ($nodelist AS $transform) {
            $algorithm = $transform->getAttribute("Algorithm");
            switch ($algorithm) {
                case 'http://www.w3.org/TR/2001/REC-xml-c14n-20010315':
                case 'http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments':
                case 'http://www.w3.org/2001/10/xml-exc-c14n#':
                case 'http://www.w3.org/2001/10/xml-exc-c14n#WithComments':
                    $canonicalMethod = $algorithm;
                    break;
            }
        }
        if ($data instanceof DOMNode) {
            $data = $this->canonicalizeData($objData, $canonicalMethod);
        }
        return $data;
    }

    public function processRefNode($refNode) {
        $dataObject = NULL;
        if ($uri = $refNode->getAttribute("URI")) {
            $arUrl = parse_url($uri);
            if (empty($arUrl['path'])) {
                if ($identifier = $arUrl['fragment']) {
                    $xPath = new DOMXPath($refNode->ownerDocument);
                    if ($this->idNS && is_array($this->idNS)) {
                        foreach ($this->idNS AS $nspf=>$ns) {
                            $xPath->registerNamespace($nspf, $ns);
                        }
                    }
                    $iDlist = '@Id="'.$identifier.'"';
                    if (is_array($this->idKeys)) {
                        foreach ($this->idKeys AS $idKey) {
                            $iDlist .= " or @$idKey='$identifier'";
                        }
                    }
                    $query = '//*['.$iDlist.']';
                    $dataObject = $xPath->query($query)->item(0);
                } else {
                    $dataObject = $refNode->ownerDocument;
                }
            } else {
                $dataObject = file_get_contents($arUrl);
            }
        } else {
            $dataObject = $refNode->ownerDocument;
        }
        $data = $this->processTransforms($refNode, $dataObject);
        return $this->validateDigest($refNode, $data);
    }

    public function validateReference() {
        $this->sigNode->parentNode->removeChild($this->sigNode);
        $doc = $this->sigNode->ownerDocument;
        $xpath = $this->getXPathObj();
        $query = "./secdsig:SignedInfo/secdsig:Reference";
        $nodeset = $xpath->query($query, $this->sigNode);
        if ($nodeset->length == 0) {
            throw new Exception("Reference nodes not found");
        }
        foreach ($nodeset AS $refNode) {
            if (! $this->processRefNode($refNode)) {
                throw new Exception("Reference validation failed");
            }
        }
        return TRUE;
    }

    private function addRefInternal($sinfoNode, $node, $algorithm, $arTransforms=NULL, $options=NULL) {
        $prefix = NULL;
        $prefix_ns = NULL;
        if (is_array($options)) {
            $prefix = empty($options['prefix'])?NULL:$options['prefix'];
            $prefix_ns = empty($options['prefix_ns'])?NULL:$options['prefix_ns'];
        }

        $refNode = $this->createNewSignNode('Reference');
        $sinfoNode->appendChild($refNode);

        $addId = 'Id';
        if (is_array($this->idKeys)) {
            $addId = $this->idKeys[0];
        }

        if ($node instanceof DOMDocument) {
            $uri = NULL;
        } else {
/* Do wer really need to set a prefix? */
            $IdNode = new DOMXPath($node->ownerDocument);
            $id = $IdNode->evaluate("//@$addId", $node)->item(0)->nodeValue;
            if(is_string($id))
                $uri = $id;
            else
                $uri = XMLSecurityDSig::generate_GUID();
            $refNode->setAttribute("URI", '#'.$uri);
        }

        $transNodes = $this->createNewSignNode('Transforms');
        $refNode->appendChild($transNodes);

        if (is_array($arTransforms)) {
            foreach ($arTransforms AS $transform) {
                $transNode = $this->createNewSignNode('Transform');
                $transNodes->appendChild($transNode);
                $transNode->setAttribute('Algorithm', $transform);
            }
        } elseif (! empty($this->canonicalMethod)) {
            $transNode = $this->createNewSignNode('Transform');
            $transNodes->appendChild($transNode);
            $transNode->setAttribute('Algorithm', $this->canonicalMethod);
        }

        if (! empty($uri)) {
            $attname = $addId;
            if (! empty($prefix)) {
                $attname = $prefix.':'.$attname;
            }
            $node->setAttributeNS($prefix_ns, $attname, $uri);
        }

        $canonicalData = $this->processTransforms($refNode, $node);
        $digValue = $this->calculateDigest($algorithm, $canonicalData);

        $digestMethod = $this->createNewSignNode('DigestMethod');
        $refNode->appendChild($digestMethod);
        $digestMethod->setAttribute('Algorithm', $algorithm);

        $digestValue = $this->createNewSignNode('DigestValue', $digValue);
        $refNode->appendChild($digestValue);
    }

    public function addReference($node, $algorithm, $arTransforms=NULL, $options=NULL) {
        if ($xpath = $this->getXPathObj()) {
            $query = "./secdsig:SignedInfo";
            $nodeset = $xpath->query($query, $this->sigNode);
            if ($sInfo = $nodeset->item(0)) {
                $this->addRefInternal($sInfo, $node, $algorithm, $arTransforms, $options);
            }
        }
    }

    public function addReferenceList($arNodes, $algorithm, $arTransforms=NULL, $options=NULL) {
        if ($xpath = $this->getXPathObj()) {
            $query = "./secdsig:SignedInfo";
            $nodeset = $xpath->query($query, $this->sigNode);
            if ($sInfo = $nodeset->item(0)) {
                foreach ($arNodes AS $node) {
                    $this->addRefInternal($sInfo, $node, $algorithm, $arTransforms, $options);
                }
            }
        }
    }

    public function locateKey($node=NULL) {
        if (empty($node)) {
            $node = $this->sigNode;
        }
        if (! $node instanceof DOMNode) {
            return NULL;
        }
        if ($doc = $node->ownerDocument) {
            $xpath = new DOMXPath($doc);
            $xpath->registerNamespace('secdsig', XMLSecurityDSig::XMLDSIGNS);
            $query = "string(./secdsig:SignedInfo/secdsig:SignatureMethod/@Algorithm)";
            $algorithm = $xpath->evaluate($query, $node);
            if ($algorithm) {
                try {
                    $objKey = new XMLSecurityKey($algorithm, array('type'=>'public'));
                } catch (Exception $e) {
                    return NULL;
                }
                return $objKey;
            }
        }
        return NULL;
    }
    
    public function verify($objKey) {
        $doc = $this->sigNode->ownerDocument;
        $xpath = new DOMXPath($doc);
        $xpath->registerNamespace('secdsig', XMLSecurityDSig::XMLDSIGNS);
        $query = "string(./secdsig:SignatureValue)";
        $sigValue = $xpath->evaluate($query, $this->sigNode);
        if (empty($sigValue)) {
            throw new Exception("Unable to locate SignatureValue");
        }
        return $objKey->verifySignature($this->signedInfo, base64_decode($sigValue));
    }
    
    public function signData($objKey, $data) {
        return $objKey->signData($data);
    }
    
    public function sign($objKey) {
        if ($xpath = $this->getXPathObj()) {
            $query = "./secdsig:SignedInfo";
            $nodeset = $xpath->query($query, $this->sigNode);
            if ($sInfo = $nodeset->item(0)) {
                $query = "./secdsig:SignatureMethod";
                $nodeset = $xpath->query($query, $sInfo);
                $sMethod = $nodeset->item(0);
                $sMethod->setAttribute('Algorithm', $objKey->type);
                $data = $this->canonicalizeData($sInfo, $this->canonicalMethod);
                $sigValue = base64_encode($this->signData($objKey, $data));
                $sigValueNode = $this->createNewSignNode('SignatureValue', $sigValue);
                $this->sigNode->appendChild($sigValueNode);
            }
        }
    }

    public function appendCert() {
        
    }

    public function appendKey($objKey, $parent=NULL) {
        $objKey->serializeKey($parent);
    }

    public function appendSignature($parentNode, $insertBefore = FALSE) {
        $baseDoc = ($parentNode instanceof DOMDocument)?$parentNode:$parentNode->ownerDocument;
        $newSig = $baseDoc->importNode($this->sigNode, TRUE);
        if ($insertBefore) {
            $parentNode->insertBefore($newSig, $parentNode->firstChild);
        } else {
            $parentNode->appendChild($newSig);
        }
    }
    
    static function get509XCert($cert, $isPEMFormat=TRUE) {
        if ($isPEMFormat) {
            $data = '';
            $arCert = explode("\n", $cert);
            $inData = FALSE;
            foreach ($arCert AS $curData) {
                if (! $inData) {
                    if (strncmp($curData, '-----BEGIN CERTIFICATE', 22) == 0) {
                        $inData = TRUE;
                    }
                } else {
                    if (strncmp($curData, '-----END CERTIFICATE', 20) == 0) {
                        break;
                    }
                    $data .= trim($curData);
                }
            }
        } else {
            $data = $cert;
        }
        return $data;
    }
    
    public function add509Cert($cert, $isPEMFormat=TRUE) {
        $data = XMLSecurityDSig::get509XCert($cert, $isPEMFormat);
        if ($xpath = $this->getXPathObj()) {
            $query = "./secdsig:KeyInfo";
            $nodeset = $xpath->query($query, $this->sigNode);
            $keyInfo = $nodeset->item(0);
            if (! $keyInfo) {
                $keyInfo = $this->createNewSignNode('KeyInfo');
                $this->sigNode->appendChild($keyInfo);
            }
            $x509DataNode = $this->createNewSignNode('X509Data');
            $keyInfo->appendChild($x509DataNode);
            $x509CertNode = $this->createNewSignNode('X509Certificate', $data);
            $x509DataNode->appendChild($x509CertNode);
        }
    }
}

class XMLSecEnc {
    const template = "<xenc:EncryptedData xmlns:xenc='http://www.w3.org/2001/04/xmlenc#'>
   <xenc:CipherData>
      <xenc:CipherValue></xenc:CipherValue>
   </xenc:CipherData>
</xenc:EncryptedData>";

    const Element = 'http://www.w3.org/2001/04/xmlenc#Element';
    const Content = 'http://www.w3.org/2001/04/xmlenc#Content';
    const URI = 3;
    const XMLENCNS = 'http://www.w3.org/2001/04/xmlenc#';

    private $encdoc = NULL;
    private $rawNode = NULL;
    public $type = NULL;
    
    public function __construct() {
        $this->encdoc = new DOMDocument();
        $this->encdoc->loadXML(XMLSecEnc::template);
    }
    
    public function setNode($node) {
        $this->rawNode = $node;
    }

    public function encryptNode($objKey, $replace=TRUE) {
        $data = '';
        if (empty($this->rawNode)) {
            throw new Exception('Node to encrypt has not been set');
        }
        $doc = $this->rawNode->ownerDocument;
        $xPath = new DOMXPath($this->encdoc);
        $objList = $xPath->query('/xenc:EncryptedData/xenc:CipherData/xenc:CipherValue');
        $cipherValue = $objList->item(0);
        if ($cipherValue == NULL) {
            throw new Exception('Error locating CipherValue element within template');
        }
        switch ($this->type) {
            case (XMLSecEnc::Element):
                $data = $doc->saveXML($this->rawNode);
                $this->encdoc->documentElement->setAttribute('Type', XMLSecEnc::Element);
                break;
            case (XMLSecEnc::Content):
                $children = $this->sawNode->childNodes;
                foreach ($children AS $child) {
                    $data .= $doc->saveXML($child);
                }
                $this->encdoc->documentElement->setAttribute('Type', XMLSecEnc::Content);
                break;
            default:
                throw new Exception('Type is currently not supported');
                return;
        }
        
        $encMethod = $this->encdoc->documentElement->appendChild($this->encdoc->createElementNS(XMLSecEnc::XMLENCNS, 'xenc:EncryptionMethod'));
        $encMethod->setAttribute('Algorithm', $objKey->getAlgorith());
        $cipherValue->parentNode->parentNode->insertBefore($encMethod, $cipherValue->parentNode);
        
        $strEncrypt = base64_encode($objKey->encryptData($data));
        $value = $this->encdoc->createTextNode($strEncrypt);
        $cipherValue->appendChild($value);
        
        if ($replace) {
            switch ($this->type) {
                case (XMLSecEnc::Element):
                    if ($this->rawNode->nodeType == XML_DOCUMENT_NODE) {
                        return $this->encdoc;
                    }
                    $importEnc = $this->rawNode->ownerDocument->importNode($this->encdoc->documentElement, TRUE);
                    $this->rawNode->parentNode->replaceChild($importEnc, $this->rawNode);
                    return $importEnc;
                    break;
                case (XMLSecEnc::Content):
                    $importEnc = $this->rawNode->ownerDocument->importNode($this->encdoc->documentElement, TRUE);
                    while($this->rawNode->firstChild) {
                        $this->rawNode->removeChild($this->rawNode->firstChild);
                    }
                    $this->rawNode->appendChild($importEnc);
                    break;
            }
        }
    }

    public function decryptNode($objKey, $replace=TRUE) {
        $data = '';
        if (empty($this->rawNode)) {
            throw new Exception('Node to decrypt has not been set');
        }
        $doc = $this->rawNode->ownerDocument;
        $xPath = new DOMXPath($doc);
        $xPath->registerNamespace('xmlencr', XMLSecEnc::XMLENCNS);
        /* Only handles embedded content right now and not a reference */
        $query = "./xmlencr:CipherData/xmlencr:CipherValue";
        $nodeset = $xPath->query($query, $this->rawNode);

        if ($node = $nodeset->item(0)) {
            $encryptedData = base64_decode($node->nodeValue);
            $decrypted = $objKey->decryptData($encryptedData);
            if ($replace) {
                switch ($this->type) {
                    case (XMLSecEnc::Element):
                        $newdoc = new DOMDocument();
                        $newdoc->loadXML($decrypted);
                        if ($this->rawNode->nodeType == XML_DOCUMENT_NODE) {
                            return $newdoc;
                        }
                        $importEnc = $this->rawNode->ownerDocument->importNode($newdoc->documentElement, TRUE);
                        $this->rawNode->parentNode->replaceChild($importEnc, $this->rawNode);
                        return $importEnc;
                        break;
                    case (XMLSecEnc::Content):
                        if ($this->rawNode->nodeType == XML_DOCUMENT_NODE) {
                            $doc = $this->rawNode;
                        } else {
                            $doc = $this->rawNode->ownerDocument;
                        }
                        $newFrag = $doc->createDOMDocumentFragment();
                        $newFrag->appendXML($decrypted);
                        $this->rawNode->parentNode->replaceChild($newFrag, $this->rawNode);
                        return $this->rawNode->parentNode;
                        break;
                    default:
                        return $decrypted;
                }
            } else {
                return $decrypted;
            }
        } else {
            throw new Exception("Cannot locate encrypted data");
        }
    }

    public function encryptKey($srcKey, $rawKey, $append=TRUE) {
        $strEncKey = base64_encode($srcKey->encryptData($rawKey->key));
        $root = $this->encdoc->documentElement;
        $encKey = $this->encdoc->createElementNS(XMLSecEnc::XMLENCNS, 'xenc:EncryptedKey');
        if (append) {
            $root->appendChild($encKey);
        }
        $encMethod = $encKey->appendChild($this->encdoc->createElementNS(XMLSecEnc::XMLENCNS, 'xenc:EncryptionMethod'));
        $encMethod->setAttribute('Algorithm', $srcKey->getAlgorith());
        if (! empty($srcKey->name)) {
            $keyInfo = $encKey->appendChild($this->encdoc->createElementNS('http://www.w3.org/2000/09/xmldsig#', 'dsig:KeyInfo'));
            $keyInfo->appendChild($this->encdoc->createElementNS('http://www.w3.org/2000/09/xmldsig#', 'dsig:KeyName', $srcKey->name));
        }
        $cipherData = $encKey->appendChild($this->encdoc->createElementNS(XMLSecEnc::XMLENCNS, 'xenc:CipherData'));
        $cipherData->appendChild($this->encdoc->createElementNS(XMLSecEnc::XMLENCNS, 'xenc:CipherValue', $strEncKey));
        return;
    }
    
    public function decryptKey($encKey) {
        if (! $encKey->isEncrypted) {
            throw new Exception("Key is not Encrypted");
        }
        if (empty($encKey->key)) {
            throw new Exception("Key is missing data to perform the decryption");
        }
        return $this->decryptNode($encKey, FALSE);
    }
    
    public function locateEncryptedData($element) {
        if ($element instanceof DOMDocument) {
            $doc = $element;
        } else {
            $doc = $element->ownerDocument;
        }
        if ($doc) {
            $xpath = new DOMXPath($doc);
            $query = "//*[local-name()='EncryptedData' and namespace-uri()='".XMLSecEnc::XMLENCNS."']";
            $nodeset = $xpath->query($query);
            return $nodeset->item(0);
        }
        return NULL;
    }
    
    public function locateKey($node=NULL) {
        if (empty($node)) {
            $node = $this->rawNode;
        }
        if (! $node instanceof DOMNode) {
            return NULL;
        }
        if ($doc = $node->ownerDocument) {
            $xpath = new DOMXPath($doc);
            $xpath->registerNamespace('xmlsecenc', XMLSecEnc::XMLENCNS);
            $query = ".//xmlsecenc:EncryptionMethod";
            $nodeset = $xpath->query($query, $node);
            if ($encmeth = $nodeset->item(0)) {
                   $attrAlgorithm = $encmeth->getAttribute("Algorithm");
                try {
                    $objKey = new XMLSecurityKey($attrAlgorithm, array('type'=>'private'));
                } catch (Exception $e) {
                    return NULL;
                }
                return $objKey;
            }
        }
        return NULL;
    }
    
    static function staticLocateKeyInfo($objBaseKey=NULL, $node=NULL) {
        if (empty($node) || (! $node instanceof DOMNode)) {
            return NULL;
        }
        if ($doc = $node->ownerDocument) {
            $xpath = new DOMXPath($doc);
            $xpath->registerNamespace('xmlsecenc', XMLSecEnc::XMLENCNS);
            $xpath->registerNamespace('xmlsecdsig', XMLSecurityDSig::XMLDSIGNS);
            $query = "./xmlsecdsig:KeyInfo";
            $nodeset = $xpath->query($query, $node);
            if ($encmeth = $nodeset->item(0)) {
                foreach ($encmeth->childNodes AS $child) {
                    switch ($child->localName) {
                        case 'KeyName':
                            if (! empty($objBaseKey)) {
                                $objBaseKey->name = $child->nodeValue;
                            }
                            break;
                        case 'KeyValue':
                            foreach ($child->childNodes AS $keyval) {
                                switch ($keyval->localName) {
                                    case 'DSAKeyValue':
                                        throw new Exception("DSAKeyValue currently not supported");
                                        break;
                                    case 'RSAKeyValue':
                                        $modulus = NULL;
                                        $exponent = NULL;
                                        if ($modulusNode = $keyval->getElementsByTagName('Modulus')->item(0)) {
                                            $modulus = base64_decode($modulusNode->nodeValue);
                                        }
                                        if ($exponentNode = $keyval->getElementsByTagName('Exponent')->item(0)) {
                                            $exponent = base64_decode($exponentNode->nodeValue);
                                        }
                                        if (empty($modulus) || empty($exponent)) {
                                            throw new Exception("Missing Modulus or Exponent");
                                        }
                                        $publicKey = XMLSecurityKey::convertRSA($modulus, $exponent);
                                        $objBaseKey->loadKey($publicKey);
                                        break;
                                }
                            }
                            break;
                        case 'RetrievalMethod':
                            /* Not currently supported */
                            break;
                        case 'EncryptedKey':
                            $objenc = new XMLSecEnc();
                            $objenc->setNode($child);
                            if (! $objKey = $objenc->locateKey()) {
                                throw new Exception("Unable to locate algorithm for this Encrypted Key");
                            }
                            $objKey->isEncrypted = TRUE;
                            $objKey->encryptedCtx = $objenc;
                            XMLSecEnc::staticLocateKeyInfo($objKey, $child);
                            return $objKey;
                            break;
                        case 'X509Data':
                            if ($x509certNodes = $child->getElementsByTagName('X509Certificate')) {
                                if ($x509certNodes->length > 0) {
                                    $x509cert = $x509certNodes->item(0)->textContent;
                                    // @PAT@ - Just remove any CR/LFs and put them back where they should be
                                    $x509cert = str_replace(array("\r", "\n"), "", $x509cert); 
                                    $x509cert = "-----BEGIN CERTIFICATE-----\n".chunk_split($x509cert, 64, "\n")."-----END CERTIFICATE-----\n";
                                    $objBaseKey->loadKey($x509cert);
                                }
                            }
                            break;
                    }
                }
            }
            return $objBaseKey;
        }
        return NULL;
    }
    
    public function locateKeyInfo($objBaseKey=NULL, $node=NULL) {
        if (empty($node)) {
            $node = $this->rawNode;
        }
        return XMLSecEnc::staticLocateKeyInfo($objBaseKey, $node);
    }
}
?>
