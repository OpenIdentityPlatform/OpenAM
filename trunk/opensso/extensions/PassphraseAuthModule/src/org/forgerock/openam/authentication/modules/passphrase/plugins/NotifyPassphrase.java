/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: NotifyPassword.java,v 1.2 2008/06/25 05:43:41 qcheng Exp $
 *     "Portions Copyrighted [2012] [Forgerock AS]"
 */

package org.forgerock.openam.authentication.modules.passphrase.plugins;

import java.util.Locale;

import org.forgerock.openam.authentication.modules.passphrase.ui.model.PPResetException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.password.ui.model.PWResetException;

/**
 * <code>NotifyPassword</code> defines a set of methods that are required to
 * notify a user when a passphrase is changed.
 */
public interface NotifyPassphrase {
    /**
     * Notifies user when passphrase is changed.
     *
     * @param user User object.
     * @param passphrase new passphrase.
     * @param locale user locale.
     * @throws PWResetException if passphrase cannot be notified.
     */
    void notifyPassphrase(AMIdentity user, String passphrase, Locale locale) throws PPResetException;
}