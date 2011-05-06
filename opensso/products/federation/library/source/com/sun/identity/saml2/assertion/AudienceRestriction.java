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
 * $Id: AudienceRestriction.java,v 1.2 2008/06/25 05:47:40 qcheng Exp $
 *
 */


package com.sun.identity.saml2.assertion;

import java.util.List;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * The <code>AudienceRestriction</code> specifies that the assertion
 * is addressed to one or more specific <code>Audience</code>s.
 * @supported.all.api
 */
public interface AudienceRestriction extends ConditionAbstract {

    /**
     * Returns a list of <code>String</code> represented audiences
     *
     * @return a list of <code>String</code> represented audiences 
     */
    public List getAudience();

    /**
     * Sets the audiences
     *
     * @param audiences List of audiences as URI strings
     * @exception SAML2Exception if the object is immutable
     */
    public void setAudience(List audiences) throws SAML2Exception;

   /**
    * Returns a String representation
    * @param includeNSPrefix Determines whether or not the namespace 
    *        qualifier is prepended to the Element when converted
    * @param declareNS Determines whether or not the namespace is declared
    *        within the Element.
    * @return A String representation
    * @exception SAML2Exception if something is wrong during conversion
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
     throws SAML2Exception;

   /**
    * Returns a String representation
    *
    * @return A String representation
    * @exception SAML2Exception if something is wrong during conversion
    */
    public String toXMLString() throws SAML2Exception;

   /**
    * Makes the object immutable
    */
    public void makeImmutable();

   /**
    * Returns true if the object is mutable
    *
    * @return true if the object is mutable
    */
    public boolean isMutable();

}
