/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.publish.common;

import org.forgerock.openam.sts.AMSTSConstants;

/**
 * Encapsulates functionality common to the RestSTSInstancePublisherImpl and the SoapSTSInstancePublisherImpl
 */
public abstract class STSInstancePublisherBaseImpl {
    protected String normalizeDeploymentSubPath(String deploymentSubPath) {
        if (deploymentSubPath.endsWith(AMSTSConstants.FORWARD_SLASH)) {
            return deploymentSubPath.substring(0, deploymentSubPath.lastIndexOf(AMSTSConstants.FORWARD_SLASH));
        }

        if (deploymentSubPath.startsWith(AMSTSConstants.FORWARD_SLASH)) {
            return deploymentSubPath.substring(1, deploymentSubPath.length());
        }
        return deploymentSubPath;
    }
}
