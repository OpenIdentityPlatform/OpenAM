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
 * Copyright 2026 3A Systems LLC.
 */
package org.openidentityplatform.openam.config.servlet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link SetupPage} method as invokable via the {@code ?actionLink=<name>} request
 * parameter handled by {@link ConfiguratorServlet}. This is the replacement for Apache Click's
 * public {@code ActionLink} fields: only annotated methods are reachable from a request, so a
 * page cannot accidentally expose an arbitrary public method to the web.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ConfiguratorAction {

    /**
     * The action name matched against {@code ?actionLink=<name>}. Defaults to the method name.
     */
    String value() default "";
}
