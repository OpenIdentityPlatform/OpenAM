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
 * Copyright 2014-2015 ForgeRock AS.
 */

package com.sun.identity.console.sts.model;

/**
 * Model responses, particularly in the context of Rest STS instance creation, require a success flag, and a message.
 * The success flag indicates whether the message will be displayed as in a success, or error, dialog.
 */
public class STSInstanceModelResponse {
    private final boolean success;
    private final String message;

    private STSInstanceModelResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static STSInstanceModelResponse failure(String failureMessage) {
        return new STSInstanceModelResponse(false, failureMessage);
    }

    public static STSInstanceModelResponse success(String successMessage) {
        return new STSInstanceModelResponse(true, successMessage);
    }

    public static STSInstanceModelResponse success() {
        return new STSInstanceModelResponse(true, null);
    }

    public boolean isSuccessful() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
