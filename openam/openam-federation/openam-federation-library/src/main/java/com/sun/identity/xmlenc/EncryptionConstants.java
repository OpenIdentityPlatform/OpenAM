/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
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
 * $Id: EncryptionConstants.java,v 1.3 2008/06/25 05:48:16 qcheng Exp $
 *
 */


package com.sun.identity.xmlenc;

public class EncryptionConstants {

    public static final String ENC_XML_NS = "http://www.w3.org/2001/04/xmlenc#";
    public static final String RSA = "RSA";
    public static final String AES = "AES";
    public static final String TRIPLEDES = "DESede";
    public static final String XML_ENCRYPTION_PROVIDER_KEY = 
           "com.sun.identity.xmlenc.EncryptionProviderImpl";

    public static final String ENC_DATA_ENC_METHOD_3DES = 
        "http://www.w3.org/2001/04/xmlenc#tripledes-cbc";
    public static final String ENC_DATA_ENC_METHOD_AES_128 = 
        "http://www.w3.org/2001/04/xmlenc#aes128-cbc"; 
    public static final String ENC_DATA_ENC_METHOD_AES_256 = 
        "http://www.w3.org/2001/04/xmlenc#aes256-cbc";
    
    public static final String ENC_KEY_ENC_METHOD_RSA_1_5 = 
        "http://www.w3.org/2001/04/xmlenc#rsa-1_5";
    public static final String ENC_KEY_ENC_METHOD_RSA_OAEP = 
        "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p";
    public static final String ENC_KEY_ENC_METHOD_3DES = 
        "http://www.w3.org/2001/04/xmlenc#kw-tripledes";
    public static final String ENC_KEY_ENC_METHOD_AES_128 = 
        "http://www.w3.org/2001/04/xmlenc#kw-aes128";
    public static final String ENC_KEY_ENC_METHOD_AES_256 = 
        "http://www.w3.org/2001/04/xmlenc#kw-aes256";
        
}
