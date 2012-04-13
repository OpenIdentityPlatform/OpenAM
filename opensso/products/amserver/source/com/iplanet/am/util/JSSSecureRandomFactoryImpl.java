/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: JSSSecureRandomFactoryImpl.java,v 1.2 2008/06/25 05:41:27 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.util;


import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import com.sun.identity.shared.debug.Debug;

/**
 * This class provides a cryptographically strong pseudo-random number 
 * generator (PRNG). The cryptographically strong pseudo-random number 
 * complies with the statistical random number generator tests 
 * specified in FIPS 140-2, Security Requirements for Cryptographic Modules
 */
public class JSSSecureRandomFactoryImpl implements SecureRandomFactory {
   private static Debug debug = Debug.getInstance("amJSS");
   static {
       JSSInit.initialize();
   }

   /*
    * Generates a SecureRandom object that implements the specified Pseudo 
    * Random Number Generator (PRNG) algorithm. 
    *
    * @return the new SecureRandom object.
    * @throw NoSuchAlgorithmException - if the PRNG algorithm is not available 
    * in the caller's environment.
    * @throw NoSuchProviderException - if the provider is not available 
    * in the caller's environment.
    */
   public SecureRandom getSecureRandomInstance() 
       throws NoSuchAlgorithmException, NoSuchProviderException {
       String method = "JSSSecureRandomFactoryImpl.getSecureRandomInstance ";
       if (debug.messageEnabled()) {
           debug.message(method + 
               "Returns SecureRandom instance of Mozilla-JSS.");
       }
       return SecureRandom.getInstance("pkcs11prng", "Mozilla-JSS");
   }
}
