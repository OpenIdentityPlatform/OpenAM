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
package com.sun.identity.console.audit;

/**
 * Class for audit configuration console constants.
 *
 * @since 13.0.0
 */
public final class AuditConsoleConstants {

    /**
     * Name of the SMS Audit Service.
     */
    public static final String AUDIT_SERVICE = "AuditService";

    /**
     * Identifies the audit handler name stored in the page session attributes.
     */
    public static final String AUDIT_HANDLER_NAME = "auditHandlerName";

    /**
     * Identifies the audit handler type stored in the page session attributes.
     */
    public static final String AUDIT_HANDLER_TYPE = "auditHandlerType";

    /**
     * URL for the base global audit config view bean.
     */
    public static final String AUDIT_GLOBAL_CONFIG_VIEW_BEAN_URL = "../audit/GlobalAuditConfig";

    /**
     * Identifies the two button header UI component.
     */
    public static final String PAGE_TITLE_TWO_BUTTONS = "pgtitleTwoBtns";

    /**
     * Identifies the property attributes UI component.
     */
    public static final String PROPERTY_ATTRIBUTE = "propertyAttributes";

    /**
     * Key for the warning message translation.
     */
    public static final String WARNING_MESSAGE = "message.warning";

    /**
     * Key for the error message translation.
     */
    public static final String ERROR_MESSAGE = "message.error";

    /**
     * Key for the information message translation.
     */
    public static final String INFORMATION_MESSAGE = "message.information";

    private AuditConsoleConstants() {
        // Constants only class
    }
}
