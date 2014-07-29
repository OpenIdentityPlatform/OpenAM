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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.scripting.sandbox;

import org.forgerock.util.Reject;
import org.mozilla.javascript.ClassShutter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Rhino class-shutter implementation that provides sandboxing via class white- and black-listing. Each class that is
 * loaded or accessed by a script must match at least one pattern in the white-list and no patterns in the black-list
 * to be allowed, otherwise it is forbidden. If a security manager is specified then each class must additionally be
 * approved by a call to {@link java.lang.SecurityManager#checkPackageAccess(String)}.
 */
public final class RhinoSandboxClassShutter implements ClassShutter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RhinoSandboxClassShutter.class);

    private final SecurityManager securityManager;
    private final List<Pattern> whiteList;
    private final List<Pattern> blackList;

    /**
     * Constructs the sandboxed class-shutter with the given configuration options.
     *
     * @param securityManager the security manager to use for package access checks. May be null to disable.
     * @param whiteList the class-name white-list. May not be null.
     * @param blackList the class-name black-list. May not be null.
     */
    public RhinoSandboxClassShutter(final SecurityManager securityManager, final List<Pattern> whiteList,
                                    final List<Pattern> blackList) {
        Reject.ifNull(whiteList, blackList);
        this.securityManager = securityManager;
        this.whiteList = new ArrayList<Pattern>(whiteList);
        this.blackList = new ArrayList<Pattern>(blackList);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation first checks whether the class/package is allowed by the configured security manager
     * (if one is set). Then it checks that the class name matches at least one white-list pattern. Finally, it
     * checks that the class name does not match any of the black-list patterns. Only if all three checks pass is the
     * class made visible to the script, otherwise it is denied.
     */
    @Override
    public boolean visibleToScripts(final String fullClassName) {
        LOGGER.debug("Checking access to class '%s'", fullClassName);

        if (securityManager != null) {
            try {
                securityManager.checkPackageAccess(fullClassName);
            } catch (SecurityException ex) {
                LOGGER.error("Access denied by SecurityManager for class '%s'", fullClassName);
                return false;
            }
        }

        boolean allowed = false;
        for (final Pattern pattern : whiteList) {
            if (pattern.matcher(fullClassName).matches()) {
                allowed = true;
                break;
            }
        }

        if (!allowed) {
            LOGGER.warn("Classname failed to match whitelist: '%s'", fullClassName);
            return false;
        }

        for (final Pattern pattern : blackList) {
            if (pattern.matcher(fullClassName).matches()) {
                LOGGER.error("Access to class '%s' denied by blacklist pattern: %s", fullClassName, pattern.pattern());
                return false;
            }
        }

        LOGGER.debug("Access allowed for class '%s'", fullClassName);
        return true;
    }
}
