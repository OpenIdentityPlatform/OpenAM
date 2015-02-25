/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock Incorporated. All Rights Reserved
 ~
 ~ The contents of this file are subject to the terms
 ~ of the Common Development and Distribution License
 ~ (the License). You may not use this file except in
 ~ compliance with the License.
 ~
 ~ You can obtain a copy of the License at
 ~ http://forgerock.org/license/CDDLv1.0.html
 ~ See the License for the specific language governing
 ~ permission and limitations under the License.
 ~
 ~ When distributing Covered Code, include this CDDL
 ~ Header Notice in each file and include the License file
 ~ at http://forgerock.org/license/CDDLv1.0.html
 ~ If applicable, add the following below the CDDL Header,
 ~ with the fields enclosed by brackets [] replaced by
 ~ your own identifying information:
 ~ "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.xacml.v3.model;

/**
 * Various Content Types which are dealt with using XACML 3 via HTTP/REST.
 * <p/>
 * Extending MediaTypes, @see javax.ws.rs.core.MediaType.
 *
 * @author jeff.schenk@forgerock.com
 */
public enum ContentType {

    JSON_HOME("application/json-home", CommonType.JSON),
    JSON("application/json", CommonType.JSON),
    XML( "application/xml" , CommonType.XML),
    XACML_PLUS_JSON("application/xacml+json", CommonType.JSON),
    XACML_PLUS_XML("application/xacml+xml", CommonType.XML),
    NONE(null, null);

    private final String applicationType;
    private final CommonType commonType;

    ContentType(String applicationType, CommonType commonType) {
        this.applicationType = applicationType;
        this.commonType = commonType;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public CommonType getCommonType() {
        return commonType;
    }

    /**
     * Normalize the Content Received from the actual HTTP Request.
     *
     * @param contentTypeStringValue
     * @return
     */
    public static ContentType getNormalizedContentType(final String contentTypeStringValue) {
        if ((contentTypeStringValue == null) || (contentTypeStringValue.isEmpty())) {
            return null;
        }
        String toCompare = contentTypeStringValue;
        if (toCompare.contains(";")) {
            toCompare = toCompare.substring(0, toCompare.indexOf(";"));
        }
        for (ContentType contentType : ContentType.values()) {
            if (contentType.equals(ContentType.NONE)) {
                continue;
            }
            if (contentType.getApplicationType().equalsIgnoreCase(toCompare)) {
                return contentType;
            }
        }
        return null;
    }
}
