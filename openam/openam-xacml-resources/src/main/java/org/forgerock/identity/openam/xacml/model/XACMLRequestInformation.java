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
package org.forgerock.identity.openam.xacml.model;

import org.forgerock.identity.openam.xacml.commons.ContentType;

/**
 * XACMLRequestInformation
 *
 * Simple POJO to hold all relevant information related to a
 * XACML Request.
 *
 * @author jeff.schenk@forgerock.com
 */
public class XACMLRequestInformation {

    /**
     * Requested URI
     */
    private String requestURI;
    /**
     * Meta Alias Information.
     */
    private String metaAlias;
    /**
     * Our PDP Entity ID.
     */
    private String pdpEntityID;
    /**
     * Realm.
     */
    private String realm;
    /**
     *
     */
    private boolean requestNodePresent;
    /**
     * Content Type
     */
    private ContentType contentType;
    /**
     * Original Request Content.
     */
    private String originalContent;
    /**
     * Content, can be either XML or JSON Object depending upon the specified ContentType.
     */
    private Object content;
    /**
     * Optional HTTP Digest Authorization Request
     */
    private String authenticationHeader;
    /**
     *  Indicates if this Request has been authenticated or not.
     */
    private boolean authenticated;
    /**
     * Content, can be either XML or JSON depending upon the specified ContentType.
     * If this object is Null, we have an Anonymous/Guest Request.
     */
    private Object authenticationContent;


    /**
     * Default Constructor.
     *
     * @param requestURI
     * @param metaAlias
     * @param pdpEntityID
     * @param realm
     */
    public XACMLRequestInformation(ContentType contentType, String requestURI, String metaAlias, String pdpEntityID,
                                   String realm) {
              this.contentType = contentType;
              this.requestURI = requestURI;
              this.metaAlias = metaAlias;
              this.pdpEntityID = pdpEntityID;
              this.realm = realm;
    }



    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public void setRequestURI(String requestURI) {
        this.requestURI = requestURI;
    }

    public String getMetaAlias() {
        return metaAlias;
    }

    public void setMetaAlias(String metaAlias) {
        this.metaAlias = metaAlias;
    }

    public String getPdpEntityID() {
        return pdpEntityID;
    }

    public void setPdpEntityID(String pdpEntityID) {
        this.pdpEntityID = pdpEntityID;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public String getAuthenticationHeader() {
        return authenticationHeader;
    }

    public void setAuthenticationHeader(String authenticationHeader) {
        this.authenticationHeader = authenticationHeader;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public Object getAuthenticationContent() {
        return authenticationContent;
    }

    public void setAuthenticationContent(Object authenticationContent) {
        this.authenticationContent = authenticationContent;
    }

    public boolean isRequestNodePresent() {
        return requestNodePresent;
    }

    public void setRequestNodePresent(boolean requestNodePresent) {
        this.requestNodePresent = requestNodePresent;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("XACMLRequestInformation");
        sb.append("{requestURI='").append(requestURI).append('\'');
        sb.append(", metaAlias='").append(metaAlias).append('\'');
        sb.append(", pdpEntityID='").append(pdpEntityID).append('\'');
        sb.append(", realm='").append(realm).append('\'');
        sb.append(", requestNodePresent=").append(requestNodePresent);
        sb.append(", contentType=").append(contentType);
        sb.append(", originalContent='").append(originalContent).append('\'');
        sb.append(", content=").append(content);
        sb.append(", authenticationHeader='").append(authenticationHeader).append('\'');
        sb.append(", authenticated=").append(authenticated);
        sb.append(", authenticationContent=").append(authenticationContent);
        sb.append('}');
        return sb.toString();
    }
}
