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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.audit.configuration;

import com.sun.identity.sm.DefaultValues;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains the default values for audit topics.
 *
 * @since 13.0.0
 */
public class AuditTopicDefaultValues extends DefaultValues {

    private static final Set<String> AUDIT_TOPICS = new HashSet<>();

    static {
        AUDIT_TOPICS.add("access");
        AUDIT_TOPICS.add("activity");
        AUDIT_TOPICS.add("authentication");
        AUDIT_TOPICS.add("config");
    };

    @Override
    public Set<String> getDefaultValues() {
        return AUDIT_TOPICS;
    }
}
