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
 * $Id: SkipIfInfo.java,v 1.2 2008/06/25 05:51:24 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;

public class SkipIfInfo {

    public String toString() {
        return "SkipInfo(" + getKey() + ", " + getValues() + ")";
    }

    public SkipIfInfo(String key, ArrayList values, boolean ignoreCase) {
        setKey(key);
        setValues(values);
        setIgnoreCase(ignoreCase);
    }

    public String getKey() {
        return key;
    }

    public ArrayList getValues() {
        return values;
    }

    public boolean getIgnoreCase() {
        return ignoreCase;
    }

    private void setKey(String key) {
        this.key = key;
    }

    private void setValues(ArrayList values) {
        this.values = values;
    }

    private void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    private String key;

    private ArrayList values;

    private boolean ignoreCase;
}
