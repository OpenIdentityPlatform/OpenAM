/**
 *
 ~ DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 ~
 ~ Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
 ~
 ~ The contents of this file are subject to the terms
 ~ of the Common Development and Distribution License
 ~ (the License). You may not use this file except in
 ~ compliance with the License.
 ~
 ~ You can obtain a copy of the License at
 ~ http://forgerock.org/license/CDDLv1.0.html
 ~ See the License for the specific language governing
 ~ permission and limitations under the License.
 ~
 ~ When distributing Covered Code, include this CDDL
 ~ Header Notice in each file and include the License file
 ~ at http://forgerock.org/license/CDDLv1.0.html
 ~ If applicable, add the following below the CDDL Header,
 ~ with the fields enclosed by brackets [] replaced by
 ~ your own identifying information:
 ~ "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package org.forgerock.identity.openam.xacml.v3.Entitlements;

public class DataType {
    public static String XACMLSTRING = "http://www.w3.org/2001/XMLSchema#string";
    public static String XACMLBOOLEAN = "http://www.w3.org/2001/XMLSchema#boolean";
    public static String XACMLINTEGER = "http://www.w3.org/2001/XMLSchema#integer";
    public static String XACMLDOUBLE = "http://www.w3.org/2001/XMLSchema#double";
    public static String XACMLTIME = "http://www.w3.org/2001/XMLSchema#time";
    public static String XACMLDATE = "http://www.w3.org/2001/XMLSchema#date";
    public static String XACMLDATETIME = "http://www.w3.org/2001/XMLSchema#dateTime";
    public static String XACMLANYURI = "http://www.w3.org/2001/XMLSchema#anyURI";
    public static String XACMLHEXBINARY = "http://www.w3.org/2001/XMLSchema#hexBinary";
    public static String XACMLBASE64BINARY = "http://www.w3.org/2001/XMLSchema#base64Binary";
    public static String XACMLDAYTIMEDURATION = "http://www.w3.org/2001/XMLSchema#dayTimeDuration";
    public static String XACMLYEARMONTHDURATION = "http://www.w3.org/2001/XMLSchema#yearMonthDuration";
    public static String XACMLX500NAME = "urn:oasis:names:tc:xacml:1.0:data-type:x500Name";
    public static String XACMLRFC822NAME = "urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name";
    public static String XACMLIPADDRESS = "urn:oasis:names:tc:xacml:2.0:data-type:ipAddress";
    public static String XACMLDNSNAME = "urn:oasis:names:tc:xacml:2.0:data-type:dnsName";
    public static String XACMLXPATHEXPRESSION = "urn:oasis:names:tc:xacml:3.0:data-type:xpathExpression";
}
