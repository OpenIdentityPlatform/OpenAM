/**
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.identity.openam.xacml.v3.resources;

import org.forgerock.identity.openam.xacml.v3.Entitlements.FunctionArgument;
import org.forgerock.identity.openam.xacml.v3.model.XACML3Constants;

/**
 * XACML PIP Resource Interface.
 * <p/>
 * Policy Information Point (PIP)
 *
 * The system entity that acts as a source of various Attribute Values.
 *
 * Depending upon the Implementation will provide where Policy Information is obtained.
 *
 * @author Jeff.Schenk@forgerock.com
 */
public interface XacmlPIPResource extends XACML3Constants {

    /**
     * Put a new instance of a FunctionArgument based upon Category and Attribute ID, which
     * have been parsed upstream.
     *
     * @param requestId
     * @param category
     * @param attributeId
     * @param dataType
     * @param value
     * @param includeInResult
     *
     * @return
     */
    public boolean put(String requestId, String category, String attributeId, String dataType, String value,
                       boolean includeInResult);

    /**
     * Remove an instance of a FunctionArgument based upon Category and Attribute ID.
     *
     * @param requestId
     * @param category
     * @param AttributeID
     * @return
     */
    public boolean remove(String requestId, String category, String AttributeID);

    /**
     * Resolve a Policy Resource Request Function Argument by using the Category and Attribute ID.
     *
     * @param requestId
     * @param category
     * @param AttributeID
     * @return
     */
    public FunctionArgument resolve(String requestId, String category, String AttributeID);

    /**
     * Clear out the Entire Map.
     */
    public void clear();

}
