/*
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package com.sun.identity.tools.javadocs;

import com.sun.javadoc.Tag;
import com.sun.tools.doclets.Taglet;
import java.util.Map;

/**
 * A no-op taglet for {@literal @}supported.all.api in order to prevent JavaDoc warnings about unprocessed taglets.
 *
 * @author Peter Major
 */
public class SupportedAllAPITaglet implements Taglet {

    public static final String NAME = "supported.all.api";

    public static void register(Map<String, Taglet> tagletMap) {
        tagletMap.put(NAME, new SupportedAllAPITaglet());
    }

    public boolean inField() {
        return true;
    }

    public boolean inConstructor() {
        return true;
    }

    public boolean inMethod() {
        return true;
    }

    public boolean inOverview() {
        return false;
    }

    public boolean inPackage() {
        return true;
    }

    public boolean inType() {
        return true;
    }

    public boolean isInlineTag() {
        return false;
    }

    public String getName() {
        return NAME;
    }

    public String toString(Tag tag) {
        return "";
    }

    public String toString(Tag[] tags) {
        return "";
    }
}
