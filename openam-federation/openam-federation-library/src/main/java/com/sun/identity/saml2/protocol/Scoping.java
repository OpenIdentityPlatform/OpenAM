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
 * $Id: Scoping.java,v 1.2 2008/06/25 05:47:58 qcheng Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.protocol.impl.ScopingImpl;
import java.util.List;


/**
 * This interface defines methods to retrieve Identity Providers and
 * context/limitations related to proxying of the request message.
 *
 * @supported.all.api
 */
@JsonTypeInfo(include = JsonTypeInfo.As.PROPERTY, use = JsonTypeInfo.Id.CLASS,
        defaultImpl = ScopingImpl.class)
public interface Scoping {

    /**
     * Returns the <code>IDPList</code> Object.
     *
     * @return the <code>IDPList</code> object.
     * @see #setIDPList(IDPList)
     */
    public IDPList getIDPList();

    /**
     * Sets the <code>IDPList</code> Object.
     *
     * @param idpList the new <code>IDPList</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getIDPList
     */
    public void setIDPList(IDPList idpList) throws SAML2Exception;

    /**
     * Returns a list of <code>RequesterID</code> Objects..
     *
     * @return list of <code>RequesterID</code> objects.
     * @see #setRequesterIDs(List)
     */
    public List<RequesterID> getRequesterIDs();

    /**
     * Sets a list of <code>RequesterID</code> Objects.
     *
     * @param requesterIDList the list of <code>RequesterID</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getRequesterIDs
     */
    public void setRequesterIDs(List<RequesterID> requesterIDList) throws SAML2Exception;

    /**
     * Returns the value of <code>ProxyCount</code> attribute.
     *
     * @return the value of <code>ProxyCount</code> attribute.
     * @see #setProxyCount(Integer)
     */
    public Integer getProxyCount();

    /**
     * Sets the value of <code>ProxyCount</code> attribute.
     *
     * @param proxyCount new value of <code>ProxyCount</code> attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getProxyCount
     */

    public void setProxyCount(Integer proxyCount) throws SAML2Exception;

    /**
     * Returns a String representation of this Object.
     *
     * @return a String representation of this Object.
     * @throws SAML2Exception if cannot create String object
     */
    public String toXMLString() throws SAML2Exception;

    /**
     * Returns a String representation of this Object.
     *
     * @param includeNSPrefix determines whether or not the namespace
     *	      qualifier is prepended to the Element when converted
     * @param declareNS determines whether or not the namespace is declared
     *	      within the Element.
     * @return the String representation of this Object.
     * @throws SAML2Exception if String object cannot be created.
     **/

    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
	throws SAML2Exception;


    /**
     * Makes this object immutable.
     */
    public void makeImmutable() ;

    /**
     * Returns true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable();
}
