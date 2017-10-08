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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.xacml.v3;

/**
 * Describes how a Privilege or Application or Resource Type read from XACML will be imported into OpenAM.
 *
 * @since 13.5.0
 */
public interface ImportStep {

    /**
     * Gets the status of the Import Step.
     *
     * @return the status.
     */
    DiffStatus getDiffStatus();

    /**
     * Name of the step. For example name of the resource type or application.
     *
     * @return
     */
    String getName();

    /**
     * Type of the step. For example Application or Resource Type.
     *
     * @return the type.
     */
    String getType();

}
