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
 * $Id: ResultStatus.java,v 1.2 2008/06/25 05:51:30 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.util;

public class ResultStatus {

    public static ResultStatus get(int value, ResultStatus[] list) {
        ResultStatus result = null;
        for (int i = 0; i < list.length; i++) {
            if (value == list[i].getIntValue()) {
                result = list[i];
                break;
            }
        }
        return result;
    }

    public static ResultStatus get(String name, ResultStatus[] list) {
        ResultStatus result = null;
        if (name != null) {
            for (int i = 0; i < list.length; i++) {
                if (name.equals(list[i].getName())) {
                    result = list[i];
                    break;
                }
            }
        }
        return result;
    }

    public boolean equals(Object object) {
        boolean result = false;

        if (object != null && (getClass().equals(object.getClass()))) {
            if (getIntValue() == ((ResultStatus) object).getIntValue()) {
                result = true;
            }
        }
        return result;
    }

    public String toString() {
        return getName();
    }

    public String getName() {
        return name;
    }

    public int getIntValue() {
        return intValue;
    }

    protected ResultStatus(String name, int intValue) {
        setName(name);
        setIntValue(intValue);
    }

    private void setName(String name) {
        this.name = name;
    }

    private void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    private String name;

    private int intValue;
}
