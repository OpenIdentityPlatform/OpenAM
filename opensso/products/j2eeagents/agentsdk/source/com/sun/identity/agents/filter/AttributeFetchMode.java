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
 * $Id: AttributeFetchMode.java,v 1.2 2008/06/25 05:51:44 qcheng Exp $
 *
 */

package com.sun.identity.agents.filter;

import com.sun.identity.agents.util.ConstrainedSelection;

/**
 * An attribute fetch mode of the agent filter
 */
public class AttributeFetchMode extends ConstrainedSelection {

    public static final int INT_MODE_NONE = 0;
    public static final int INT_MODE_HTTP_HEADER = 1;
    public static final int INT_MODE_REQUEST_ATTRIBUTE = 2;
    public static final int INT_MODE_COOKIE = 3;
    
    public static final String STR_MODE_NONE = "NONE";
    public static final String STR_MODE_HTTP_HEADER = "HTTP_HEADER";
    public static final String STR_MODE_REQUEST_ATTRIBUTE = "REQUEST_ATTRIBUTE";
    public static final String STR_MODE_COOKIE = "HTTP_COOKIE";
    
    public static final AttributeFetchMode MODE_NONE = 
        new AttributeFetchMode(STR_MODE_NONE, INT_MODE_NONE);

    public static final AttributeFetchMode MODE_HTTP_HEADER = 
        new AttributeFetchMode(STR_MODE_HTTP_HEADER, INT_MODE_HTTP_HEADER);
    
    public static final AttributeFetchMode MODE_REQUEST_ATTRIBUTE = 
        new AttributeFetchMode(STR_MODE_REQUEST_ATTRIBUTE, 
                INT_MODE_REQUEST_ATTRIBUTE);
    
    public static final AttributeFetchMode MODE_COOKIE = 
        new AttributeFetchMode(STR_MODE_COOKIE, INT_MODE_COOKIE);

    private static final AttributeFetchMode[] values = 
        new AttributeFetchMode[] {
            MODE_NONE, MODE_HTTP_HEADER, MODE_REQUEST_ATTRIBUTE, MODE_COOKIE
            };
    
    public static AttributeFetchMode get(int mode) {
        return (AttributeFetchMode) ConstrainedSelection.get(mode, values);
    }
    
    public static AttributeFetchMode get(String mode) {
        return (AttributeFetchMode) ConstrainedSelection.get(mode, values);
    }

    private AttributeFetchMode(String name, int value) {
        super(name, value);
    }
}
