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
 * $Id: FSLogoutStatus.java,v 1.2 2008/06/25 05:47:00 qcheng Exp $
 *
 */


package com.sun.identity.federation.services.logout;

/**
 * Logout status.
 */
public class FSLogoutStatus {
    private String status;
    
    /**
     * Creates new <code>FSLogoutStatus</code> object.
     * @param status logout status.
     */
    public FSLogoutStatus(String status) {
        this.status = status;
    }

    /**
     * Gets logout status.
     * @return logout status.
     * @see #setStatus(String)
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Sets logout status.
     * @param status logout status
     * @see #getStatus()
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
