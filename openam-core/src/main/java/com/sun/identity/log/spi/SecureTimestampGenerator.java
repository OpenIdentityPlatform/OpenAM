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
 * $Id: SecureTimestampGenerator.java,v 1.4 2008/06/25 05:43:40 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.log.spi;

import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Generates Secure time stamp either using hardware or a secure time stamp
 * server.
 */
public class SecureTimestampGenerator implements ITimestampGenerator {
    
    private static SimpleDateFormat sdf = 
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /** 
     * Creates new <code>SecureTimestampGenerator</code>.
     */
    public SecureTimestampGenerator() {
    }
    
    /**
     * Returns generated time stamp.
     *
     * @return generated time stamp.
     */
    public String getTimestamp() {
        Date date = new Date();
        return sdf.format(date);
    }
}
