/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 ~
 ~ The contents of this file are subject to the terms
 ~ of the Common Development and Distribution License
 ~ (the License). You may not use this file except in
 ~ compliance with the License.
 ~
 ~ You can obtain a copy of the License at
 ~ http://forgerock.org/license/CDDLv1.0.html
 ~ See the License for the specific language governing
 ~ permission and limitations under the License.
 ~
 ~ When distributing Covered Code, include this CDDL
 ~ Header Notice in each file and include the License file
 ~ at http://forgerock.org/license/CDDLv1.0.html
 ~ If applicable, add the following below the CDDL Header,
 ~ with the fields enclosed by brackets [] replaced by
 ~ your own identifying information:
 ~ "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.identity.openam.xacml.commons;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Referencing:
 * RFC2141. @see http://www.ietf.org/rfc/rfc2141.txt
 * RFC2169, HTTP in URN Resolution @see http://tools.ietf.org/html/rfc2169
 *
 * @author jeff.schenk@forgerock.com
 *
 */
public class URN implements Serializable {

    /**
     * Pattern for Validating a URN.
     */
    public final static Pattern URN_PATTERN = Pattern.compile(
            "^urn:[a-z0-9][a-z0-9-]{0,31}:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Original URN
     */
    private String originalURN;

    /**
     * Constructor with single parameter
     * @param urn
     */
    public URN(String urn) {
        originalURN = urn;
    }

    /**
     * Determines if URN is valid or not.
     *
     * @return
     */
    public boolean isValid() {
        return URN.URN_PATTERN.matcher(originalURN).matches();
    }

    /**
     * Get the original URN.
     * @return
     */
    public String getURN() {
        return originalURN;
    }

}
