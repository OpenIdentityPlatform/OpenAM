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
 * $Id: ECPUtils.java,v 1.2 2008/03/17 03:11:05 hengming Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

public class ECPUtils {

    public static final String ACTOR =
        "http://schemas.xmlsoap.org/soap/actor/next";
    public static final String ECPNAMESPACE = "urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp";
    public static final String PAOSNAMESPACE = "urn:liberty:paos:2003-08";

    public static String PAOS_MIME_TYPE_VAL = "application/vnd.paos+xml";
    public static String PAOS_HEADER_TYPE = "PAOS";
    public static String PAOS_HEADER_VAL = "ver='urn:liberty:paos:2003-08'; 'urn:oasis:names:tc:SAML:2.0:profiles:SSO:ecp'";

    public static String COOKIE_NAME = "iPlanetDirectoryPro";
}

