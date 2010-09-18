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
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

public interface WSSConstants {
    
    /*
     * Property for WSS agent profile name 
     */
    String KEY_WSC_NAME = "name";
    
    /*
     * Property for WSS agent profile security mechanism
     */
    String KEY_SEC_MECHANISM = "secMechanism";
    
    /*
     * Property for WSS agent profile user credential
     */
    String KEY_HAS_USER_CREDENTIAL = "hasUserCredential";
    
    /*
     * Property for request signed
     */
    String KEY_IS_REQ_SIGNED = "isRequestSigned";
    
    /*
     * Property for request encrypted
     */
    String KEY_IS_REQ_ENCRYPTED = "isRequestEncrypted";
            
    /*
     * Property for response signature verified
     */
    String KEY_IS_RESP_SIG_VERIFIED = "isResponseSigVerified";
            
    /*
     * Property for response decrypted
     */
    String KEY_IS_RESP_DECRYPTED = "isResponseDecrypted";
    
    /*
     * Property for keystore usage i.e default or custom
     */
    String KEY_KEYSTORE_USAGE = "keystoreUsage";
    
    /*
     * Property for keystore file lcoation when using custom keystore
     */
    String KEY_KEYSTORE_FILE = "KeyStoreFile";
    
    /*
     * Property for keystore password
     */
    String KEY_KEYSTORE_PASSWORD = "KeyStorePassword";
    
    /*
     * Property for key password
     */
    String KEY_KEYPASSWORD = "KeyPassword";
    
    /*
     * Property for keeping security headers in message
     */
    String KEY_PRIVATE_SEC_HEADERS = "keepPrivateSecHeaderInMsg";
    
    /*
     * Property for service type
     */
    String KEY_SVC_TYPE = "svcType";
    
    /*
     * Property for wsp end point
     */
    String KEY_WSP_ENDPOINT = "WSPEndpoint";
    
    /*
     * Property for signing reference type
     */
    String KEY_SIGNING_REF_TYPE = "SigningRefType";
    
    /*
     * Property for encryption strength
     */
    String KEY_ENC_STRENGTH = "EncryptionStrength";
    
    /*
     * Property for encryption algorithm
     */
    String KEY_ENC_ALGORITHM = "EncryptionAlgorithm";
    
    /*
     * Property for to force end user authentication
     */
    String KEY_FORCE_USER_AUTH = "setForceUserAuthentication";
    
    /*
     * Property for public key alias
     */
    String KEY_PUBLIC_KEY_ALIAS = "publicKeyAlias";
    
    /*
     * Property for Private key alias
     */
    String KEY_PRIVATE_KEY_ALIAS = "privateKeyAlias";
    
    /*
     * Property for STS client WS Trust version
     */
    String KEY_WSTRUSTVERSION ="WSTrustVersion";
    
    /*
     * Property for STS client STS End Point 
     */
    String KEY_STSENDPOINT="STSEndpoint";
    
    /*
     * Property for STS client Mex End Point 
     */
    String KEY_STSMEXENDPOINT="STSMexEndpoint";
    
    /*
     * Property for STS service 
     */
    String KEY_STS_IS_REQ_SIGNED = "isRequestSign";

    /*
     * Property for STS service
     */
    String KEY_STS_IS_RESP_SIGNED = "isResponseSign";

    /*
     * Property for STS service
     */
    String KEY_STS_IS_REQ_ENCRYPTED = "isRequestEncrypt";

     /*
     * Property for STS service
     */
    String KEY_STS_IS_RESP_ENCRYPTED = "isResponseEncrypt";
   
 
    /*
     * Property for qatest to identitfy is STS service is remote or local
     */
    String KEY_STSSERVICETYPE = "stsservicetype";
    
    /*
     * Property for STS service signing request 
     */
    String KEY_STSREQSIGN = "isRequestSign";
    
    /*
     * Property for STS service User Credential
     */
    String KEY_STS_USERCRED = "UserCredential";
 
    /*
     * Property for additional end user credentials for the wsp
     */
    String KEY_END_USER_CREDENTIALS = "EndUserCredentials";
    
   
    
}
