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
 * $Id: SubjectQuery.java,v 1.2 2008/06/25 05:47:37 qcheng Exp $
 *
 */


package com.sun.identity.saml.protocol;

import com.sun.identity.saml.assertion.Subject;
/**
 * This abstract class extends from another abstract class Query. It is an
 * extension point that allows new SAML queries that specify a single SAML
 * subject. It corresponds to
 * <code>&lt;samlp:SubjectQueryAbstractType&gt;</code> in SAML protocol schema.
 *
 * @supported.all.api
 */
public abstract class SubjectQuery extends Query {
   
    protected Subject subject = null; 
   
    /**
     * Default constructor
     */
    protected SubjectQuery() {
    }
   
    /**
     * Gets the Subject associated with this Query.
     * @return the Subject associated with the query.
     */
    public Subject getSubject() {
	return subject;
    }
}
