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
 * $Id: Permission.java,v 1.14 2009/12/08 02:13:41 babysunil Exp $
 */

package com.sun.identity.admin.model;

public enum Permission {
    HOME,
    ENTITLEMENTS,
    POLICY,
    POLICY_CREATE,
    POLICY_MANAGE,
    POLICY_VIEW,
    POLICY_EDIT,
    NEWS,
    REFERRAL,
    REFERRAL_CREATE,
    REFERRAL_MANAGE,
    REFERRAL_VIEW,
    REFERRAL_EDIT,
    FEDERATION,
    SAMLV2_HOSTED_SP_CREATE,
    SAMLV2_HOSTED_IDP_CREATE,
    SAMLV2_REMOTE_SP_CREATE,
    SAMLV2_REMOTE_IDP_CREATE,
    WEBEX_CONFIG,
    APPLICATION,
    APPLICATION_CREATE,
    APPLICATION_VIEW,
    APPLICATION_EDIT,
    APPLICATION_MANAGE,
    DELEGATION,
    DELEGATION_CREATE,
    DELEGATION_VIEW,
    DELEGATION_EDIT,
    DELEGATION_MANAGE,
    WEB_SERVICE_SECURITY,
    WSP_CREATE,
    WSP_MANAGE,
    WSC_CREATE,
    WSC_MANAGE,
    STS_MANAGE;
}
