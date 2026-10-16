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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.authentication.modules.windowsdesktopsso;

import org.ietf.jgss.GSSException;

import javax.security.auth.Subject;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service credentials that expire once: the first service subject fails as it would with a GSS provider that reports
 * expired credentials. The JDK Kerberos provider never does.
 * <p>
 * Not a nested class of the PowerMock test: JDK calls made from there go through reflection into packages
 * the test JVM does not open.
 */
final class ExpiringServiceCredentials {
    private final boolean wrapped;
    private final List<Subject> used = new ArrayList<>();

    ExpiringServiceCredentials(boolean wrapped) {
        this.wrapped = wrapped;
    }

    /** Throws for the first subject; the caller must not accept the token then, a second accept is a replay. */
    void check(Subject subject) throws Exception {
        used.add(subject);
        if (used.get(0) == subject) {
            GSSException expired = new GSSException(GSSException.CREDENTIALS_EXPIRED);
            throw wrapped ? new PrivilegedActionException(expired) : expired; // as Subject.doAs wraps it, or bare
        }
    }

    Subject lastUsed() {
        return used.get(used.size() - 1);
    }
}
