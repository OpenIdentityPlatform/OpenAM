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
 * $Id: XACMLPrivilegeUtils.java,v 1.4 2010/01/10 06:39:42 dillidorai Exp $
 */
/**
 * Portions Copyrighted 2011-2013 ForgeRock AS
 * Portions Copyrighted 2013 Nomura Research Institute, Ltd
 */
package com.sun.identity.entitlement.xacml3;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ResourceAttribute;

import com.sun.identity.entitlement.UserSubject;

import com.sun.identity.entitlement.opensso.XACMLOpenSSOPrivilege;
import com.sun.identity.entitlement.xacml3.core.AllOf;
import com.sun.identity.entitlement.xacml3.core.Apply;
import com.sun.identity.entitlement.xacml3.core.AnyOf;
import com.sun.identity.entitlement.xacml3.core.AttributeValue;
import com.sun.identity.entitlement.xacml3.core.AttributeDesignator;
import com.sun.identity.entitlement.xacml3.core.Condition;
import com.sun.identity.entitlement.xacml3.core.EffectType;
import com.sun.identity.entitlement.xacml3.core.Match;
import com.sun.identity.entitlement.xacml3.core.ObjectFactory;
import com.sun.identity.entitlement.xacml3.core.Policy;
import com.sun.identity.entitlement.xacml3.core.PolicySet;
import com.sun.identity.entitlement.xacml3.core.Rule;
import com.sun.identity.entitlement.xacml3.core.Target;
import com.sun.identity.entitlement.xacml3.core.VariableDefinition;
import com.sun.identity.entitlement.xacml3.core.Version;

import com.sun.identity.shared.JSONUtils;
import com.sun.identity.shared.xml.XMLUtils;

import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;

/**
 * Class with utility methods to map from
 * <code>com.sun.identity.entity.Privilege</code>
 * to
 * </code>com.sun.identity.entitlement.xacml3.core.Policy</code>
 */
public class XACMLPrivilegeUtils {
    /**
     * Constructs XACMLPrivilegeUtils
     */
    private XACMLPrivilegeUtils() {
    }

    public static String toXACML(Privilege privilege) {
        if (privilege == null) {
            return "";
        }
        Policy policy = privilegeToPolicy(privilege);
        return toXML(policy);
    }

    public static String toXML(Policy policy) {
        if (policy == null) {
            return "";
        }
        StringWriter stringWriter = new StringWriter();
        try {
            ObjectFactory objectFactory = new ObjectFactory();
            JAXBContext jaxbContext = JAXBContext.newInstance(
                    XACMLConstants.XACML3_CORE_PKG);
            JAXBElement<Policy> policyElement
                    = objectFactory.createPolicy(policy);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                    Boolean.TRUE);
            marshaller.marshal(policyElement, stringWriter);
        } catch (JAXBException je) {
            //TOODO: handle, propogate exception
            PrivilegeManager.debug.error(
                "JAXBException while mapping privilege to policy:", je);
        }
        return stringWriter.toString();
    }

    public static String toXML(PolicySet policySet) {
        if (policySet == null) {
            return "";
        }
        StringWriter stringWriter = new StringWriter();
        try {
            ObjectFactory objectFactory = new ObjectFactory();
            JAXBContext jaxbContext = JAXBContext.newInstance(
                    XACMLConstants.XACML3_CORE_PKG);
            JAXBElement<PolicySet> policySetElement
                    = objectFactory.createPolicySet(policySet);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                    Boolean.TRUE);
            marshaller.marshal(policySetElement, stringWriter);
        } catch (JAXBException je) {
            //TOODO: handle, propogate exception
            PrivilegeManager.debug.error(
                "JAXBException while mapping privilege to policy:", je);
        }
        return stringWriter.toString();
    }

    

    public static Policy privilegeToPolicy(Privilege privilege)  {
        Policy policy = null;
        try {
            policy = privilegeToPolicyInternal(privilege);
        } catch (JAXBException je) {
            //TODO: log error, jaxbexception
        }
        return policy;
    }

    private static Policy privilegeToPolicyInternal(Privilege privilege) throws
            JAXBException  {

        /*
         * See entitelement meeting minutes - 22apr09
         *
         * privilege name would map to policy id
         *
         * application name would map to application category attribute
         *
         * entitlement resource names would map to xacml policy target
         *
         * entitlement excluded resource names would map to xacml rule target
         *
         * simple one level entitlement subjects (without or, and etc) 
         * would map to policy target
         *
         * all entitlement subjects would also map to xacml rule condition
         *
         * entitlement conditions would map to xacml rule condition
         *
         * entitlement resource attributes would map to rule advice expression
         *
         * at present xacml obligation support is out of scope 
         */

        if (privilege == null) {
            return null;
        }

        Policy policy = new Policy();

        String privilegeName = privilege.getName();
        String applicationName = null; 
        String entitlementName = null;
        Entitlement entitlement = privilege.getEntitlement();
        if (entitlement != null) {
            applicationName = entitlement.getApplicationName();
            entitlementName = entitlement.getName();
        }

        String policyId = privilegeNameToPolicyId(privilegeName,
                applicationName);
        policy.setPolicyId(policyId);

        String description = privilege.getDescription();
        policy.setDescription(description);

        List<Object> vrList 
            = policy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition();

        ObjectFactory objectFactory = new ObjectFactory();
        JAXBContext jaxbContext = JAXBContext.newInstance(
                XACMLConstants.XACML3_CORE_PKG);

        if (applicationName != null) {
            VariableDefinition appName = new VariableDefinition();
            vrList.add(appName);
            appName.setVariableId(XACMLConstants.APPLICATION_NAME);
            AttributeValue cbv = new AttributeValue();
            cbv.setDataType(XACMLConstants.XS_STRING);
            cbv.getContent().add(applicationName);
            JAXBElement<AttributeValue> cbve 
                    = objectFactory.createAttributeValue(cbv);
            appName.setExpression(cbve);
        }

        if (entitlementName != null) {
            VariableDefinition entName = new VariableDefinition();
            vrList.add(entName);
            entName.setVariableId(XACMLConstants.ENTITLEMENT_NAME);
            AttributeValue cbv = new AttributeValue();
            cbv.setDataType(XACMLConstants.XS_STRING);
            cbv.getContent().add(applicationName);
            JAXBElement<AttributeValue> cbve 
                    = objectFactory.createAttributeValue(cbv);
            entName.setExpression(cbve);
        }

        VariableDefinition createdBy = new VariableDefinition();
        vrList.add(createdBy);
        createdBy.setVariableId(XACMLConstants.PRIVILEGE_CREATED_BY);
        AttributeValue cbv = new AttributeValue();
        cbv.setDataType(XACMLConstants.XS_STRING);
        cbv.getContent().add(privilege.getCreatedBy());
        JAXBElement<AttributeValue> cbve 
                = objectFactory.createAttributeValue(cbv);
        createdBy.setExpression(cbve);

        VariableDefinition lastModifiedBy = new VariableDefinition();
        vrList.add(lastModifiedBy);
        lastModifiedBy.setVariableId(XACMLConstants.PRIVILEGE_LAST_MODIFIED_BY);
        AttributeValue lmbv = new AttributeValue();
        lmbv.setDataType(XACMLConstants.XS_STRING);
        lmbv.getContent().add(privilege.getLastModifiedBy());
        JAXBElement<AttributeValue> lmbve 
                = objectFactory.createAttributeValue(lmbv);
        lastModifiedBy.setExpression(cbve);

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss.SSS");
        SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS");
        sdf1.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf2.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf3.setTimeZone(TimeZone.getTimeZone("GMT"));

        VariableDefinition creationDate = new VariableDefinition();
        vrList.add(creationDate); 
        creationDate.setVariableId(XACMLConstants.PRIVILEGE_CREATION_DATE);
        AttributeValue cdv = new AttributeValue();
        cdv.setDataType(XACMLConstants.XS_DATE_TIME);
        cdv.getContent().add(
                sdf1.format(privilege.getCreationDate())
                + "T"
                + sdf2.format(privilege.getCreationDate()));
        JAXBElement<AttributeValue> cdve 
                = objectFactory.createAttributeValue(cdv);
        creationDate.setExpression(cdve);

        VariableDefinition lastModifiedDate = new VariableDefinition();
        vrList.add(lastModifiedDate); 
        lastModifiedDate.setVariableId(
                XACMLConstants.PRIVILEGE_LAST_MODIFIED_DATE);
        AttributeValue lmdv = new AttributeValue();
        lmdv.setDataType(XACMLConstants.XS_DATE_TIME);
        lmdv.getContent().add(
                sdf1.format(privilege.getLastModifiedDate())
                + "T"
                + sdf2.format(privilege.getLastModifiedDate()));
        JAXBElement<AttributeValue> lmdve 
                = objectFactory.createAttributeValue(lmdv);
        lastModifiedDate.setExpression(lmdve);

        // PolicyIssuer policyIssuer = null;  // optional, TODO

        Version version = new Version();

        // TODO: use privilege version in future
        version.setValue(sdf3.format(privilege.getLastModifiedDate())); 
        policy.setVersion(version);

        // Defaults policyDefaults = null; // optional, TODO

        String rca = getRuleCombiningAlgId(applicationName);
        policy.setRuleCombiningAlgId(rca);

        // String ruleCombiningAlgId = "rca"; // required

        // XACML Target contains a  list of AnyOf(s)
        // XACML AnyOf contains a list of AllOf(s)
        // XACML AllOf contains a list of Match(s)

        Target target = new Target();
        policy.setTarget(target);

        List<AnyOf> targetAnyOfList = target.getAnyOf();

        EntitlementSubject es = privilege.getSubject();

        /* TODO: detect simple subjects and set attribute value and designator
        List<AnyOf> anyOfSubjectList = entitlementSubjectToAnyOfList(es);
        if (anyOfSubjectList != null) {
            targetAnyOfList.addAll(anyOfSubjectList);
        }
        */

        AnyOf anyOfSubject = entitlementSubjectToAnyOf(es);
        if (anyOfSubject != null) {
            targetAnyOfList.add(anyOfSubject);
        }

        Set<String> resources = entitlement.getResourceNames();


        List<AnyOf> anyOfResourceList = resourceNamesToAnyOfList(resources,
                applicationName);
        if (anyOfResourceList != null) {
            targetAnyOfList.addAll(anyOfResourceList);
        }

        AnyOf anyOfApplication = applicationNameToAnyOf(applicationName);
        if (anyOfApplication != null) {
            targetAnyOfList.add(anyOfApplication);
        }

        Map<String, Boolean> actionValues = entitlement.getActionValues();
        List<AnyOf> anyOfActionList 
                = actionNamesToAnyOfList(actionValues.keySet(),
                applicationName);
        if (anyOfActionList != null) {
            targetAnyOfList.addAll(anyOfActionList);
        }

        // PermitRule, DenyRule
        Set<String> permitActions = new HashSet<String>();
        Set<String> denyActions = new HashSet<String>();
        if (actionValues != null) {
            Set<String> actionNames = actionValues.keySet();
            for(String actionName : actionNames) {
                if (Boolean.TRUE.equals(actionValues.get(actionName))) {
                    permitActions.add(actionName);
                } else {
                    denyActions.add(actionName);
                }
            }
        }

        Set<String> excludedResources = entitlement.getExcludedResourceNames();
        List<AnyOf> anyOfExcludedResourceList
                = excludedResourceNamesToAnyOfList(
                excludedResources, applicationName);
        Condition condition = eSubjectConditionToXCondition(
                privilege.getSubject(), privilege.getCondition());

        // FIXME: add it to Policy as AdviceExpressions?
        Set<ResourceAttribute> ra = privilege.getResourceAttributes();

        if (!permitActions.isEmpty()) {
            Rule permitRule = new Rule();
            vrList.add(permitRule);
            permitRule.setRuleId(entitlement.getName() + ":"
                    + XACMLConstants.PREMIT_RULE_SUFFIX);
            permitRule.setDescription(XACMLConstants.PERMIT_RULE_DESCRIPTION);
            permitRule.setEffect(EffectType.PERMIT);
            Target permitTarget = new Target();
            permitRule.setTarget(permitTarget);
            List<AnyOf> permitTargetAnyOfList = permitTarget.getAnyOf();
            if (anyOfExcludedResourceList != null) {
                permitTargetAnyOfList.addAll(anyOfExcludedResourceList);
            }
            List<AnyOf> anyOfPermitActionList
                    = actionNamesToAnyOfList(permitActions, applicationName);
            if (anyOfPermitActionList != null) {
                permitTargetAnyOfList.addAll(anyOfPermitActionList);
            }
            if (condition != null) {
                permitRule.setCondition(condition);
            }
            
        }

        if (!denyActions.isEmpty()) {
            Rule denyRule = new Rule();
            vrList.add(denyRule);
            denyRule.setRuleId(entitlement.getName()  + ":"
                    + XACMLConstants.DENY_RULE_SUFFIX);
            denyRule.setDescription(XACMLConstants.DENY_RULE_DESCRIPTION);
            denyRule.setEffect(EffectType.DENY);
            Target denyTarget = new Target();
            denyRule.setTarget(denyTarget);
            List<AnyOf> denyTargetAnyOfList = denyTarget.getAnyOf();
            if (anyOfExcludedResourceList != null) {
                denyTargetAnyOfList.addAll(anyOfExcludedResourceList);
            }
            List<AnyOf> anyOfDenyActionList
                    = actionNamesToAnyOfList(denyActions, applicationName);
            if (anyOfDenyActionList != null) {
                denyTargetAnyOfList.addAll(anyOfDenyActionList);
            }
            if (condition != null) {
                denyRule.setCondition(condition);
            }
        }


        return policy;
    }

    public static String privilegeNameToPolicyId(String privilegeName,
            String applicationName) {
         //TODO: implement privilegeNameToPolicyId() correctly
        return privilegeName;
    }

    // TODO: not used now, use, test, fix and verify
    public static List<AnyOf> entitlementSubjectToAnyOfList(
            EntitlementSubject es) {
        if (es == null) {
            return null;
        }
        List<AnyOf> anyOfList = new ArrayList<AnyOf>();
        AnyOf anyOf = new AnyOf();
        anyOfList.add(anyOf);
        List<AllOf> allOfList = anyOf.getAllOf();
        AllOf allOf = new AllOf();
        allOfList.add(allOf);
        List<Match> matchList = allOf.getMatch();
        if (es instanceof UserSubject) {
            UserSubject us = (UserSubject)es;
            String userId = us.getID();

            Match match = new Match();
            matchList.add(match);
            match.setMatchId("user-subject-match");

            AttributeValue attributeValue = new AttributeValue();
            String dataType = "datatype";
            attributeValue.setDataType(dataType);
            attributeValue.getContent().add(userId);

            AttributeDesignator attributeDesignator = new AttributeDesignator();
            String category = "subject-category";
            attributeDesignator.setCategory(category);
            String attributeId = "user-subject:user-id";
            attributeDesignator.setAttributeId(attributeId);
            String dt = "xs;string";
            attributeDesignator.setDataType(dt);
            String issuer = "subject:issuer";
            // attributeDesignator.setIssuer(issuer); TODO: verify and fix
            boolean mustBePresent = true;
            attributeDesignator.setMustBePresent(mustBePresent);

            match.setAttributeValue(attributeValue);
            match.setAttributeDesignator(attributeDesignator);
        }
        return anyOfList;
    }


    public static AnyOf entitlementSubjectToAnyOf(
            EntitlementSubject es) throws JAXBException {
        if (es == null) {
            return null;
        }
        AnyOf anyOf = new AnyOf();
        List<AllOf> allOfList = anyOf.getAllOf();
        AllOf allOf = new AllOf();
        allOfList.add(allOf);
        List<Match> matchList = allOf.getMatch();

        Match match = new Match();
        matchList.add(match);
        match.setMatchId(XACMLConstants.JSON_SUBJECT_MATCH);

        AttributeValue attributeValue = new AttributeValue();
        String dataType = XACMLConstants.JSON_SUBJECT_DATATYPE + ":"
                + es.getClass().getName();
        attributeValue.setDataType(dataType);
        String esString = es.toString();
        attributeValue.getContent().add(esString);

        AttributeDesignator attributeDesignator = new AttributeDesignator();
        String category = XACMLConstants.XACML_ACCESS_SUBJECT_CATEGORY;
        attributeDesignator.setCategory(category);
        String attributeId = XACMLConstants.JSON_SUBJECT_ID;
        attributeDesignator.setAttributeId(attributeId);
        String dt = XACMLConstants.JSON_SUBJECT_DATATYPE + ":"
                + es.getClass().getName();
        attributeDesignator.setDataType(dt);
        String issuer = XACMLConstants.SUBJECT_ISSUER; // TODO: not a constant?
        //attributeDesignator.setIssuer(issuer); //TODO: verify and fix
        boolean mustBePresent = true;
        attributeDesignator.setMustBePresent(mustBePresent);

        match.setAttributeValue(attributeValue);
        match.setAttributeDesignator(attributeDesignator);

        return anyOf;
    }

    public static List<AnyOf> resourceNamesToAnyOfList(
            Set<String> resourceNames, String applicationName) {
        if (resourceNames == null || resourceNames.isEmpty()) {
            return null;
        }
        List<AnyOf> anyOfList = new ArrayList<AnyOf>();
        AnyOf anyOf = new AnyOf();
        anyOfList.add(anyOf);
        List<AllOf> allOfList = anyOf.getAllOf();
        for (String resourceName : resourceNames) {
            AllOf allOf = new AllOf();
            List<Match> matchList = allOf.getMatch();
            matchList.add(resourceNameToMatch(resourceName, applicationName));
            allOfList.add(allOf);
        }
        return anyOfList;
    }

    public static AnyOf applicationNameToAnyOf(String applicationName) {
        AnyOf anyOf = new AnyOf();
        List<AllOf> allOfList = anyOf.getAllOf();
        AllOf allOf = new AllOf();
        List<Match> matchList = allOf.getMatch();
        matchList.add(applicationNameToMatch(applicationName));
        allOfList.add(allOf);
        return anyOf;
    }

    public static List<AnyOf> actionNamesToAnyOfList(
            Set<String> actionNames, String applicationName) {
        if (actionNames == null || actionNames.isEmpty()) {
            return null;
        }
        List<AnyOf> anyOfList = new ArrayList<AnyOf>();
        AnyOf anyOf = new AnyOf();
        anyOfList.add(anyOf);
        List<AllOf> allOfList = anyOf.getAllOf();
        for (String actionName : actionNames) {
            AllOf allOf = new AllOf();
            List<Match> matchList = allOf.getMatch();
            matchList.add(actionNameToMatch(actionName, applicationName));
            allOfList.add(allOf);
        }
        return anyOfList;
    }

    public static List<AnyOf> excludedResourceNamesToAnyOfList(
            Set<String> excludedResources, String applicationName) {
        if (excludedResources == null || excludedResources.isEmpty()) {
            return null;
        }
        List<AnyOf> anyOfList = new ArrayList<AnyOf>();
        AnyOf anyOf = new AnyOf();
        anyOfList.add(anyOf);
        List<AllOf> allOfList = anyOf.getAllOf();
        for (String resourceName : excludedResources) {
            AllOf allOf = new AllOf();
            List<Match> matchList = allOf.getMatch();
            matchList.add(resourceNameToNotMatch(resourceName, applicationName));
            allOfList.add(allOf);
        }
        return anyOfList;
    }

    public static Match resourceNameToMatch(String resourceName,
            String applicationName) {
        if (resourceName == null || resourceName.length() == 0) {
            return null;
        }

        Match match = new Match();
        String matchId = XACMLConstants.ENTITLEMENT_RESOURCE_MATCH + ":"
                + applicationName;
        match.setMatchId(matchId);

        AttributeValue attributeValue = new AttributeValue();
        String dataType = XACMLConstants.XS_STRING;
        attributeValue.setDataType(dataType);
        attributeValue.getContent().add(resourceName);

        AttributeDesignator attributeDesignator = new AttributeDesignator();
        String category = XACMLConstants.XACML_RESOURCE_CATEGORY;
        attributeDesignator.setCategory(category);
        String attributeId = XACMLConstants.XACML_RESOURCE_ID;
        attributeDesignator.setAttributeId(attributeId);
        String dt = XACMLConstants.XS_STRING;
        attributeDesignator.setDataType(dt);
        String issuer = XACMLConstants.RESOURCE_ISSUER; // TOOD: not a constant?
        // attributeDesignator.setIssuer(issuer); TODO: verify and fix
        boolean mustBePresent = true;
        attributeDesignator.setMustBePresent(mustBePresent);

        match.setAttributeValue(attributeValue);
        match.setAttributeDesignator(attributeDesignator);

        return match;
    }

    public static Match resourceNameToNotMatch(String resourceName,
            String applicationName) {
        if (resourceName == null || resourceName.length() == 0) {
            return null;
        }

        Match match = new Match();
        String matchId = XACMLConstants.ENTITLEMENT_RESOURCE_NO_MATCH + ":"
                + applicationName;
        match.setMatchId(matchId);

        AttributeValue attributeValue = new AttributeValue();
        String dataType = XACMLConstants.XS_STRING;
        attributeValue.setDataType(dataType);
        attributeValue.getContent().add(resourceName);

        AttributeDesignator attributeDesignator = new AttributeDesignator();
        String category = XACMLConstants.XACML_RESOURCE_CATEGORY;
        attributeDesignator.setCategory(category);
        String attributeId = XACMLConstants.XACML_RESOURCE_ID;
        attributeDesignator.setAttributeId(attributeId);
        String dt = XACMLConstants.XS_STRING;
        attributeDesignator.setDataType(dt);
        String issuer = XACMLConstants.RESOURCE_ISSUER; // TODO: not a constant?
        // attributeDesignator.setIssuer(issuer); TODO: verify and fix
        boolean mustBePresent = true;
        attributeDesignator.setMustBePresent(mustBePresent);

        match.setAttributeValue(attributeValue);
        match.setAttributeDesignator(attributeDesignator);

        return match;
    }

    public static Match actionNameToMatch(String actionName,
            String applicationName) {
        if (actionName == null || actionName.length() == 0) {
            return null;
        }

        Match match = new Match();
        String matchId = XACMLConstants.ENTITLEMENT_ACTION_MATCH + ":"
                + applicationName;
        match.setMatchId(matchId);

        AttributeValue attributeValue = new AttributeValue();
        String dataType = XACMLConstants.XS_STRING;
        attributeValue.setDataType(dataType);
        attributeValue.getContent().add(actionName);

        AttributeDesignator attributeDesignator = new AttributeDesignator();
        String category = XACMLConstants.XACML_ACTION_CATEGORY;
        attributeDesignator.setCategory(category);
        String attributeId = XACMLConstants.XACML_ACTION_ID;
        attributeDesignator.setAttributeId(attributeId);
        String dt = XACMLConstants.XS_STRING;
        attributeDesignator.setDataType(dt);
        String issuer = XACMLConstants.ACTION_ISSUER; // TODO: not a constant?
        // attributeDesignator.setIssuer(issuer); // TODO: verify and fix
        boolean mustBePresent = true;
        attributeDesignator.setMustBePresent(mustBePresent);

        match.setAttributeValue(attributeValue);
        match.setAttributeDesignator(attributeDesignator);

        return match;
    }

    public static Match applicationNameToMatch(String applicationName) {
        if (applicationName == null || applicationName.length() == 0) {
            return null;
        }

        Match match = new Match();
        String matchId = XACMLConstants.APPLICATION_MATCH;
        match.setMatchId(matchId);

        AttributeValue attributeValue = new AttributeValue();
        String dataType = XACMLConstants.XS_STRING;
        attributeValue.setDataType(dataType);
        attributeValue.getContent().add(applicationName);

        AttributeDesignator attributeDesignator = new AttributeDesignator();
        String category = XACMLConstants.APPLICATION_CATEGORY;
        attributeDesignator.setCategory(category);
        String attributeId = XACMLConstants.APPLICATION_ID;
        attributeDesignator.setAttributeId(attributeId);
        String dt = XACMLConstants.XS_STRING;
        attributeDesignator.setDataType(dt);
        String issuer = XACMLConstants.APPLICATION_ISSUER; // TODO: not a constant?
        // attributeDesignator.setIssuer(issuer); // TODO: verify and fix
        boolean mustBePresent = false;
        attributeDesignator.setMustBePresent(mustBePresent);

        match.setAttributeValue(attributeValue);
        match.setAttributeDesignator(attributeDesignator);

        return match;
    }

    public static Condition eSubjectConditionToXCondition(
            EntitlementSubject es, EntitlementCondition ec) 
            throws JAXBException {
        Condition condition = null;
        if (es != null || ec != null) {
            condition = new Condition();
            ObjectFactory objectFactory = new ObjectFactory();
            JAXBContext jaxbContext = JAXBContext.newInstance(
                    XACMLConstants.XACML3_CORE_PKG);

            Apply apply = new Apply();
            apply.setFunctionId(
                    XACMLConstants.JSON_SUBJECT_AND_CONDITION_SATISFIED);
            List applyExpressions = apply.getExpression();
            if (es != null) {
                String esString = es.toString();
                // TODO: add custom xml attribute to idenity as privilge subject
                AttributeValue esv = new AttributeValue();
                Map<QName, String> otherAttrs = esv.getOtherAttributes();
                QName qn = new QName("privilegeComponent");
                otherAttrs.put(qn, "entitlementSubject");
                String dataType = XACMLConstants.JSON_SUBJECT_DATATYPE + ":"
                        + es.getClass().getName();
                esv.setDataType(dataType);
                esv.getContent().add(esString);
                JAXBElement esve  = objectFactory.createAttributeValue(esv);
                applyExpressions.add(esve);
            }
            if (ec != null) {
                String ecString = ec.toString();
                // TODO: add custom xml attribute to idenity as privilge condition
                AttributeValue ecv = new AttributeValue();
                Map<QName, String> otherAttrs = ecv.getOtherAttributes();
                QName qn = new QName("privilegeComponent");
                otherAttrs.put(qn, "entitlementCondition");
                String dataType = XACMLConstants.JSON_CONDITION_DATATYPE + ":"
                        + ec.getClass().getName();
                ecv.setDataType(dataType);
                ecv.getContent().add(ecString);
                JAXBElement ecve  = objectFactory.createAttributeValue(ecv);
                applyExpressions.add(ecve);
            }
            JAXBElement applyElement  = objectFactory.createApply(apply);
            condition.setExpression(applyElement);
        }
        return condition;
    }

    public static String getRuleCombiningAlgId(String applicationName) {
        // TODO: return the correct alogrithm id based on application
        return XACMLConstants.XACML_RULE_DENY_OVERRIDES;
    }

    public static Set<Privilege> policySetToPrivileges(PolicySet policySet) 
            throws EntitlementException {
        if (policySet == null) {
            return null;
        }
        Set<Privilege> privileges = new HashSet<Privilege>();
        Set<Policy> policies = getPoliciesFromPolicySet(policySet);
        if (policies != null) {
            for (Policy policy : policies) {
                Privilege p = policyToPrivilege(policy);
                privileges.add(p);
            }
        }
        return privileges;
    }

    public static Privilege policyToPrivilege(Policy policy)
            throws EntitlementException {

        String policyId = policy.getPolicyId();
        String privilegeName = policyIdToPrivilegeName(policyId);
        String description = policy.getDescription();

        String createdBy = getVariableById(policy,
                XACMLConstants.PRIVILEGE_CREATED_BY);

        long createdAt = dateStringToLong(getVariableById(policy,
                XACMLConstants.PRIVILEGE_CREATION_DATE));

        String lastModifiedBy = getVariableById(policy,
                XACMLConstants.PRIVILEGE_LAST_MODIFIED_BY);

        long lastModifiedAt = dateStringToLong(getVariableById(policy,
                XACMLConstants.PRIVILEGE_LAST_MODIFIED_DATE));

        String entitlementName = getVariableById(policy, 
                XACMLConstants.ENTITLEMENT_NAME);

        String applicationName = getVariableById(policy, 
                XACMLConstants.APPLICATION_NAME);

        List<Match> policyMatches = getAllMatchesFromTarget(policy.getTarget());
        Set<String> resourceNames = getResourceNamesFromMatches(policyMatches);
        Set<String> excludedResourceNames = getExcludedResourceNamesFromMatches(policyMatches);
        Map<String, Boolean> actionValues = getActionValuesFromPolicy(policy);

        EntitlementSubject es = getEntitlementSubjectFromPolicy(policy);
        EntitlementCondition ec = getEntitlementConditionFromPolicy(policy);

        /*
         * Constuct entitlement from Rule target
         * Get resource names, excluded resource names, action names from Rule Match element
         * One Match for Action
         * One Rule per value
         */
        Entitlement entitlement = new Entitlement(applicationName, resourceNames, actionValues);
        entitlement.setExcludedResourceNames(excludedResourceNames);
        if (entitlementName != null) {
            entitlement.setName(entitlementName);
        }

        // FIXME: add support ResourceAttributes
        Set<ResourceAttribute> ras = null;

       
        Privilege privilege = new XACMLOpenSSOPrivilege();
        privilege.setName(privilegeName);
        privilege.setEntitlement(entitlement);
        privilege.setSubject(es);
        privilege.setCondition(ec);
        privilege.setResourceAttributes(ras);
        return privilege;
    }

    public static String policyIdToPrivilegeName(String policyId) {
        // FIXME: do some transform, not required at this time
        return policyId;
    }

    public static String getVariableById(Policy policy, String id) {
        String val = null;
        List<Object> vrList =
           policy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition();
        for (Object obj : vrList) {
            if (obj instanceof VariableDefinition) {
                VariableDefinition vd =(VariableDefinition)obj;
                if (vd.getVariableId().equals(id)) {
                    JAXBElement<AttributeValue> jav
                            = (JAXBElement<AttributeValue>)vd.getExpression();
                    AttributeValue cbav = (AttributeValue)jav.getValue();
                    val = cbav.getContent().get(0).toString();

                }
            }  
        }
        return val;
    }

    private static long dateStringToLong(String dateString) {
        if ((dateString) == null || (dateString.length() == 0)) {
            return 0;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(
                "yyyy-MM-dd:HH:mm:ss.SSSS");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        dateString = dateString.replace("T", ":");
        long time = 0;
        try {
            time = sdf.parse(dateString).getTime();
        } catch (java.text.ParseException pe) {
            //TODO: log debug warning
        }
        return time;
    }

    public static PolicySet privilegesToPolicySet(String realm, Set<Privilege> privileges) {
        PolicySet policySet = null;
        try {
            policySet = privilegesToPolicySetInternal(realm, privileges);
        } catch (JAXBException je) {
            //TODO: log error, jaxbexception
        }
        return policySet; 
    }

    private static PolicySet privilegesToPolicySetInternal(String realm, 
            Set<Privilege> privileges) throws JAXBException {
        if (privileges == null) {
            return null;
        }
        Set<Policy> policies = new HashSet<Policy>();
        for (Privilege privilege : privileges) {
            Policy policy = privilegeToPolicy(privilege);
            policies.add(policy);
        }
        PolicySet policySet = policiesToPolicySetInternal(realm, policies);
        return policySet; 
    }

    public static PolicySet newPolicySet(String realm)
            throws JAXBException {
        PolicySet policySet = new PolicySet();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String currentTime = sdf.format(System.currentTimeMillis());
        String policySetId  = realm + ":" + currentTime;

        policySet.setPolicySetId(policySetId);

        Version version = new Version();
        version.setValue(sdf.format(System.currentTimeMillis()));
        policySet.setVersion(version);

        // FIXME: is there a better choice?
        // policySet could contain policies for different applications
        policySet.setPolicyCombiningAlgId(XACMLConstants.XACML_RULE_DENY_OVERRIDES);

        Target target = new Target();
        policySet.setVersion(version);
        policySet.setTarget(target);

        return policySet;
    }
    private static PolicySet policiesToPolicySetInternal(String realm, Set<Policy> policies)
            throws JAXBException {
        PolicySet policySet = new PolicySet();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String currentTime = sdf.format(System.currentTimeMillis());
        String policySetId  = realm + ":" + currentTime;

        policySet.setPolicySetId(policySetId);

        Version version = new Version();
        version.setValue(sdf.format(System.currentTimeMillis()));
        policySet.setVersion(version);

        // FIXME: is there a better choice?
        // policySet could contain policies for different applications
        policySet.setPolicyCombiningAlgId(XACMLConstants.XACML_RULE_DENY_OVERRIDES);

        Target target = new Target();
        policySet.setVersion(version);
        policySet.setTarget(target);

        ObjectFactory objectFactory = new ObjectFactory();
        JAXBContext jaxbContext = JAXBContext.newInstance(
                XACMLConstants.XACML3_CORE_PKG);

       List<JAXBElement<?>> pList
            = policySet.getPolicySetOrPolicyOrPolicySetIdReference();
        if (policies != null) {
            for (Policy policy : policies) {
                JAXBElement<Policy> policyElement
                        = objectFactory.createPolicy(policy);
                pList.add(policyElement);
            }
        }
        return policySet;
    }

    public static PolicySet addPolicyToPolicySet(Policy policy,
            PolicySet policySet)
            throws JAXBException {
        if (policySet == null || policy == null) {
            return policySet;
        }
        ObjectFactory objectFactory = new ObjectFactory();
        JAXBContext jaxbContext = JAXBContext.newInstance(
                XACMLConstants.XACML3_CORE_PKG);

        List<JAXBElement<?>> pList
                = policySet.getPolicySetOrPolicyOrPolicySetIdReference();
        JAXBElement<Policy> policyElement = objectFactory.createPolicy(policy);
        pList.add(policyElement);
        return policySet;
    }

    static List<Match> getAllMatchesFromTarget(Target target) {
        List<AnyOf> anyOfList = target.getAnyOf();
        List<Match> matches = new ArrayList<Match>();
        for (AnyOf anyOf : anyOfList) {
            List<AllOf> allOfList = anyOf.getAllOf();
            for (AllOf allOf : allOfList) {
                List<Match> matchList = allOf.getMatch();
                matches.addAll(matchList);
            }
        }
        return matches;
    }

    static Set<String> getResourceNamesFromMatches(List<Match> matches) {
        if (matches == null) {
            return null;
        }
        Set<String> resourceNames = new HashSet<String>();
        for (Match match : matches) {
            String matchId = match.getMatchId();
            if ((matchId != null) && matchId.indexOf(":resource-match:") != -1) {
                AttributeValue attributeValue = match.getAttributeValue();
                if (attributeValue != null) {
                    List<Object> contentList = attributeValue.getContent();
                    if ((contentList != null) && !contentList.isEmpty()) {
                        // FIXME: log a warning if more than one element
                        Object obj = contentList.get(0);
                        resourceNames.add(obj.toString());
                    }
                }
            }
        }
        return resourceNames;
    }

    static Set<String> getExcludedResourceNamesFromMatches(List<Match> matches) {
        if (matches == null) {
            return null;
        }
        Set<String> resourceNames = new HashSet<String>();
        for (Match match : matches) {
            String matchId = match.getMatchId();
            if ((matchId != null) && matchId.indexOf(":resource-no-match:") != -1) {
                AttributeValue attributeValue = match.getAttributeValue();
                if (attributeValue != null) {
                    List<Object> contentList = attributeValue.getContent();
                    if ((contentList != null) && !contentList.isEmpty()) {
                        // FIXME: log a warning if more than one element
                        Object obj = contentList.get(0);
                        resourceNames.add(obj.toString());
                    }
                }
            }
        }
        return resourceNames;
    }

    static Set<String> getActionNamesFromMatches(List<Match> matches) {
        if (matches == null) {
            return null;
        }
        Set<String> actionNames = new HashSet<String>();
        for (Match match : matches) {
            String matchId = match.getMatchId();
            if ((matchId != null) && matchId.indexOf(":action-match:") != -1) {
                AttributeValue attributeValue = match.getAttributeValue();
                if (attributeValue != null) {
                    List<Object> contentList = attributeValue.getContent();
                    if ((contentList != null) && !contentList.isEmpty()) {
                        // FIXME: log a warning if more than one element
                        Object obj = contentList.get(0);
                        actionNames.add(obj.toString());
                    }
                }
            }
        }
        return actionNames;
    }
    
    static JSONObject getRealmsAppsResources(List<Match> matches)
            throws JSONException {
        if (matches == null) {
            return null;
        }
        JSONObject jo = null;
        String jsonString = null;
        for (Match match : matches) {
            String matchId = match.getMatchId();
            if ((matchId != null) && matchId.equals(
                    XACMLConstants.JSON_REALMS_APPS_RESOURCES_MATCH)) {
                AttributeValue attributeValue = match.getAttributeValue();
                if (attributeValue != null) {
                    List<Object> contentList = attributeValue.getContent();
                    if ((contentList != null) && !contentList.isEmpty()) {
                        Object obj = contentList.get(0);
                        jsonString =obj.toString();
                        break;
                    }
                } 
            }
        }
        if (jsonString != null) {
            jo = new JSONObject(jsonString);
        }
        return jo;
    }

    static List<Rule> getRules(Policy policy) {
        if (policy == null) {
            return null;
        }
        List<Rule> ruleList = new ArrayList<Rule>();
        List<Object> obList = policy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition();
        for (Object ob : obList) {
            if (ob instanceof Rule) {
                ruleList.add((Rule)ob);
            }
        }
        return ruleList;
    }

    static Map<String, Boolean> getActionValuesFromPolicy(Policy policy) {
        if (policy == null) {
            return null;
        }
        List<Rule> rules = getRules(policy);
        if (rules == null) {
            return null;
        }
        Map<String, Boolean> actionValues = new HashMap<String, Boolean>();
        for (Rule rule : rules) {
            Target target = rule.getTarget();
            List<Match> matches = getAllMatchesFromTarget(target);
            Set<String> actions = getActionNamesFromMatches(matches);
            EffectType effectType = rule.getEffect();
            for (String action : actions) {
                actionValues.put(action,
                        (EffectType.PERMIT == effectType) ? Boolean.TRUE : Boolean.FALSE);
            }
        }
        return actionValues;
    }

    static EntitlementSubject getEntitlementSubjectFromPolicy(Policy policy) {
        if (policy == null) {
            return null;
        }
        List<Rule> rules = getRules(policy);
        if (rules == null) {
            return null;
        }
        EntitlementSubject es = null;
        for (Rule rule : rules) {
            Condition condition = rule.getCondition();
            JAXBElement jaxbElement = condition.getExpression();
            if (jaxbElement.getDeclaredType().equals(Apply.class)) {
                Apply apply = (Apply)jaxbElement.getValue();
                String functionId = apply.getFunctionId();
                if (XACMLConstants.JSON_SUBJECT_AND_CONDITION_SATISFIED.equals(
                        functionId)) {
                    List<JAXBElement<?>> expressionList = apply.getExpression();
                    for (JAXBElement jaxe : expressionList) {
                        if (jaxe.getDeclaredType().equals(AttributeValue.class)) {
                            AttributeValue av = (AttributeValue)jaxe.getValue();
                            String dataType = av.getDataType();
                            if (dataType.startsWith(
                                    XACMLConstants.JSON_SUBJECT_DATATYPE)) {
                                List<Object> valueList = av.getContent();
                                String value = null;
                                if (valueList != null) {
                                    for (Object ob : valueList) {
                                        if (ob instanceof String) {
                                            value = (String)ob;
                                            break;
                                        }
                                    }
                                }
                                if (value != null) {
                                    es = createEntitlementSubject(dataType, value);
                                }
                            }
                        }
                    }
                }
            }
            if (es != null) {
                break;
            }

        }
        return es;
    }

    static EntitlementCondition getEntitlementConditionFromPolicy(Policy policy) {
        if (policy == null) {
            return null;
        }
        List<Rule> rules = getRules(policy);
        if (rules == null) {
            return null;
        }
        EntitlementCondition ec = null;
        for (Rule rule : rules) {
            Condition condition = rule.getCondition();
            JAXBElement jaxbElement = condition.getExpression();
            if (jaxbElement.getDeclaredType().equals(Apply.class)) {
                Apply apply = (Apply)jaxbElement.getValue();
                String functionId = apply.getFunctionId();
                if (XACMLConstants.JSON_SUBJECT_AND_CONDITION_SATISFIED.equals(
                        functionId)) {
                    List<JAXBElement<?>> expressionList = apply.getExpression();
                    for (JAXBElement jaxe : expressionList) {
                        if (jaxe.getDeclaredType().equals(AttributeValue.class)) {
                            AttributeValue av = (AttributeValue)jaxe.getValue();
                            String dataType = av.getDataType();
                            if (dataType.startsWith(
                                    XACMLConstants.JSON_CONDITION_DATATYPE)) {
                                List<Object> valueList = av.getContent();
                                String value = null;
                                if (valueList != null) {
                                    for (Object ob : valueList) {
                                        if (ob instanceof String) {
                                            value = (String)ob;
                                            break;
                                        }
                                    }
                                }
                                if (value != null) {
                                    ec = createEntitlementCondition(dataType, value);
                                }
                            }
                        }
                    }
                }
                if (ec != null) {
                    break;
                }
            }

        }
        return ec;
    }

    public static PolicySet streamToPolicySet(InputStream stream) throws JAXBException {
        //FIXME: remove
        PrivilegeManager.debug.error(
            "XACMLProvilegeUtils.streamToPolicySet(), core_pkg:"
                    + XACMLConstants.XACML3_CORE_PKG, null);
        if (stream == null) {
            return null;
        }
        JAXBContext jc = JAXBContext.newInstance(
                    XACMLConstants.XACML3_CORE_PKG);
                
        Unmarshaller um = jc.createUnmarshaller();
        JAXBElement je = (JAXBElement)um.unmarshal(XMLUtils.createSAXSource(new InputSource(stream)));
        PolicySet ps = (PolicySet)je.getValue();
        return ps;
    }

    public static Set<Policy> getPoliciesFromPolicySet(PolicySet policySet) {
        if (policySet == null) {
            return null;
        }
        Set<Policy> policies = new HashSet<Policy>();
        List<JAXBElement<?>> choiceList = policySet.getPolicySetOrPolicyOrPolicySetIdReference();
        for (JAXBElement jaxe : choiceList) {
            if (jaxe.getDeclaredType().equals(Policy.class)) {
                Policy p = (Policy)jaxe.getValue();
                policies.add(p);
            }
        }
        return policies;
    }

    static EntitlementSubject createEntitlementSubject(String dataType, String value) {
        if (dataType == null || value == null) {
            return null;
        }
        EntitlementSubject es = null;
        int i = dataType.indexOf(XACMLConstants.JSON_SUBJECT_DATATYPE);
        if (i != 0) {
            return null;
        }
        String className =  null;
        if (dataType.length() 
                > XACMLConstants.JSON_SUBJECT_DATATYPE.length()) {
            className = dataType.substring(
                    XACMLConstants.JSON_SUBJECT_DATATYPE.length() + 1);
            Object ob = createDefaultObject(className);
            if (ob != null && ob instanceof EntitlementSubject) {
                es = (EntitlementSubject)ob;
            } else {
                PrivilegeManager.debug.error("XACMLPrivilegeUtils."
                        + "createEntitlementSubject()"
                        + "not an EntitlementSubject", null);
            }
        }
        if (es != null) {
            es.setState(value);
        }
        return es;
    }

    static EntitlementCondition createEntitlementCondition(String dataType, String value) {
        if (dataType == null || value == null) {
            return null;
        }
        EntitlementCondition ec = null;
        int i = dataType.indexOf(XACMLConstants.JSON_CONDITION_DATATYPE);
        if (i != 0) {
            return null;
        }
        String className =  null;
        if (dataType.length() 
                > XACMLConstants.JSON_CONDITION_DATATYPE.length()) {
            className = dataType.substring(
                    XACMLConstants.JSON_CONDITION_DATATYPE.length() + 1);
            Object ob = createDefaultObject(className);
            if (ob != null && ob instanceof EntitlementCondition) {
                ec = (EntitlementCondition)ob;
            } else {
                PrivilegeManager.debug.error("XACMLPrivilegeUtils."
                        + "createEntitlementCondition()"
                        + "not an EntitlementCondition", null);
            }
        }
        if (ec != null) {
            ec.setState(value);
        }
        return ec;
    }

    static Object createDefaultObject(String className) {
        if (className == null) {
            return null;
        }
        Object ob = null;
        try {
            Class cla = Class.forName(className.trim());
            ob =  cla.newInstance();
        } catch (ClassNotFoundException e) {
            PrivilegeManager.debug.error("XACMLPrivilegeUtils.createDefaultObject(),"
                    + "hit exception", e);
        } catch (IllegalAccessException e) {
            PrivilegeManager.debug.error("XACMLPrivilegeUtils.createDefaultObject(),"
                    + "hit exception", e);
        } catch (InstantiationException e) {
            PrivilegeManager.debug.error("XACMLPrivilegeUtils.createDefaultObject(),"
                    + "hit exception", e);
        }
        return ob;
    }

    public static Policy referralToPolicy(ReferralPrivilege privilege) throws JSONException {
        Policy policy = null;
        try {
            policy = referralToPolicyInternal(privilege);
        } catch (JAXBException je) {
            PrivilegeManager.debug.error(
                "JAXBException while mapping referral to policy:", je);
        }
        return policy;
    }

    public static Policy referralToPolicyInternal(ReferralPrivilege privilege) 
            throws JAXBException, JSONException {
 
        if (privilege == null) {
            return null;
        }

        Policy policy = new Policy();

        String privilegeName = privilege.getName();

        String policyId = privilegeNameToPolicyId(privilegeName,
                null);
        policy.setPolicyId(policyId);

        String description = privilege.getDescription();
        policy.setDescription(description);

        List<Object> vrList 
            = policy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition();

        ObjectFactory objectFactory = new ObjectFactory();
        JAXBContext jaxbContext = JAXBContext.newInstance(
                XACMLConstants.XACML3_CORE_PKG);

        VariableDefinition createdBy = new VariableDefinition();
        vrList.add(createdBy);
        createdBy.setVariableId(XACMLConstants.PRIVILEGE_CREATED_BY);
        AttributeValue cbv = new AttributeValue();
        cbv.setDataType(XACMLConstants.XS_STRING);
        cbv.getContent().add(privilege.getCreatedBy());
        JAXBElement<AttributeValue> cbve 
                = objectFactory.createAttributeValue(cbv);
        createdBy.setExpression(cbve);

        VariableDefinition lastModifiedBy = new VariableDefinition();
        vrList.add(lastModifiedBy);
        lastModifiedBy.setVariableId(XACMLConstants.PRIVILEGE_LAST_MODIFIED_BY);
        AttributeValue lmbv = new AttributeValue();
        lmbv.setDataType(XACMLConstants.XS_STRING);
        lmbv.getContent().add(privilege.getLastModifiedBy());
        JAXBElement<AttributeValue> lmbve 
                = objectFactory.createAttributeValue(lmbv);
        lastModifiedBy.setExpression(cbve);

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss.SSS");
        SimpleDateFormat sdf3 = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS");
        sdf1.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf2.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf3.setTimeZone(TimeZone.getTimeZone("GMT"));

        VariableDefinition creationDate = new VariableDefinition();
        vrList.add(creationDate); 
        creationDate.setVariableId(XACMLConstants.PRIVILEGE_CREATION_DATE);
        AttributeValue cdv = new AttributeValue();
        cdv.setDataType(XACMLConstants.XS_DATE_TIME);
        cdv.getContent().add(
                sdf1.format(privilege.getCreationDate())
                + "T"
                + sdf2.format(privilege.getCreationDate()));
        JAXBElement<AttributeValue> cdve 
                = objectFactory.createAttributeValue(cdv);
        creationDate.setExpression(cdve);

        VariableDefinition lastModifiedDate = new VariableDefinition();
        vrList.add(lastModifiedDate); 
        lastModifiedDate.setVariableId(
                XACMLConstants.PRIVILEGE_LAST_MODIFIED_DATE);
        AttributeValue lmdv = new AttributeValue();
        lmdv.setDataType(XACMLConstants.XS_DATE_TIME);
        lmdv.getContent().add(
                sdf1.format(privilege.getLastModifiedDate())
                + "T"
                + sdf2.format(privilege.getLastModifiedDate()));
        JAXBElement<AttributeValue> lmdve 
                = objectFactory.createAttributeValue(lmdv);
        lastModifiedDate.setExpression(lmdve);

        VariableDefinition isReferralPolicy = new VariableDefinition();
        vrList.add(isReferralPolicy);
        isReferralPolicy.setVariableId(
                XACMLConstants.IS_REFERRAL_POLICY);
        AttributeValue irdv = new AttributeValue();
        irdv.setDataType(XACMLConstants.XS_BOOLEAN_TYPE);
        irdv.getContent().add(XACMLConstants.XS_BOOLEAN_TRUE);
        JAXBElement<AttributeValue> irdve
                = objectFactory.createAttributeValue(irdv);
        isReferralPolicy.setExpression(irdve);

        // PolicyIssuer policyIssuer = null;  // optional, TODO

        Version version = new Version();

        // TODO: use privilege version in future
        version.setValue(sdf3.format(privilege.getLastModifiedDate())); 
        policy.setVersion(version);

        // Defaults policyDefaults = null; // optional, TODO

        policy.setRuleCombiningAlgId(XACMLConstants.XACML_RULE_DENY_OVERRIDES);

        // XACML Target contains a  list of AnyOf(s)
        // XACML AnyOf contains a list of AllOf(s)
        // XACML AllOf contains a list of Match(s)

        Target target = new Target();
        policy.setTarget(target);

        List<AnyOf> targetAnyOfList = target.getAnyOf();

        Set<String> realms = privilege.getRealms();
        Map<String, Set<String>> appsResources
                = privilege.getOriginalMapApplNameToResources();
        

        AnyOf anyOfRealmsAppsResources
                = realmsAppsResourcesToAnyOf(realms, appsResources);
        if (anyOfRealmsAppsResources != null) {
            targetAnyOfList.add(anyOfRealmsAppsResources);
        }

        Rule permitRule = new Rule();
        vrList.add(permitRule);
        permitRule.setRuleId(privilegeName + ":"
                + XACMLConstants.PREMIT_RULE_SUFFIX);
        permitRule.setDescription(XACMLConstants.PERMIT_RULE_DESCRIPTION);
        permitRule.setEffect(EffectType.PERMIT);
        Target permitTarget = new Target();
        permitRule.setTarget(permitTarget);

        return policy;

    }

    public static boolean isReferralPolicy(Policy policy) {
        String s = getVariableById(policy, XACMLConstants.IS_REFERRAL_POLICY);
        return XACMLConstants.XS_BOOLEAN_TRUE.equalsIgnoreCase(s);
    }

    public static ReferralPrivilege policyToReferral(Policy policy)
            throws EntitlementException, JSONException {
        String policyId = policy.getPolicyId();
        String privilegeName = policyIdToPrivilegeName(policyId);
        String description = policy.getDescription();

        String createdBy = getVariableById(policy,
                XACMLConstants.PRIVILEGE_CREATED_BY);

        long createdAt = dateStringToLong(getVariableById(policy,
                XACMLConstants.PRIVILEGE_CREATION_DATE));

        String lastModifiedBy = getVariableById(policy,
                XACMLConstants.PRIVILEGE_LAST_MODIFIED_BY);

        long lastModifiedAt = dateStringToLong(getVariableById(policy,
                XACMLConstants.PRIVILEGE_LAST_MODIFIED_DATE));

        List<Match> policyMatches = getAllMatchesFromTarget(policy.getTarget());
        JSONObject jo = getRealmsAppsResources(policyMatches);

        Set<String> realms = JSONUtils.getSet(jo, "realms");
        Map<String, Set<String>> appsResources
                = JSONUtils.getMapStringSetString(jo, "appsResources");
        ReferralPrivilege  referral = new ReferralPrivilege(privilegeName,
                appsResources, realms);
        referral.setCreatedBy(createdBy);
        referral.setCreationDate(createdAt);
        referral.setLastModifiedBy(lastModifiedBy);
        referral.setLastModifiedDate(lastModifiedAt);
 
        return referral;
    }

    public static AnyOf realmsAppsResourcesToAnyOf(Set<String> realms,
            Map<String, Set<String>> appsResources) throws JSONException {
        AnyOf anyOf = new AnyOf();
        List<AllOf> allOfList = anyOf.getAllOf();
        AllOf allOf = new AllOf();
        allOfList.add(allOf);
        List<Match> matchList = allOf.getMatch();
        Match match = new Match();
        matchList.add(match);
        match.setMatchId(XACMLConstants.JSON_REALMS_APPS_RESOURCES_MATCH); //FIXME

        AttributeValue attributeValue = new AttributeValue();
        String dataType = XACMLConstants.JSON_REALMS_APPS_RESOURCES_DATATYPE; //FIXME
        attributeValue.setDataType(dataType);
        JSONObject jo = new JSONObject();
        jo.put("realms", realms);
        jo.put("appsResources", appsResources);
        attributeValue.getContent().add(jo.toString());

        AttributeDesignator attributeDesignator = new AttributeDesignator();
        String category = XACMLConstants.REALMS_APPS_RESOURCES_CATEGORY; 
        attributeDesignator.setCategory(category);
        String attributeId = XACMLConstants.JSON_REALMS_APPS_RESOURCES_ID; 
        attributeDesignator.setAttributeId(attributeId);
        attributeDesignator.setDataType(dataType);
        boolean mustBePresent = false;
        attributeDesignator.setMustBePresent(mustBePresent);

        match.setAttributeValue(attributeValue);
        match.setAttributeDesignator(attributeDesignator);

        return anyOf;
    }

}

