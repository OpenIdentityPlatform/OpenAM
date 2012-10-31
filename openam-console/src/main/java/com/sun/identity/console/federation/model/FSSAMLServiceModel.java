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
 * $Id: FSSAMLServiceModel.java,v 1.2 2008/06/25 05:49:40 qcheng Exp $
 *
 */

package com.sun.identity.console.federation.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* - NEED NOT LOG - */

public interface FSSAMLServiceModel
    extends AMModel {
    /**
     * Returns localized label for all attributes.
     *
     * @return localized label for all attributes.
     */
    public Map getAttributeLabels();
    
    /**
     * Returns localized inline help for all attributes.
     *
     * @return localized inline help for all attributes.
     */
    public Map getAttributeInlineHelps();
    
    /**
     * Returns attribute values.
     *
     * @return a map of samlv1.x attribute values.
     */
    public Map getAttributeValues();
    
    /**
     * Set attribute values.
     *
     * @param values Attribute values. Map of samlv1.x attribute name to set of values.
     * @throws AMConsoleException if values cannot be set.
     */
    public void setAttributeValues(Map values)
    throws AMConsoleException;
    
    /**
     * delete trusted partners set
     *
     * @param values a Set of trusted partner .
     * @throws AMConsoleException if name cannot be set.
     */
    public void deleteTrustPartners(Set values)
    throws AMConsoleException;
    
    /**
     * modify trusted partners set
     *
     * @param values a Set of trusted partner .
     * @throws AMConsoleException if name cannot be set.
     */
    public void modifyTrustPartners(Set values)
    throws AMConsoleException;
    
}
