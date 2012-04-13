/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: EncryptionUtil.java,v 1.1 2008/07/18 06:48:55 mallas Exp $
 *
 */

package com.sun.identity.wssagents.common.util;

import com.iplanet.services.util.Crypt;

public class EncryptionUtil {

    public static void main(String[] args) {
         if(args.length != 1) {
            System.out.println("Invalid number of arguments");
            return;
         }
         System.out.println(Crypt.encrypt(args[0]));
    }
    
    
}
