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

package org.forgerock.openam.console.ui.taglib;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.jato.taglib.html.FormTag;
import com.iplanet.jato.util.NonSyncStringBuffer;
import com.sun.identity.shared.Constants;


public class AutoFormTag extends FormTag {

    private boolean autoCompleteEnabled = true;

    public AutoFormTag() {
        super();
        autoCompleteEnabled = SystemProperties.getAsBoolean(Constants.AUTOCOMPLETE_ENABLED, true);
    }

    @Override
    public void appendStyleAttributes(NonSyncStringBuffer buffer) {

        if (!autoCompleteEnabled) {
            buffer.append(" autocomplete=\"off\" ");
        }

        super.appendStyleAttributes(buffer);
    }
}
