/**
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
 * $Id: FMEnvironmentMapper.java,v 1.2 2008/06/25 05:50:16 qcheng Exp $
 *
 */

package com.sun.identity.xacml.plugins;

import com.sun.identity.xacml.context.Environment;
import com.sun.identity.xacml.context.Subject;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.spi.EnvironmentMapper;

import java.util.List;
import java.util.Map;

/**
 * This class implements EnvironmentMapper to map between XACML context 
 * Environment and FM native environment.
 * This mapper does not recognise any attributes of
 * <code>xacml-context:Envrionement</code> at present.
 * So, at present it is essentially a no-op.
 *
 */
public class FMEnvironmentMapper implements EnvironmentMapper {

    /**
     * Initializes the mapper implementation. This would be called immediately 
     * after constructing an instance of the implementation.
     *
     * @param pdpEntityId EntityID of PDP
     * @param pepEntityId EntityID of PEP
     * @param properties configuration properties
     * @exception XACMLException if can not initialize
     */
    public void initialize(String pdpEntityId, String pepEntityId, 
            Map properties) throws XACMLException {
    }

    /**
     * Returns native environment
     * @param xacmlContextEnvironment XACML  context Environment
     * @param xacmlContextSubjects XACML context Subject(s)
     * @return native environment map
     * @exception XACMLException if can not map to native environment
     */
    public Map mapToNativeEnvironment(Environment xacmlContextEnvironment, 
            List xacmlContextSubjects) //List xacmlContextSubject) 
            throws XACMLException {
        return null;
    }

}

