/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

/**
 * The classes in this package provide context to help set the various configuration options in the CXF STS client
 * necessary to consume sts instances published via OpenAM. They are intended as examples to help guide the development
 * of sts clients.
 *
 * Note that the classes in this module augment those in the openam-client-sts module. The classes in the openam-client-sts
 * module are included in the OpenAM client SDK. The classes in the current module cannot be placed in the openam-client-sts
 * module, as that would pollute the OpenAM .war file with cxf-related dependencies. Likewise, the soap-sts-related
 * classes in the openam-client-sts module cannot be placed in this current module, as they are needed by e.g. the
 * openam-publish-sts module, and the openam-rest-sts module (some domain objects, like SAML2Config, are shared between
 * the soap and rest sts), and thus would have to be referenced by these modules, which would again pollute the OpenAM
 * .war file with cxf-related dependencies. The bottom line is that the openam-soap-sts module is largely stand-alone -
 * the .war file in the openam-soap-sts-server is deployed distinct from the OpenAM .war file, and the classes in the
 * openam-soap-sts-client module are stand-alone examples helpful in configuring the cxf-sts-client to successfully consume
 * published soap-sts instances. Thus the cxf-dependencies in the openam-soap-sts sub-modules won't pollute the OpenAM
 * .war file, and the openam-soap-sts can deploy any cxf version without fear of side-effects. This does mean, however,
 * that clients who wish to programmatically publish soap-sts instances, and then consume these instances, must
 * have both the OpenAM client-sdk, and the .jar file produced by the openam-soap-sts-client module, in their classpath
 * (in addition to the cxf-related dependencies defined in the openam-soap-sts-client pom file).
 */
package org.forgerock.openam.sts.soap;