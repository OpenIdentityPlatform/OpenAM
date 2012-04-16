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
 * $Id: AdvicesHandleableByAMResponse.java,v 1.4 2008/08/19 19:09:19 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.policy.remote;

import com.sun.identity.policy.PolicyUtils;
import java.util.Set;
import org.w3c.dom.Node;


/**
 * This <code>AdvicesHandleableByAMResponse</code> class represents a 
 * AdvicesHandleableByAMResponse XML element. 
 * The AdvicesHandleableByAMResponse DTD is 
 * defined as the following:
 * <p>
 * <pre>
 *    <!-- AdvicesHandleableByAMResponse element is the response from server
 *         that contains the names of policy advices that could be
 *         handled by OpenSSO
 *         if PEP redirects the user agent to OpenSSO
 *    -->
 *
 *    <!ELEMENT    AdvicesHandleableByAMResponse    (AttributeValuePair?) >
 * </pre>
 * <p>
 */

public class AdvicesHandleableByAMResponse {

    static final String ADVICES_HANDLEABLE_BY_AM_RESPONSE 
            = "AdvicesHandleableByAMResponse";
    static final String ADVICES_HANDLEABLE_BY_AM = "AdvicesHandleableByAM";
    Set advicesHandleableByAM = null;
    static final String LT = "<";
    static final String GT = ">";
    static final String NEW_LINE = "\n";
    static final String SLASH = "/";
    
    /** 
     * Default constructor for <code>AdvicesHandleableByAMResponse</code>.
     */
    public AdvicesHandleableByAMResponse() {
    }

    /**
     * Constructs an <code>AdvicesHandleableByAMResponse</code> object.
     *
     * @param advicesHandleableByAM Set of advices to be handled by OpenSSO
     *        Enterprise
     */
    public AdvicesHandleableByAMResponse(Set advicesHandleableByAM) {
        this.advicesHandleableByAM = advicesHandleableByAM;
    }

    /**
     * Sets the advices to be handled by OpenSSO.
     *
     * @param advicesHandleableByAM Set of advices to be handled by OpenSSO
     *        Enterprise.
     */
    public void setAdvicesHandleableByAM(Set advicesHandleableByAM) {
        this.advicesHandleableByAM = advicesHandleableByAM;
    }

    /**
     * Returns the advices to be handled by OpenSSO.
     *
     * @return advicesHandleableByAM Set of advices to be handled by OpenSSO
     *         Enterprise.
     */
    public Set getAdvicesHandleableByAM() {
        return advicesHandleableByAM;
    }

    /**
     * Returns <code>AdvicesHandleableByAMResponse</code> object from
     * XML string.
     *
     * @param node the XML DOM node for the
     *        <code>AdvicesHandleableByAMResponse</code> object.
     * @return constructed <code>AdvicesHandleableByAMResponse</code>.
     */
    public static AdvicesHandleableByAMResponse parseXML(Node node) {
        return new AdvicesHandleableByAMResponse(
            (Set)PolicyUtils.parseAttributeValuePairs(node).get(
                ADVICES_HANDLEABLE_BY_AM));
    }

    /**
     * Returns a XML representation of this object.
     *
     * @return a XML string representation of this object.
     */
    public String toXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append(LT).append( ADVICES_HANDLEABLE_BY_AM_RESPONSE)
                .append(">").append(NEW_LINE);
        sb.append(PolicyUtils.attributeValuePairToXMLString(
                ADVICES_HANDLEABLE_BY_AM, advicesHandleableByAM));
        sb.append(LT).append(SLASH).append( ADVICES_HANDLEABLE_BY_AM_RESPONSE)
                .append(GT).append(NEW_LINE);
        return sb.toString();
    }
}
