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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.authn.core;

/**
 * The possible stages of the Login process.
 *
 * Can only be in a stage where there are requirements(callbacks) to be submitted or all requirements have been
 * submitted and the login process has completed and the status of the login process will need to be check to
 * determine the actual outcome of the login process.
 */
public enum LoginStage {

    /** Login Stage when requirements are still to be submitted. */
    REQUIREMENTS_WAITING,
    /** Login Stage when all requirements have been submitted. */
    COMPLETE;
}
