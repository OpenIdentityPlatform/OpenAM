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

/**
 * Implementors of this interface will be notified of changes to the Audit service SMS configuration. Add the listener
 * by calling {@link AuditServiceConfigurationProvider#addConfigurationListener(AuditServiceConfigurationListener)}.
 *
 * @since 13.0.0
 */
public interface AuditServiceConfigurationListener {

    /**
     * Called when a global configuration change occurred.
     */
    void globalConfigurationChanged();

    /**
     * Called when the configuration for this realm was added or changed.
     *
     * @param realm The realm in which the change occurred.
     */
    void realmConfigurationChanged(String realm);

    /**
     * Called when the configuration for this realm was removed.
     *
     * @param realm The realm in which the change occurred.
     */
    void realmConfigurationRemoved(String realm);
}
