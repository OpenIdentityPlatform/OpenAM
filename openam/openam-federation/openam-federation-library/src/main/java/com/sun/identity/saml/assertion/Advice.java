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
 * $Id: Advice.java,v 1.2 2008/06/25 05:47:31 qcheng Exp $
 *
 */


package com.sun.identity.saml.assertion;

import java.util.*; 
import org.w3c.dom.*; 
import com.sun.identity.saml.common.SAMLException;

/**
 * The <code>Advice</code> element contains additional information that the
 * issuer wish to provide. This information MAY be ignored by applications
 * without affecting either the semantics or validity. Advice elements MAY
 * be specified in  an extension schema. 
 *
 * @supported.all.api
 */
public class Advice extends AdviceBase {
    
    /**
     * Constructs an Advice element from an existing XML block.
     *
     * @param element representing a DOM tree element 
     * @exception SAMLException if there is an error in the sender or in the
     *            element definition.
     */
    public Advice(Element element) throws SAMLException  {
        super(element);
    }
    
    /**
     * Constructor
     *
     * @param assertionidreference A List of <code>AssertionIDReference</code>.
     * @param assertion A List of Assertion
     * @param otherelement A List of any element defined as 
     *        <code>any namespace="##other" processContents="lax"</code>; 
     */
    public Advice(List assertionidreference, List assertion, 
        List otherelement){ 
        super(assertionidreference, assertion, otherelement);
    }
     
    protected  AssertionBase createAssertion(Element assertionElement)
        throws SAMLException {
        return new Assertion(assertionElement);
    }
 
    protected  AssertionIDReference
        createAssertionIDReference(Element assertionIDRefElement)
            throws SAMLException {
        return  new AssertionIDReference(assertionIDRefElement);
    }

}
