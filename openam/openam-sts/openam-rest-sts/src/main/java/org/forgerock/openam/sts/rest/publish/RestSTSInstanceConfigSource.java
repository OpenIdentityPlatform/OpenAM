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

package org.forgerock.openam.sts.rest.publish;

import org.forgerock.openam.sts.rest.config.user.RestSTSInstanceConfig;

/**
 * Defines the interface whereby the Rest STS publish components obtain the RestSTSInstanceConfig instances corresponding
 * to a to-be-published Rest STS instance.
 *
 * This interface is currently unimplemented. It may serve to decouple STS-instance-publish clients (AdminUI, Crest service?)
 * from the publish functionality. This may, however, be overkill, and, at the moment, the Crest service which allows
 * for the publication of Rest STS instances consumes the RestSTSInstancePublisher interface directly.
 */
public interface RestSTSInstanceConfigSource {
    /**
     * A blocking call. Returns a RestSTSInstanceConfig instance if one has been published via ui/client elements.
     * @return the RestSTSInstanceConfig instance corresponding to a to-be-published Rest STS instance.
     */
    RestSTSInstanceConfig getSTSInstanceConfig();
}
