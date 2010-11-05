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
 * $Id: XACMLConstants.java,v 1.5 2009/09/22 23:00:15 madan_ranganath Exp $
 *
 */

package com.sun.identity.xacml.common;

/**
 * This interface  defines constants common to all XACML elements.
 *
 * @supported.all.api
 */
public class  XACMLConstants {
      
    /**
     * Constant for SAML namespace URI 
     */
    public static String SAML_NS_URI
            = "urn:oasis:names:tc:SAML:2.0:assertion";

    /**
     * Constant for SAML namespace prefix
     */
    public static String SAML_NS_PREFIX = "saml:";

    /**
     * Constant for SAML namespace declaration URI
     */
    public static String SAML_NS_DECLARATION 
            = " xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ";

    /**
     * Constant for SAML2 protocol namespace URI
     */
    public static String SAMLP_NS_URI =
        "urn:oasis:names:tc:SAML:2.0:protocol";
    
    /**
     * Constant for SAML2 Protocol namespace prefix
     */
    public static String SAMLP_NS_PREFIX = "samlp:";
    
    /**
     * Constant for SAML2 protocol namespace declaration
     */
    public static String SAMLP_NS_DECLARATION =
        " xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"";
    
    /**
     * Constant for xacml-saml namespace  URI
     */
    public final static String XACML_SAML_NS_URI
            = "urn:oasis:names:tc:xacml:2.0:saml:assertion:schema:os";
    /**
     * Constant for xacml-saml namespace prefix
     */
    public final static String XACML_SAML_NS_PREFIX
            = " xacml-saml:";

    /**
     * Constant for xacml-saml namespace declaration
     */
    public final static String XACML_SAML_NS_DECLARATION
            = " xmlns:xacml-saml=\"urn:oasis:names:tc:xacml:2.0:saml:assertion:schema:os\" ";

    /**
     * Constant for XACML SAML2 protocol namespace URI
     */
    public static String XACML_SAMLP_NS_URI =
        "urn:oasis:xacml:2.0:saml:protocol:schema:os";

    /**
     * Constant for XACML SAML2 Protocol namespace prefix.
     */
    public static String XACML_SAMLP_NS_PREFIX = "xacml-samlp:";
    
    /**
     * Constant for XACML SAML2 protocol namespace declaration
     */
    public static String XACML_SAMLP_NS_DECLARATION =
        " xmlns:xacml-samlp=\"urn:oasis:xacml:2.0:saml:protocol:schema:os\" ";

    /**
     * Constant for XACML policy namespace URI
     */
    public static String XACML_NS_URI =
        "urn:oasis:names:tc:xacml:2.0:policy:schema:os";

    /**
     * Constant for XACML policy namespace prefix
     */
    public static String XACML_NS_PREFIX = "xacml";

    /**
     * Constant for XACML policy namespace declaration
     */
    public static String XACML_NS_DECLARATION =
        " xmlns:xacml=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\" ";

    /**
     * Constant for XACML context namespace URI
     */
    public static String CONTEXT_NS_URI =
    "urn:oasis:names:tc:xacml:2.0:context:schema:os";
    
    /**
     * Constant for XACML context namespace prefix
     */
    public static String CONTEXT_NS_PREFIX = "xacml-context";

    /**
     * Constant for XACML context namespace declaration
     */
    public static String CONTEXT_NS_DECLARATION =
    " xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\" ";
    
    /*
     * Constant for XMLSchema-instance URI
     */
    public static String XSI_NS_URI = 
        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"";
    
    /**
     * Constant for xsi name space delcaration
     */
    public final static String XSI_NS_DECLARATION
            = " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ";

    /**
     * Constant for xsi:type="xacml-samlp:XACMLAuthzDecisionQuery
     */
    public final static String XSI_TYPE_XACML_AUTHZ_DECISION_QUERY
            = " xsi:type=\"xacml-samlp:XACMLAuthzDecisionQuery\"";

    /**
     * XACML context schema location 
     */
    public static String CONTEXT_SCHEMA_LOCATION=
    "xsi:schemaLocation=\"urn:oasis:names:tc:xacml:2.0:context:schema:os http:"
        +"//docs.oasis-open.org/xacml/access_control-xacml"
        +"-2.0-context-schema-os.xsd\"";
        
    /**
     * Constant for RequestAbstract
     */
    public static String REQUEST_ABSTRACT =
        "RequestAbstract";

    /**
     * Constant for XACMLAuthzDecisionQuery 
     */
    public static String XACML_AUTHZ_DECISION_QUERY =
            "XACMLAuthzDecisionQuery";

    /**
     * Constant for xsi:type
     */
    public final static String XSI_TYPE_XACML_AUTHZ_DECISION_STATEMENT
            = " xsi:type=\"xacml-saml:XACMLAuthzDecisionStatement\"";

    /**
     * Constant for Request 
     */
    public  static String REQUEST = "Request";

    /**
     * Constant for Subject 
     */
    public static String SUBJECT = "Subject";

    /**
     * Constant for SubjectCategory element
     */
    public static  String SUBJECT_CATEGORY = "SubjectCategory";

    /**
     * Constant for AttributeValue element
     */
      
    /**
     * Constant for Resource element
     */
    public static String RESOURCE = "Resource";
    
    /**
     * Constant for ResourceContent element
     */
    public static String RESOURCE_CONTENT = "ResourceContent";

    /**
     * Constant for Action element
     */
    public static String ACTION = "Action";
    
     
    /**
     * Constant for Environment element
     */
    public static String ENVIRONMENT = "Environment";
      
    /**
     * Constant for Attribute element
     */
    public static String ATTRIBUTE = "Attribute";

    /**
     * Constant for AttributeId element
     */
    public static  String ATTRIBUTE_ID ="AttributeId";
  
    /**
     * Constant for DataType element
     */
    public  static  String DATATYPE ="DataType";
    
    /**
     * Constant for Issuer element
     */
    public  static  String ISSUER ="Issuer";
 
    /**
     * Constant for AttributeValue element
     */
    public static String ATTRIBUTE_VALUE = "AttributeValue";

    /**
     * Constant for SAML Statement 
     */
    public final static String SAML_STATEMENT
            = "Statement";

    /*
     * Constant for XACMLAuthzDecisionStatementElement
     */
    public final static String XACML_AUTHZ_DECISION_STATEMENT
            = "XACMLAuthzDecisionStatement";

   /**
     * Constant for InputContextOnly attribute
     */
    public  static  String INPUT_CONTEXT_ONLY ="InputContextOnly";

    /**
     * Constant for ReturnContext attribute
     */
    public  static  String RETURN_CONTEXT ="ReturnContext";

    /**
     * Constant for Response element
     */
    public static final String RESPONSE = "Response";

    /**
     * Constant for Result element
     */
    public static final String RESULT = "Result";

    /**
     * Constant for ResourceId attribute
     */
    public static final String RESOURCE_ID = "ResourceId";

    /**
     * Constant for Decision element
     */
    public static final String DECISION = "Decision";

    /**
     * Constant for Status element
     */
    public static final String STATUS = "Status";

    /**
     * Constant for StatusCode element
     */
    public static final String STATUS_CODE = "StatusCode";

    /**
     * Constant for Value attribute
     */
    public static final String VALUE = "Value";

    /**
     * Constant for StatusMessage element
     */
    public static final String STATUS_MESSAGE = "StatusMessage";

    /**
     * Constant for StatusDetail element
     */
    public final static String STATUS_DETAIL = "StatusDetail";

    /**
     * Constant for Permit
     */
    public static final String PERMIT = "Permit";

    /**
     * Constant for Deny
     */
    public static final String DENY = "Deny";

    /**
     * Constant for Indeterminate
     */
    public static final String INDETERMINATE = "Indeterminate";

    /**
     * Constant for NotApplicable
     */
    public static final String NOT_APPLICABLE = "NotApplicable";

    /**
     * Constant for Obligations
     */
    public  static  String OBLIGATIONS = "Obligations";

    /**
     * Constant for Obligation
     */
    public  static  String OBLIGATION = "Obligation";

    /**
     * Constant for ObligationId
     */
    public  static  String OBLIGATION_ID = "ObligationId";

    /**
     * Constant for AttributeAssignment
     */
    public  static  String ATTRIBUTE_ASSIGNMENT = "AttributeAssignment";

    /**
     * Constant for FulfillOn
     */
    public  static  String FULFILL_ON = "FulfillOn";

    /**
     * Constant for DataType
     */
    public  static  String DATA_TYPE = "DataType";

    /**
     * Constant for EffectType
     */
    public  static  String EFFECT_TYPE = "EffectType";
      
    /**
     * Constant for access-subject URI
     */
    public static String ACCESS_SUBJECT =
        "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";

    /**
     * Constant for intemediray-subject URI
     */
    public static String INTERMEDIARY_SUBJECT =
        "urn:oasis:names:tc:xacml:1.0:subject-category:intermediary-subject";

   /**
    * Constant for subject-id URI
    */
   public static String SUBJECT_ID =
      "urn:oasis:names:tc:xacml:1.0:subject:subject-id";

   /**
    * Constant for resource-id URI
    */
   public static String RESOURCE_ID_URI =
      "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

   /**
    * Constant for action-id URI
    */
   public static String ACTION_ID =
      "urn:oasis:names:tc:xacml:1.0:action:action-id";

   /**
    * Constant for opensso-session-id URI
    */
   public static String OPENSSO_SESSION_ID =
      "urn:sun:names:xacml:2.0:data-type:opensso-session-id";

   /**
    * Constant for saml2-nameid URI
    */
   public static String SAML2_NAMEID =
      "urn:sun:names:xacml:2.0:data-type:saml2-nameid";

   /**
    * Constant for resource:target-service URI
    */
   public static String TARGET_SERVICE =
      "urn:sun:names:xacml:2.0:resource:target-service";

   /**
    * Constant for x500name URI
    */
   public static String X500NAME =
      "urn:oasis:names:tc:xacml:1.0:data-type:x500Name";

   /**
    * Constant for XMLSchema#String URI
    */
   public static String XS_STRING =
      "http://www.w3.org/2001/XMLSchema#string";

    /**
     * Constant key for XACML SDK class mapping
     */
    public static  String SDK_CLASS_MAPPING = 
        "com.sun.identity.xacml.sdk.mapping.";

    /**
     * Constant for subject-category URI
     */
    public static String SUBJECT_CATEGORY_ID =
        "urn:oasis:names:tc:xacml:1.0:subject-category";

    /**
     * Constant for status code: ok URI
     */
    public static final String STATUS_CODE_OK 
            = "urn:oasis:names:tc:xacml:1.0:status:ok";

    /**
     * Constant for status code: missing-attribute URI
     */
    public static final String STATUS_CODE_MISSING_ATTRIBUTE 
            = "urn:oasis:names:tc:xacml:1.0:status:missing-attribute";
    
    /**
     * Constant for status code: syntax-error URI
     */
    public static final String STATUS_CODE_SYNTAX_ERROR 
            = "urn:oasis:names:tc:xacml:1.0:status:syntax-error";

    /**
     * Constant for status code: processing-error URI
     */
    public static final String STATUS_CODE_PROCESSING_ERROR 
            = "urn:oasis:names:tc:xacml:1.0:status:processing-error";

    /**
     * Constant for space
     */
    public static  String SPACE= " ";

}
