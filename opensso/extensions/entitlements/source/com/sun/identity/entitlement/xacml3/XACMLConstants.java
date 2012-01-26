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
 * at opensso/legal/CDDLv1.0.txt
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: XACMLConstants.java,v 1.4 2009/06/16 00:59:30 dillidorai Exp $
 */
package com.sun.identity.entitlement.xacml3;

/**
 * Interface that defines constants used by XACML classes
 */
public final class XACMLConstants {

public static final String XACML3_CORE_PKG 
        = "com.sun.identity.entitlement.xacml3.core";

public static final String PRIVILEGE_CREATED_BY 
        = "createdBy";
public static final String PRIVILEGE_LAST_MODIFIED_BY 
        = "lastModifiedBy";

public static final String XS_STRING 
        = "htp://www.w3.org/2001/XMLSchema#string";
public static final String XS_DATE_TIME 
        = "htp://www.w3.org/2001/XMLSchema#dateTime";

public static final String PRIVILEGE_CREATION_DATE 
        = "creationDate";
public static final String PRIVILEGE_LAST_MODIFIED_DATE 
        = "lastModifiedDate";

public static final String PREMIT_RULE_SUFFIX 
        = "permit-rule";
public static final String DENY_RULE_SUFFIX 
        = "deny-rule";

public static final String PERMIT_RULE_DESCRIPTION 
        = "Permit Rule";
public static final String DENY_RULE_DESCRIPTION 
        = "Deny Rule";


public static final String XACML_SUBJECT_ID 
        = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";
public static final String JSON_SUBJECT_ID 
        = "urn:sun:opensso:entitlement:json-subject";
public static final String XACML_ACCESS_SUBJECT_CATEGORY 
        = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";
public static final String JSON_SUBJECT_MATCH 
        = "urn:sun:opensso:entitlement:json-subject-match";
public static final String JSON_SUBJECT_DATATYPE 
        = "urn:sun:opensso:entitlement:json-subject-type";
public static final String SUBJECT_ISSUER 
        = "urn:sun:opensso";

public static final String XACML_RESOURCE_ID 
        = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";
public static final String XACML_RESOURCE_CATEGORY 
        = "urn:oasis:names:tc:xacml:3.0:attribute-category:resource";
public static final String ENTITLEMENT_RESOURCE_MATCH 
        = "urn:sun:opensso:entitlement:resource-match:application";
public static final String ENTITLEMENT_RESOURCE_NO_MATCH 
        = "urn:sun:opensso:entitlement:resource-no-match:application";
public static final String RESOURCE_ISSUER 
        = "urn:sun:opensso";

public static final String XACML_ACTION_ID 
        = "urn:oasis:names:tc:xacml:1.0:action:action-id";
public static final String XACML_ACTION_CATEGORY 
        = "urn:oasis:names:tc:xacml:3.0:attribute-category:action";
public static final String ENTITLEMENT_ACTION_MATCH 
        = "urn:sun:opensso:entitlement:action-match:application";
public static final String ACTION_ISSUER 
        = "urn:sun:opensso";

public static final String JSON_SUBJECT_AND_CONDITION_SATISFIED 
        = "urn:sun:opensso:entitlement:json-subject-and-condiiton-satisfied";
public static final String JSON_CONDITION_DATATYPE 
        = "urn:sun:opensso:entitlement:json-condition-type";

// XACML standard combining algorithms
public static final String XACML_RULE_DENY_OVERRIDES
        = "urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:deny-overrides";
public static final String XACML_POLICY_DENY_OVERRIDES
        = "urn:oasis:names:tc:xacml:3.0:policy-combining-algorithm:deny-overrides";
public static final String XACML_RULE_PERMIT_OVERRIDES
        = "urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:permit-overrides";
public static final String XACML_POLICY_PERMIT_OVERRIDES
        = "urn:oasis:names:tc:xacml:3.0:policy-combining-algorithm:permit-overrides";
public static final String XACML_RULE_DENY_UNLESS_PERMIT
        = "urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:deny-unless-permit";
public static final String XACML_POLICY_DENY_UNLESS_PERMIT
        = "urn:oasis:names:tc:xacml:3.0:policy-combining-algorithm:deny-unless-permit";
public static final String XACML_RULE_PERMIT_UNLESS_DENY
        = "urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:permit-unless-deny";
public static final String XACML_POLICY_PERMIT_UNLESS_DENY
        = "urn:oasis:names:tc:xacml:3.0:policy-combining-algorithm:permit-unless-deny";

}

