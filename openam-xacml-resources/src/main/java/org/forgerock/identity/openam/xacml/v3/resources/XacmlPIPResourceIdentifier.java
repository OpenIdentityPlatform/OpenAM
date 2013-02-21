/**
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.identity.openam.xacml.v3.resources;

import java.io.Serializable;

/**
 * XACML PIP Resource Identifier.
 * <p/>
 * Policy Information Point (PIP)
 *
 * Provides a Complex Object to wrap the Category, Attribute ID and DataType for
 * resolution of Request
 *
 * @author Jeff.Schenk@forgerock.com
 */
public class XacmlPIPResourceIdentifier implements Serializable {
    /**
     * Request Identifier to keep things consistent among Requests.
     */
    private String requestId;
    /**
     * XACML Category URN
     * Example:
     *     "oasis:names:tc:xacml:1.0:subject-category:access-subject"
     */
    private String category;
    /**
     * XACML Attribute Id URN.
     * Example:
     *     "urn:oasis:names:tc:xacml:3.0:ipc:subject:organization
     */
    private String attributeId;
    /**
     * XACML Value, to indicate if this value should be contained in the XACML Result.
     */
    private boolean includeInResult;

    /**
     * Default Constructor.
     */
    public XacmlPIPResourceIdentifier() {
    }

    /**
     * Constructor with all required fields to instantiate this Element Entry.
     * @param requestId
     * @param category
     * @param attributeId
     * @param includeInResult
     */
    public XacmlPIPResourceIdentifier(String requestId, String category, String attributeId, boolean includeInResult) {
        this.requestId = requestId;
        this.category = category;
        this.attributeId = attributeId;
        this.includeInResult = includeInResult;
    }

    /**
     * Constructor with minimal required fields to instantiate this Object Type.
     * @param requestId
     * @param category
     * @param attributeId
     */
    public XacmlPIPResourceIdentifier(String requestId, String category, String attributeId) {
        this.requestId = requestId;
        this.category = category;
        this.attributeId = attributeId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(String attributeId) {
        this.attributeId = attributeId;
    }

    public boolean isIncludeInResult() {
        return includeInResult;
    }

    public void setIncludeInResult(boolean includeInResult) {
        this.includeInResult = includeInResult;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("XacmlPIPResourceIdentifier");
        sb.append("{requestId='").append(requestId).append('\'');
        sb.append(", category='").append(category).append('\'');
        sb.append(", attributeId='").append(attributeId).append('\'');
        sb.append(", includeInResult=").append(includeInResult);
        sb.append('}');
        return sb.toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XacmlPIPResourceIdentifier that = (XacmlPIPResourceIdentifier) o;

        if (attributeId != null ? !attributeId.equals(that.attributeId) : that.attributeId != null) return false;
        if (category != null ? !category.equals(that.category) : that.category != null) return false;
        if (requestId != null ? !requestId.equals(that.requestId) : that.requestId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = requestId != null ? requestId.hashCode() : 0;
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (attributeId != null ? attributeId.hashCode() : 0);
        return result;
    }
}
