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
 * Portions Copyrighted 2019 Open Source Solution Technology Corporation.
 * Portions Copyrighted 2019 OGIS-RI Co., Ltd.
 */

package com.sun.identity.console.task.model;

import javax.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import org.forgerock.openam.utils.StringUtils;

/**
 * Default implementation of the OAuth2 profiles model.
 */
public class OAuth2ModelImpl extends AMModelBase implements OAuth2Model {

    private static final String NAME_PREFIX = "configure.oauth2profile.name.";
    private static final String HELP_PREFIX = "configure.oauth2profile.help.";
    private static final Set<String> ACCEPTABLE_TYPE_NAMES = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("oauth2", "oidc", "mobileconnect", "uma")));

    private final String type;

    public OAuth2ModelImpl(final HttpServletRequest req, final Map map) {
        super(req, map);
        this.type = req.getParameter("type");
    }

    @Override
    public SortedSet<String> getRealms() throws AMConsoleException {
        final SortedSet<String> realms = new TreeSet<>(super.getRealmNames("/", "*"));
        realms.add("/");
        return realms;
    }

    @Override
    public String getDisplayName() {
        if (StringUtils.isEmpty(type)) {
            throw new IllegalStateException("type parameter is required");
        }
        if (ACCEPTABLE_TYPE_NAMES.contains(type)) {
            return getLocalizedString(NAME_PREFIX + type);
        }
        throw new IllegalStateException("type parameter is invalid");
    }

    @Override
    public String getLocalizedHelpMessage() {
        if (StringUtils.isEmpty(type)) {
            throw new IllegalStateException("type parameter is required");
        }
        if (ACCEPTABLE_TYPE_NAMES.contains(type)) {
            return getLocalizedString(HELP_PREFIX + type);
        }
        throw new IllegalStateException("type parameter is invalid");
    }
}
