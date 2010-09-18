/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: IdRepoAttributeValidator.java,v 1.1 2009/11/10 01:48:01 hengming Exp $
 */
package com.sun.identity.idm.server;

import java.util.Map;
import java.util.Set;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepoException;

/**
 * 
 * This interface defines the methods which validate attributes.
 *
 */
public interface IdRepoAttributeValidator {

    /**
     * Initialization paramters as configred for a given plugin.
     * 
     * @param configParams configuration parameters
     */
    public void initialize(Map<String, Set<String>> configParams);

    /**
     * Validates attributes for create or modify operation. 
     *
     * @param attrMap attributes map to be validated.
     * @param idOp operaton which is ethier <code>IdOperation.CREATE</code> or
     *     <code>IdOperation.EDIT</code>
     * @throws IdRepoException If attributes can't be validated or there are
     *     repository related error conditions.
     */
    public void validateAttributes(Map<String, Set<String>> attrMap,
        IdOperation idOp) throws IdRepoException;

}
