package com.sun.identity.admin.model;

import java.util.HashMap;
import java.util.Map;

public enum FromAction {
    HOME("home"),
    POLICY("policy"),
    POLICY_CREATE("policy-create"),
    POLICY_MANAGE("policy-manage"),
    POLICY_EDIT("policy-edit"),
    REFERRAL_CREATE("referral-create"),
    REFERRAL_MANAGE("referral-manage"),
    REFERRAL_EDIT("referral-edit"),
    NEWS("news"),
    FEDERATION("federation"),
    SAMLV2_HOSTED_SP_CREATE("samlv2-hosted-sp-create"),
    SAMLV2_REMOTE_SP_CREATE("samlv2-remote-sp-create"),
    SAMLV2_HOSTED_IDP_CREATE("samlv2-hosted-idp-create"),
    SAMLV2_REMOTE_IDP_CREATE("samlv2-remote-idp-create"),
    WEB_SERVICE_SECURITY("wss"),
    WEB_SERVICE_SECURITY_CREATE("wss-create"),
    STS_CREATE("sts-create"),
    APPLICATION("application"),
    APPLICATION_CREATE("application-create"),
    PERMISSION_DENIED("permission-denied");

    private static final Map<String,FromAction> actionValues = new HashMap<String,FromAction>() {
        {
            put(HOME.getAction(), HOME);
            put(POLICY.getAction(), POLICY);
            put(POLICY_CREATE.getAction(), POLICY_CREATE);
            put(POLICY_MANAGE.getAction(), POLICY_MANAGE);
            put(POLICY_EDIT.getAction(), POLICY_EDIT);
            put(REFERRAL_CREATE.getAction(), REFERRAL_CREATE);
            put(REFERRAL_MANAGE.getAction(), REFERRAL_MANAGE);
            put(REFERRAL_EDIT.getAction(), REFERRAL_EDIT);
            put(SAMLV2_HOSTED_SP_CREATE.getAction(), SAMLV2_HOSTED_SP_CREATE);
            put(SAMLV2_REMOTE_SP_CREATE.getAction(), SAMLV2_REMOTE_SP_CREATE);
            put(SAMLV2_HOSTED_IDP_CREATE.getAction(), SAMLV2_HOSTED_IDP_CREATE);
            put(SAMLV2_REMOTE_IDP_CREATE.getAction(), SAMLV2_REMOTE_IDP_CREATE);
            put(FEDERATION.getAction(), FEDERATION);
            put(NEWS.getAction(), NEWS);
            put(WEB_SERVICE_SECURITY.getAction(), WEB_SERVICE_SECURITY);
            put(WEB_SERVICE_SECURITY_CREATE.getAction(), WEB_SERVICE_SECURITY_CREATE);
            put(STS_CREATE.getAction(), STS_CREATE);
            put(APPLICATION.getAction(), APPLICATION);
            put(APPLICATION_CREATE.getAction(), APPLICATION_CREATE);
            put(PERMISSION_DENIED.getAction(), PERMISSION_DENIED);
        }
    };

    private String action;

    FromAction(String action) {
        this.action = action;
    }

    public Permission toPermission() {
        return Permission.valueOf(this.toString());
    }

    public String getAction() {
        return action;
    }

    public static FromAction valueOfAction(String action) {
        return actionValues.get(action);
    }

    public ViewId getViewId() {
        return ViewId.valueOf(this.toString());
    }
}
