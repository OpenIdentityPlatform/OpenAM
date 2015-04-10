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

/**
 * Contains the classes related to publishing STS instances. Logic includes querying sts-publish/soap on the OpenAM
 * 'home' server for published soap-sts instances, and adding/removing/updating the web-services corresponding to
 * these published soap-sts instances.
 */
package org.forgerock.openam.sts.soap.publish;