/* The contents of this file are subject to the terms
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
 * $Id: CardSpaceConstants.java,v 1.2 2008/03/31 05:25:07 mrudul_uchil Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.cardfactory;

import org.xmldap.infocard.policy.SupportedClaim;

public class CardSpaceConstants {
    public static String WSDL_NS_URI = "http://schemas.xmlsoap.org/wsdl/";
    public static String WS_ADRESSING_08_2005_NS_URI = "http://www.w3.org/2005/08/addressing";
    public static String WS_TRUST_02_2005_NS_URI = "http://schemas.xmlsoap.org/ws/2005/02/trust";
    public static String WS_ADDRESSING_IDENTITY_02_2006_NS_URI = "http://schemas.xmlsoap.org/ws/2006/02/addressingidentity";
    public static String WS_ADRESSING_WSDL_05_2006_NS_URI = "http://www.w3.org/2006/05/addressing/wsdl";
    public static String WS_POLICY_09_2004_NS_URI = "http://schemas.xmlsoap.org/ws/2004/09/policy";
    public static String WS_SECURITYPOLICY_07_2005_NS_URI = "http://schemas.xmlsoap.org/ws/2005/07/securitypolicy";
    public static String WSSEC_UTILITY_01_2004_NS_URI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    public static String SOAP_12_NS_URI = "http://schemas.xmlsoap.org/wsdl/soap12/";
    public static String XML_DSIG_NS_URI = "http://www.w3.org/2000/09/xmldsig#";
    public static String INFOCARD_05_2005_NS_URI = "http://schemas.xmlsoap.org/ws/2005/05/identity";
    public static String supportedClaimsClass = "com.sun.identity.cardfactory.supportedClaimsClass";
    public static String defaultSupportedClaimsClass = "org.xmldap.sts.db.DbSupportedClaims";

    public final static String INFOCARD_NAMESPACE = "http://schemas.xmlsoap.org/ws/2005/05/identity";
    public final static String INFOCARD_PREFIX = "ic";
    public static final String IC_NAMESPACE_PREFIX = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/";
    public static final String IC_NAMESPACE = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims";

    public static final String IC_GIVENNAME = "givenname";
    public static final String IC_SURNAME = "surname";
    public static final String IC_EMAILADDRESS = "emailaddress";
    public static final String IC_PRIVATEPERSONALIDENTIFIER = "privatepersonalidentifier"; 

    public static final String IC_NS_GIVENNAME = IC_NAMESPACE_PREFIX + IC_GIVENNAME;
    public static final String IC_NS_SURNAME = IC_NAMESPACE_PREFIX + IC_SURNAME;
    public static final String IC_NS_EMAILADDRESS = IC_NAMESPACE_PREFIX + IC_EMAILADDRESS;
    public static final String IC_NS_PRIVATEPERSONALIDENTIFIER = IC_NAMESPACE_PREFIX + IC_PRIVATEPERSONALIDENTIFIER;

    public static final String IC_STREETADDRESS = "streetaddress";
    public static final String IC_NS_STREETADDRESS = IC_NAMESPACE_PREFIX + IC_STREETADDRESS;    

    public static final String IC_CITY = "locality";
    public static final String IC_NS_CITY = IC_NAMESPACE_PREFIX + IC_CITY;    

    public static final String IC_STATE = "stateorprovince";
    public static final String IC_NS_STATE = IC_NAMESPACE_PREFIX + IC_STATE;    

    public static final String IC_POSTALCODE = "postalcode";
    public static final String IC_NS_POSTALCODE = IC_NAMESPACE_PREFIX + IC_POSTALCODE;    

    public static final String IC_COUNTRY = "country";
    public static final String IC_NS_COUNRTY = IC_NAMESPACE_PREFIX + IC_COUNTRY;    

    public static final String IC_HOMEPHONE = "homephone";
    public static final String IC_NS_HOMEPHONE = IC_NAMESPACE_PREFIX + IC_HOMEPHONE;    

    public static final String IC_WORKPHONE = "otherphone";
    public static final String IC_NS_WORKPHONE = IC_NAMESPACE_PREFIX + IC_WORKPHONE;    

    public static final String IC_MOBILEPHONE = "mobilephone";
    public static final String IC_NS_MOBILEPHONE = IC_NAMESPACE_PREFIX + IC_MOBILEPHONE;    

    public static final String IC_GENDER = "gender";
    public static final String IC_NS_GENDER = IC_NAMESPACE_PREFIX + IC_GENDER;    

    public static final String IC_DOB = "dateofbirth";
    public static final String IC_NS_DOB = IC_NAMESPACE_PREFIX + IC_DOB;    

    public static final String IC_WEBPAGE = "webpage";
    public static final String IC_NS_WEBPAGE = IC_NAMESPACE_PREFIX + IC_WEBPAGE;

    public enum ClaimType {

        GivenName("Given Name", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname", "Given or First Name"),
        LastName("Last Name", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname", "Last or Surname"),
        EmailAddress("Email Address", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress", "Email Address"),
        StreetAddress("Street Address", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/streetaddress", "Street Address"),
        City("City", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/locality", "City or Locality"),
        State("State", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/stateorprovince", "State or Province"),
        PostalCode("Postal Code", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/postalcode", "Zip or Postal Code"),
        Country("Country", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/country", "Country"),
        HomePhone("Home Phone", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/homephone", "Home or Primary Phone Number"),
        WorkPhone("Work Phone", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/otherphone", "Work or Secondary Phone Number"),
        MobilePhone("Mobile Phone", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mobilephone", "Mobile Phone Number"),
        DateOfBirth("Date of Birth", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/dateofbirth", "Date of Birth"),
        Gender("Gender", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/gender", "Gender"),
        PPID("PPID", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/privatepersonalidentifier", "Private Personal Identifier"),
        WebPage("Web Page", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/webpage", "Web Page");
        private final String displayTag;
        private final String uri;
        private final String description;

        ClaimType(String displayTag, String uri, String description) {
            this.displayTag = displayTag;
            this.uri = uri;
            this.description = description;
        }

        public String displayTag() {
            return displayTag;
        }

        public String uri() {
            return uri;
        }

        public String description() {
            return description;
        }

        public SupportedClaim getSupportedClaim() {
            return new SupportedClaim(displayTag(), uri(), description());
        }

        public static ClaimType claimTypeFromUri(String uri) {
            if (uri.equals(ClaimType.GivenName.uri())) {
                return ClaimType.GivenName;
            } else if (uri.equals(ClaimType.LastName.uri())) {
                return ClaimType.LastName;
            } else if (uri.equals(ClaimType.EmailAddress.uri())) {
                return ClaimType.EmailAddress;
            } else if (uri.equals(ClaimType.StreetAddress.uri())) {
                return ClaimType.StreetAddress;
            } else if (uri.equals(ClaimType.City.uri())) {
                return ClaimType.City;
            } else if (uri.equals(ClaimType.State.uri())) {
                return ClaimType.State;
            } else if (uri.equals(ClaimType.PostalCode.uri())) {
                return ClaimType.PostalCode;
            } else if (uri.equals(ClaimType.Country.uri())) {
                return ClaimType.Country;
            } else if (uri.equals(ClaimType.HomePhone.uri())) {
                return ClaimType.HomePhone;
            } else if (uri.equals(ClaimType.WorkPhone.uri())) {
                return ClaimType.WorkPhone;
            } else if (uri.equals(ClaimType.MobilePhone.uri())) {
                return ClaimType.MobilePhone;
            } else if (uri.equals(ClaimType.Gender.uri())) {
                return ClaimType.Gender;
            } else if (uri.equals(ClaimType.DateOfBirth.uri())) {
                return ClaimType.DateOfBirth;
            } else if (uri.equals(ClaimType.WebPage.uri())) {
                return ClaimType.WebPage;
            } else if (uri.equals(ClaimType.PPID.uri())) {
                return ClaimType.PPID; 
            } else {
                return null; 
            }
        }
    }
}
