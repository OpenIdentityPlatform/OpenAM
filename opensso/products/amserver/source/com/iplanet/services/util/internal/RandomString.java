/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RandomString.java,v 1.3 2008/06/25 05:41:42 qcheng Exp $
 *
 */

package com.iplanet.services.util.internal;

import com.sun.identity.shared.encode.Base64;
import java.security.SecureRandom;

/**
 * This class is used to get the random string value. It is used during
 * installation.
 */
public class RandomString {

    /**
     * This method prints the encrypted random string. If the random string
     * generation fails then it prints the exception stack trace.
     * 
     * @param args
     *            arguments for this method.
     * 
     */
    public static void main(String args[]) {
        String randomStr = null;
        try {
            byte[] bytes = new byte[24];
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            random.nextBytes(bytes);
            randomStr = Base64.encode(bytes).trim();
        } catch (Exception e) {
            randomStr = null;
            e.printStackTrace();
        }
        if (randomStr != null) {
            System.out.println(randomStr);
        }
    }
}
