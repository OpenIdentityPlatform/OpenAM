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
 * $Id: ViewId.java,v 1.14 2009/12/08 02:13:42 babysunil Exp $
 */

package com.sun.identity.admin.model;

import java.util.HashMap;
import java.util.Map;

public enum ViewId {

    HOME("/admin/facelet/home.xhtml"),
    POLICY("/admin/facelet/policy.xhtml"),
    POLICY_CREATE("/admin/facelet/policy-create.xhtml"),
    POLICY_MANAGE("/admin/facelet/policy-manage.xhtml"),
    POLICY_VIEW("/admin/facelet/policy-view.xhtml"),
    POLICY_EDIT("/admin/facelet/policy-edit.xhtml"),
    REFERRAL_CREATE("/admin/facelet/referral-create.xhtml"),
    REFERRAL_MANAGE("/admin/facelet/referral-manage.xhtml"),
    REFERRAL_VIEW("/admin/facelet/referral-view.xhtml"),
    REFERRAL_EDIT("/admin/facelet/referral-edit.xhtml"),
    DELEGATION_VIEW("/admin/facelet/delegation-view.xhtml"),
    DELEGATION_EDIT("/admin/facelet/delegation-edit.xhtml"),
    DELEGATION_CREATE("/admin/facelet/delegation-create.xhtml"),
    DELEGATION_MANAGE("/admin/facelet/delegation-manage.xhtml"),
    FEDERATION("/admin/facelet/federation.xhtml"),
    SAMLV2_HOSTED_SP_CREATE("/admin/facelet/samlv2-hosted-sp-create.xhtml"),
    SAMLV2_REMOTE_SP_CREATE("/admin/facelet/samlv2-remote-sp-create.xhtml"),
    SAMLV2_HOSTED_IDP_CREATE("/admin/facelet/samlv2-hosted-idp-create.xhtml"),
    SAMLV2_REMOTE_IDP_CREATE("/admin/facelet/samlv2-remote-idp-create.xhtml"),
    WEBEX_CONFIG("/admin/facelet/webex-config.xhtml"),
    NEWS("/admin/facelet/news.xhtml"),
    APPLICATION("/admin/facelet/application.xhtml"),
    APPLICATION_CREATE("/admin/facelet/application-create.xhtml"),
    APPLICATION_EDIT("/admin/facelet/application-edit.xhtml"),
    APPLICATION_VIEW("/admin/facelet/application-view.xhtml"),
    APPLICATION_MANAGE("/admin/facelet/application-manage.xhtml"),
    WEB_SERVICE_SECURITY("/admin/facelet/wss.xhtml"),
    WSP_CREATE("/admin/facelet/wsp-create.xhtml"),
    WSP_MANAGE("/admin/facelet/wsp-manage.xhtml"),
    WSC_CREATE("/admin/facelet/wsc-create.xhtml"),
    WSC_MANAGE("/admin/facelet/wsc-manage.xhtml"),
    STS_MANAGE("/admin/facelet/sts-manage.xhtml"),
    PERMISSION_DENIED("/admin/facelet/permission-denied.xhtml");

    private static final Map<String, ViewId> idValues = new HashMap<String, ViewId>() {

        {
            put(HOME.getId(), HOME);
            put(POLICY.getId(), POLICY);
            put(POLICY_CREATE.getId(), POLICY_CREATE);
            put(POLICY_MANAGE.getId(), POLICY_MANAGE);
            put(POLICY_VIEW.getId(), POLICY_VIEW);
            put(POLICY_EDIT.getId(), POLICY_EDIT);
            put(REFERRAL_CREATE.getId(), REFERRAL_CREATE);
            put(REFERRAL_MANAGE.getId(), REFERRAL_MANAGE);
            put(REFERRAL_VIEW.getId(), REFERRAL_VIEW);
            put(REFERRAL_EDIT.getId(), REFERRAL_EDIT);
            put(DELEGATION_CREATE.getId(), DELEGATION_CREATE);
            put(DELEGATION_VIEW.getId(), DELEGATION_VIEW);
            put(DELEGATION_EDIT.getId(), DELEGATION_EDIT);
            put(DELEGATION_MANAGE.getId(), DELEGATION_MANAGE);
            put(SAMLV2_HOSTED_SP_CREATE.getId(), SAMLV2_HOSTED_SP_CREATE);
            put(SAMLV2_REMOTE_SP_CREATE.getId(), SAMLV2_REMOTE_SP_CREATE);
            put(SAMLV2_HOSTED_IDP_CREATE.getId(), SAMLV2_HOSTED_IDP_CREATE);
            put(SAMLV2_REMOTE_IDP_CREATE.getId(), SAMLV2_REMOTE_IDP_CREATE);
            put(FEDERATION.getId(), FEDERATION);
            put(WEBEX_CONFIG.getId(), WEBEX_CONFIG);
            put(NEWS.getId(), NEWS);
            put(APPLICATION.getId(), APPLICATION);
            put(APPLICATION_CREATE.getId(), APPLICATION_CREATE);
            put(APPLICATION_MANAGE.getId(), APPLICATION_MANAGE);
            put(APPLICATION_EDIT.getId(), APPLICATION_EDIT);
            put(APPLICATION_VIEW.getId(), APPLICATION_VIEW);
            put(WEB_SERVICE_SECURITY.getId(), WEB_SERVICE_SECURITY);
            put(WSP_CREATE.getId(), WSP_CREATE);
            put(WSP_MANAGE.getId(), WSP_MANAGE);
            put(WSC_CREATE.getId(), WSC_CREATE);
            put(WSC_MANAGE.getId(), WSC_MANAGE);
            put(STS_MANAGE.getId(), STS_MANAGE);
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
