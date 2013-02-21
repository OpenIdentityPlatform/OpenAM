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

import com.sun.identity.shared.debug.Debug;
import org.forgerock.identity.openam.xacml.v3.model.XACML3Constants;

/**
 * XACML PEP Resource
 * <p/>
 * Policy enforcement point (PEP)
 * The system entity that performs access control, by making decision requests and enforcing authorization decisions.
 * This term is defined in a joint effort by the IETF Policy Framework Working Group
 * and the Distributed Management Task Force (DMTF)/Common Information Model (CIM) in [RFC3198].
 * This term corresponds to "Access Enforcement Function" (AEF) in [ISO10181-3].
 *
 * @author Jeff.Schenk@forgerock.com
 */
public class XacmlPEPResource implements XACML3Constants {
    /**
     * Define our Static resource Bundle for our debugger.
     */
    private static Debug debug = Debug.getInstance("amXACML");


}
