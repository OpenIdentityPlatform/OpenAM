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
 * $Id: PolicyResourceComparator.java,v 1.2 2008/06/25 05:43:18 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.service.model;

import com.sun.identity.console.base.model.AMAdminUtils;
import java.util.Map;

/* - NEED NOT LOG - */

public class PolicyResourceComparator {
    private final String DELIMITER = "|";
    private final String TOKEN_NAME_SERVICE_TYPE = "serviceType";
    private final String TOKEN_NAME_CLASS = "class";
    private final String TOKEN_NAME_WILDCARD = "wildcard";
    private final String TOKEN_NAME_DELIMITER = "delimiter";
    private final String TOKEN_NAME_CASE_SENSITIVE = "caseSensitive";
    private final String TOKEN_NAME_ONE_LEVEL_WILDCARD = "oneLevelWildcard";
    private String serviceType;
    private String clazz;
    private String wildcard;
    private String delimiter;
    private boolean caseSensitive;
    private String oneLevelWildcard;

    public PolicyResourceComparator() {
    }

    public PolicyResourceComparator(String formattedString) {
        Map values = AMAdminUtils.getValuesFromDelimitedString(
            formattedString, DELIMITER);
        serviceType = (String)values.get(TOKEN_NAME_SERVICE_TYPE);
        clazz = (String)values.get(TOKEN_NAME_CLASS);
        wildcard = (String)values.get(TOKEN_NAME_WILDCARD);
        delimiter = (String)values.get(TOKEN_NAME_DELIMITER);
        oneLevelWildcard = (String)values.get(TOKEN_NAME_ONE_LEVEL_WILDCARD);

        String tmp = (String)values.get(TOKEN_NAME_CASE_SENSITIVE);
        caseSensitive = (tmp != null) && tmp.equalsIgnoreCase("true");
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append(TOKEN_NAME_SERVICE_TYPE)
            .append("=")
            .append(serviceType.trim());

        if (hasValue(clazz)) {
            buff.append(DELIMITER)
                .append(TOKEN_NAME_CLASS)
                .append("=")
                .append(clazz.trim());
        }

        if (hasValue(wildcard)) {
            buff.append(DELIMITER)
                .append(TOKEN_NAME_WILDCARD)
                .append("=")
                .append(wildcard.trim());
        }

        if (hasValue(delimiter)) {
            buff.append(DELIMITER)
                .append(TOKEN_NAME_DELIMITER)
                .append("=")
                .append(delimiter.trim());
        }

        if (hasValue(oneLevelWildcard)) {
            buff.append(DELIMITER)
                .append(TOKEN_NAME_ONE_LEVEL_WILDCARD)
                .append("=")
                .append(oneLevelWildcard.trim());
        }

        buff.append(DELIMITER)
            .append(TOKEN_NAME_CASE_SENSITIVE)
            .append("=");

        if (caseSensitive) {
            buff.append("true");
        } else {
            buff.append("false");
        }

        return buff.toString();
    }

    private boolean hasValue(String val) {
        return (val != null) && (val.trim().length() > 0);
    }

    public String getServiceType() {
        return (serviceType != null) ? serviceType : "";
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getClazz() {
        return (clazz != null) ? clazz : "";
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getWildcard() {
        return (wildcard != null) ? wildcard : "";
    }

    public void setWildcard(String wildcard) {
        this.wildcard = wildcard;
    }

    public String getDelimiter() {
        return (delimiter != null) ? delimiter : "";
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getOneLevelWildcard() {
        return (oneLevelWildcard != null) ? oneLevelWildcard : "";
    }

    public void setOneLevelWildcard(String oneLevelWildcard) {
        this.oneLevelWildcard = oneLevelWildcard;
    }

    public String getCaseSensitive() {
        return (caseSensitive) ? "true" : "false";
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(String caseSensitive) {
        this.caseSensitive = (caseSensitive != null) &&
            caseSensitive.equalsIgnoreCase("true");
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
}
