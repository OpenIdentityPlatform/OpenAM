/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SubjectMapper.java,v 1.2 2008/06/25 05:48:16 qcheng Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.xacml.spi;
import com.sun.identity.xacml.context.Subject;
import com.sun.identity.xacml.common.XACMLException;
import java.util.List;
import java.util.Map;

    /**
     * This is an interface which provides an SPI to be able to map the <code>
     * Subject</code> in the XACML <code>Request</code> to an Object which
     * represents the "subject" in the Federation manager context. A plugin 
     * implementing this SPI needs to be defined  and configured at the PDP 
     * end for each trusted PEP ( as part of the metadata). A default 
     * mapper has been provided out-of-box which would map the XACML 
     * <code>Subject</code> to a <code>SSOToken</code>,
     * the <code>SSOToken</code>  being  the representation of the 
     * <code>Subject</code> in federation manager.
     */

public interface SubjectMapper {

    /**
     * Initializes the configuration data needed by this mapper.  It uses the
     * the entity IDs passed as parameters as index to the local metadata.
     * It can also consume a  generic <code>Map</code> of key-value pairs
     * to define its configuration in addition to the metadata.
     * @param pdpEntityId entity id of the PDP which is doing this subject 
     *        mapping and who has received the XACML request
     * @param pepEntityId entity id of the PEP ( requester) of the 
     *        policy decision.
     * @param properties <code>Map</code> of other properties which can be
     *        consumed by this mapper to do the subject mapping.
     * @exception XACMLException if the configration intialization 
     *        encounters an error condition.
     */

    public void initialize(String pdpEntityId, String pepEntityId, 
        Map properties) throws XACMLException;

    /**
     * This is the main API which does the mapping of XACML <code>Subject</code>
     * to native subject ( native being subject in the context of the federation
     * manager).
     * @param xacmlContextSubjects <code>xacml-context:Subject</code>s from the
     * <code>xacml-context:Request</code> object.
     * @return OpenAM <code>SSOToken</code> representing the mapped native subject.
     * If the mapping fails, <code>null<code> would be returned
     * @exception XACMLException if an error conditions occurs.
     */
    public Object mapToNativeSubject(List xacmlContextSubjects) 
            throws XACMLException;
            //TODO: pass List instead of Subject[]

}

