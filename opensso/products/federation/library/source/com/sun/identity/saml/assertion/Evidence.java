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
 * $Id: Evidence.java,v 1.2 2008/06/25 05:47:32 qcheng Exp $
 *
 */

package com.sun.identity.saml.assertion;

import com.sun.identity.saml.common.SAMLException;
import org.w3c.dom.Element;
import java.util.Set;

/**
 *The <code>Evidence</code> element specifies an assertion either by 
 *reference or by value. An assertion is specified by reference to the value of 
 *the assertion's  <code>AssertionIDReference</code> element.
 *An assertion is specified by value by including the entire 
 *<code>Assertion</code> object 
 *@supported.all.api
 */

public class Evidence extends EvidenceBase {

    /**
     * Constructs an <code>Evidence</code> object from a block of existing XML
     * that has already been built into a DOM.
     *
     * @param assertionSpecifierElement A <code>org.w3c.dom.Element</code> 
     *        representing DOM tree for <code>Evidence</code> object.
     * @exception SAMLException if it could not process the Element properly, 
     *            implying that there is an error in the sender or in the
     *            element definition.
     */
    public Evidence(org.w3c.dom.Element assertionSpecifierElement) 
        throws SAMLException 
    {
        super(assertionSpecifierElement);
    }    
    
    /**
     * Constructs a new <code>Evidence></code> element containing a
     * set of <code>Assertion</code> objects.
     *
     * @param evidenceContent A set of <code>Assertion</code> and
     *        <code>AssertionIDReference</code> objects to be put within the
     *        <code>Evidence</code> element. The same Set contains both type
     *        of elements.
     * @exception SAMLException if the Set is empty or has invalid object.
     */
    public Evidence(Set evidenceContent ) throws SAMLException {
        super(evidenceContent);
    }
  
    /**
     * Constructs an Evidence from a Set of <code>Assertion</code> and
     * <code>AssertionIDReference</code> objects.
     * 
     * @param assertionIDRef Set of <code>AssertionIDReference</code> objects.
     * @param assertion Set of <code>Assertion</code> objects.
     * @exception SAMLException if either Set is empty or has invalid object.
     */
    public Evidence(Set assertionIDRef, Set assertion)  throws SAMLException {
         super(assertionIDRef, assertion);
    }

    protected  AssertionBase
        createAssertion(Element assertionElement) throws SAMLException {
        return new Assertion(assertionElement);
    }
      
    protected  AssertionIDReference
        createAssertionIDReference(String assertionID) throws SAMLException {
        return new AssertionIDReference(assertionID);
    }

}
