/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock Incorporated. All Rights Reserved
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
package org.forgerock.identity.openam.xacml.v3.commons;

/**
 * Normalized Content Types which are dealt with using XACML 3 via HTTP/REST.
 * <p/>
 *
 * @author jeff.schenk@forgerock.com
 * @see org.forgerock.identity.openam.xacml.v3.commons.ContentType
 */
public enum CommonType {

    JSON(0), XML(1);

    private final int commonTypeId;

    CommonType(int commonTypeId) {
        this.commonTypeId = commonTypeId;
    }

    public int id() {
        return commonTypeId;
    }

}
