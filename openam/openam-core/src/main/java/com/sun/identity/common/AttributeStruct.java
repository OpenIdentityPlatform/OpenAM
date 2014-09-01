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
 * $Id: AttributeStruct.java,v 1.3 2008/06/25 05:42:25 qcheng Exp $
 *
 */

package com.sun.identity.common;

import java.util.StringTokenizer;

/**
 * This class encapulates the information for mapping property
 * in configuration properties file to attribute in service.
 */
public class AttributeStruct {
     String serviceName;
    int revisionNumber;
    String attributeName;

    /**
     * Creates an instance of <code>AttributeStruct</code>.
     *
     * @param str Format string of attribute information. The format is <pre>
     *        &lt;servicername&gt;@&lt;revisionnumber&gt;@&lt;attributename&gt;
     *        </pre>.
     */
    public AttributeStruct(String str) {
        StringTokenizer st = new StringTokenizer(str, "@");
        if (st.countTokens() == 3) {
            while (st.hasMoreTokens()) {
                serviceName = st.nextToken().trim();
                revisionNumber = Integer.parseInt(st.nextToken());
                attributeName = st.nextToken().trim();
            }
        }
    }
    
    /**
     * Returns service name.
     *
     * @return service name.
     */
    public String getServiceName() {
        return serviceName;
    }
}
