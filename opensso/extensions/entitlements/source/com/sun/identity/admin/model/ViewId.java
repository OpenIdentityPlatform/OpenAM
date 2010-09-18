/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ViewId.java,v 1.8 2009/08/03 22:25:31 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.util.HashMap;
import java.util.Map;

public enum ViewId {

    HOME("/admin/facelet/home.xhtml"),
    POLICY("/admin/facelet/policy.xhtml"),
    POLICY_CREATE("/admin/facelet/policy-create.xhtml"),
    POLICY_MANAGE("/admin/facelet/policy-manage.xhtml"),
    POLICY_EDIT("/admin/facelet/policy-edit.xhtml"),
    REFERRAL_CREATE("/admin/facelet/referral-create.xhtml"),
    REFERRAL_MANAGE("/admin/facelet/referral-manage.xhtml"),
    REFERRAL_EDIT("/admin/facelet/referral-edit.xhtml"),
    FEDERATION("/admin/facelet/federation.xhtml"),
    SAMLV2_HOSTED_SP_CREATE("/admin/facelet/samlv2-hosted-sp-create.xhtml"),
    SAMLV2_REMOTE_SP_CREATE("/admin/facelet/samlv2-remote-sp-create.xhtml"),
    SAMLV2_HOSTED_IDP_CREATE("/admin/facelet/samlv2-hosted-idp-create.xhtml"),
    SAMLV2_REMOTE_IDP_CREATE("/admin/facelet/samlv2-remote-idp-create.xhtml"),
    NEWS("/admin/facelet/news.xhtml"),
    APPLICATION("/admin/facelet/application.xhtml"),
    APPLICATION_CREATE("/admin/facelet/application-create.xhtml"),
    WEB_SERVICE_SECURITY("/admin/facelet/wss.xhtml"),
    WEB_SERVICE_SECURITY_CREATE("/admin/facelet/wss-create.xhtml"),
    STS_CREATE("/admin/facelet/sts-create.xhtml"),
    PERMISSION_DENIED("/admin/facelet/permission-denied.xhtml");

    private static final Map<String, ViewId> idValues = new HashMap<String, ViewId>() {

        {
            put(HOME.getId(), HOME);
            put(POLICY.getId(), POLICY);
            put(POLICY_CREATE.getId(), POLICY_CREATE);
            put(POLICY_MANAGE.getId(), POLICY_MANAGE);
            put(POLICY_EDIT.getId(), POLICY_EDIT);
            put(REFERRAL_CREATE.getId(), REFERRAL_CREATE);
            put(REFERRAL_MANAGE.getId(), REFERRAL_MANAGE);
            put(REFERRAL_EDIT.getId(), REFERRAL_EDIT);
            put(SAMLV2_HOSTED_SP_CREATE.getId(), SAMLV2_HOSTED_SP_CREATE);
            put(SAMLV2_REMOTE_SP_CREATE.getId(), SAMLV2_REMOTE_SP_CREATE);
            put(SAMLV2_HOSTED_IDP_CREATE.getId(), SAMLV2_HOSTED_IDP_CREATE);
            put(SAMLV2_REMOTE_IDP_CREATE.getId(), SAMLV2_REMOTE_IDP_CREATE);
            put(FEDERATION.getId(), FEDERATION);
            put(NEWS.getId(), NEWS);
            put(APPLICATION.getId(), APPLICATION);
            put(APPLICATION_CREATE.getId(), APPLICATION_CREATE);
            put(WEB_SERVICE_SECURITY.getId(), WEB_SERVICE_SECURITY);
            put(WEB_SERVICE_SECURITY_CREATE.getId(), WEB_SERVICE_SECURITY_CREATE);
            put(STS_CREATE.getId(), STS_CREATE);
            put(PERMISSION_DENIED.getId(), PERMISSION_DENIED);
        }
    };

    private String id;

    ViewId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static ViewId valueOfId(String id) {
        ViewId vid = idValues.get(id);
        return vid;
    }

    public Permission getPermission() {
        return Permission.valueOf(this.toString());
    }
}
