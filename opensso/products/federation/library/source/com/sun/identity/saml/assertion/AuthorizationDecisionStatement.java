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
 * $Id: AuthorizationDecisionStatement.java,v 1.2 2008/06/25 05:47:32 qcheng Exp $
 *
 */


package com.sun.identity.saml.assertion;

import java.text.*; 
import java.util.*;
import org.w3c.dom.*;
import com.sun.identity.saml.common.SAMLException;

/**
 *The <code>AuthorizationDecisionStatement</code> element supplies a statement 
 *by the issuer that the request for access by the specified subject to the 
 *specified resource has resulted in the specified decision on the basis of 
 *some optionally specified evidence. 
 *
 *@supported.all.api
 */
public class AuthorizationDecisionStatement 
    extends AuthorizationDecisionStatementBase {

    /**
     *Default constructor 
     */
    protected AuthorizationDecisionStatement() {
        super();
    }
    
    /**
     * Constructs an <code>AuthorizationStatement</code> element from an 
     * existing XML block.
     *
     * @param element representing a DOM tree element 
     * @exception SAMLException if there is an error in the sender or in
     *            the element definition.
     */
    public AuthorizationDecisionStatement(Element element) 
                                          throws SAMLException {
        super(element);
    }
      
    /**
     * Constructs an instance of <code>AuthorizationDecisionStatement</code>.
     *
     * @param subject (required) A Subject object
     * @param resource (required) A String identifying the resource to which 
     *        access authorization is sought.
     * @param decision (required) The decision rendered by the issuer with
     *        respect to the specified resource. The value is of the 
     *        <code>DecisionType</code> simple type. 
     * @param action (required) A List of Action objects specifying the set of 
     *        actions authorized to be performed on the specified resource.
     * @param evidence (optional) An Evidence object representing a set of 
     *        assertions that the issuer replied on in making decisions.  
     * @exception SAMLException if there is an error in the sender.
     */     
    public AuthorizationDecisionStatement(Subject subject, String resource, 
        int decision, List action, EvidenceBase evidence)
        throws SAMLException {
        super(subject, resource, decision, action, evidence);
    }
    
    /**
     * Constructs a <code>AuthorizationDecisionStatement</code> instance.
     *
     *@param subject (required) A Subject object
     *@param resource (required) A String identifying the resource to which 
     *       access authorization is sought. 
     *@param decision (required) The decision rendered by the issuer with
     *       respect to the specified resource. The value is of the 
     *       <code>DecisionType</code> simple type. 
     *@param action (required) A List of Action objects specifying the set of
     *       actions authorized to be performed on the  
     *       specified resource.
     *@exception SAMLException if there is an error in the sender.
     */   
    public AuthorizationDecisionStatement(Subject subject, String resource, 
        int decision, List action) throws SAMLException {
        super(subject, resource, decision, action);
    }            
                  
    protected EvidenceBase createEvidence(Element evidenceElement)
        throws SAMLException {
        return new Evidence(evidenceElement);
    }
  
    protected  Subject createSubject(Element subjectElement)
        throws SAMLException {
        return new Subject(subjectElement);
    }
  
    protected  Action createAction(Element actionElement) throws SAMLException {
        return new Action(actionElement);
    }
  
   /**
    * Returns the evidence from <code>AuthorizationStatement</code>.
    *
    * @return An Evidence object that the issuer replied on in making decisions.
    */
    public Evidence getEvidence() {
        return (Evidence)_evidence;
    }

}

