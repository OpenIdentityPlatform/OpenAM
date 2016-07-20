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

package org.forgerock.openam.sm.validation;

import java.util.Set;

import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.annotations.VisibleForTesting;

import com.sun.identity.configuration.SystemProperties;
import com.sun.identity.shared.configuration.ISystemProperties;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceAttributeValidator;

/**
 * Validates stateless session signature algorithm choice is consistent with
 * {@code org.forgerock.openam.session.stateless.signing.allownone} system property.
 */
public final class StatelessSessionSigningAlgorithmValidator implements ServiceAttributeValidator {
    private static final Debug DEBUG = Debug.getInstance("amServerProperties");
    private static final String ALLOW_NONE = "org.forgerock.openam.session.stateless.signing.allownone";

    private final ISystemProperties systemProperties;

    @VisibleForTesting
    StatelessSessionSigningAlgorithmValidator(final ISystemProperties systemProperties) {
        this.systemProperties = systemProperties;
    }

    /**
     * Default constructor.
     */
    public StatelessSessionSigningAlgorithmValidator() {
        this(new SystemProperties());
    }

    @Override
    public boolean validate(final Set<String> values) {
        final boolean allowNone = Boolean.parseBoolean(systemProperties.getOrDefault(ALLOW_NONE, "false"));
        final String value = CollectionUtils.getFirstItem(values, "NONE");

        if (!allowNone && "NONE".equalsIgnoreCase(value)) {
            DEBUG.error("Disallowing setting stateless session signing algorithm to NONE: set " +
                    "org.forgerock.openam.session.stateless.signing.allownone to 'true' if you are sure.");
            return false;
        }
        return true;
    }
}
