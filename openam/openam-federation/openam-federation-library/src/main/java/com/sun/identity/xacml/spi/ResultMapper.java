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
 * $Id: ResultMapper.java,v 1.2 2008/06/25 05:48:16 qcheng Exp $
 *
 */

package com.sun.identity.xacml.spi;
import com.sun.identity.xacml.context.Result;
import com.sun.identity.xacml.common.XACMLException;
import java.util.Map;

/**
 * This interface defines the SPI for pluggable implementations 
 * to map native resource result to XACML context result
 */
public interface ResultMapper {

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
            Map properties) throws XACMLException;

    /**
     * Returns XACML  context Result
     * @param nativeResult native resource result
     * @param nativeServiceName native service name
     * @param resourceMapper resource mapper to map to XACML Resource
     * @param actionMapper  action mapper to map to XACML Action
     * @return XACML  context Result
     * @exception XACMLException if can not map to XACML  context Result
     */
    public Result mapToXACMLResult(Object nativeResult, 
            String nativeServiceName,
            ResourceMapper resourceMapper, ActionMapper actionMapper) 
            throws XACMLException;

}

