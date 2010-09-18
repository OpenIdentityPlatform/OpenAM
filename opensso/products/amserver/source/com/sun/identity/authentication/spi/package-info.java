/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: package-info.java,v 1.5 2008/08/19 19:08:56 veiming Exp $
 *
 */

/**
 * Provides interfaces and classes for writing a supplemental authentication
 * module to plug into OpenSSO. Using the interfaces
 * and classes provided, a custom authentication module may be added to Sun
 * OpenSSO's list of supported authentication modules/types.
 * <p>
 * Provides an interface for post authentication processing on successful,
 * failed authentication or on a logout.
 * <p>
 * Provides an interface for UserID Generation for Membership/Self Registration
 * auth module.
 * <p>
 * Provides an interface to receive notifications of a user status change after
 * successful password reset or after account lockout (memory).
 * @supported.api
 */

package com.sun.identity.authentication.spi;
