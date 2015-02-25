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

package com.sun.identity.console.reststs.model;

/**
 * Model responses, particularly in the context of Rest STS instance creation, require a success flag, and a message.
 * The success flag indicates whether the message will be displayed as in a success, or error, dialog.
 */
public class RestSTSModelResponse {
    private final boolean success;
    private final String message;

    private RestSTSModelResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static RestSTSModelResponse failure(String failureMessage) {
        return new RestSTSModelResponse(false, failureMessage);
    }

    public static RestSTSModelResponse success(String successMessage) {
        return new RestSTSModelResponse(true, successMessage);
    }

    public static RestSTSModelResponse success() {
        return new RestSTSModelResponse(true, null);
    }

    public boolean isSuccessful() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
