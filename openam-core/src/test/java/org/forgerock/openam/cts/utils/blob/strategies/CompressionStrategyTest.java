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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS.
 */
package org.forgerock.openam.cts.utils.blob.strategies;

import org.forgerock.openam.cts.utils.blob.TokenStrategyFailedException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.fest.assertions.Assertions.assertThat;

public class CompressionStrategyTest {
    private CompressionStrategy compression;
    private byte[] data;

    private static final String JSON_SAMPLE = "{\"clientDomain\":\"dc=openam,dc=forgerock,dc=org\",\"" +
            "clientID\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"cookieMode\":null,\"" +
            "cookieStr\":null,\"creationTime\":1375353841,\"isISStored\":true,\"latestAccessTime\":" +
            "1375353841,\"maxCachingTime\":3,\"maxIdleTime\":30,\"maxSessionTime\":120,\"" +
            "reschedulePossible\":false,\"restrictedTokensByRestriction\":{},\"restrictedTokensBySid\":" +
            "{},\"sessionEventURLs\":{\"http://rwapshott.forgerock.com:8080/openam/notificationservice" +
            "\":[{\"comingFromAuth\":false,\"cookieMode\":null,\"encryptedString\":\"" +
            "AQIC5wM2LY4SfcxjU9TuISV5pcZVBhh8fA2kRtHPX065uzE.*AAJTSQACMDIAAlNLABM4NjE3NjM5MTc2NTIyMzc" +
            "3Mzg1AAJTMQACMDE.*\",\"extensionPart\":null,\"extensions\":{},\"isParsed\":false,\"session" +
            "Domain\":\"\",\"sessionServer\":\"\",\"sessionServerID\":\"\",\"sessionServerPort\":\"\",\"s" +
            "essionServerProtocol\":\"\",\"sessionServerURI\":\"\",\"tail\":null}]},\"sessionHandle\":\"" +
            "shandle:AQIC5wM2LY4Sfcx3QShvJQovWxXLo4HeN8INGNzJ0ObVPs0.*AAJTSQACMDIAAlMxAAIwMQACU0sAEzg2MT" +
            "c2MzkxNzY1MjIzNzczODU.*\",\"sessionID\":{\"comingFromAuth\":false,\"cookieMode\":null,\"enc" +
            "ryptedString\":\"AQIC5wM2LY4SfcxjU9TuISV5pcZVBhh8fA2kRtHPX065uzE.*AAJTSQACMDIAAlNLABM4NjE3Nj" +
            "M5MTc2NTIyMzc3Mzg1AAJTMQACMDE.*\",\"extensionPart\":\"AAJTSQACMDIAAlNLABM4NjE3NjM5MTc2NTIyMz" +
            "c3Mzg1AAJTMQACMDE=\",\"extensions\":{\"SI\":\"02\",\"S1\":\"01\",\"SK\":\"86176391765223773" +
            "85\"},\"isParsed\":true,\"sessionDomain\":\"dc=openam,dc=forgerock,dc=org\",\"sessionServer" +
            "\":\"rwapshott.forgerock.com\",\"sessionServerID\":\"02\",\"sessionServerPort\":\"8080\",\"s" +
            "essionServerProtocol\":\"http\",\"sessionServerURI\":\"/openam\",\"tail\":\"\"},\"sessionPro" +
            "perties\":{\"CharSet\":\"UTF-8\",\"UserId\":\"amadmin\",\"FullLoginURL\":\"/openam/UI/Logi" +
            "n\",\"successURL\":\"/openam/console\",\"cookieSupport\":\"true\",\"AuthLevel\":\"0\",\"Sessi" +
            "onHandle\":\"shandle:AQIC5wM2LY4Sfcyn6TUnRk0cPYbywbMa5eHp3KodJFMuh08.*AAJTSQACMDIAAlMxAAIwMQA" +
            "CU0sAFC0yOTc3NjI0NjQ4NDYyODA4NDk0*\",\"UserToken\":\"amadmin\",\"loginURL\":\"/openam/UI/Logi" +
            "n\",\"Principals\":\"amadmin\",\"Service\":\"ldapService\",\"sun.am.UniversalIdentifier\":\"i" +
            "d=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"amlbcookie\":\"01\",\"Organization\":\"dc" +
            "=openam,dc=forgerock,dc=org\",\"Locale\":\"en_US\",\"HostName\":\"172.16.100.130\",\"AuthType" +
            "\":\"DataStore\",\"Host\":\"172.16.100.130\",\"UserProfile\":\"Required\",\"clientType\":\"ge" +
            "nericHTML\",\"AMCtxId\":\"70cb377418240f5601\",\"authInstant\":\"2013-08-01T10:44:01Z\",\"Pri" +
            "ncipal\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\"},\"sessionState\":1,\"sessionTyp" +
            "e\":0,\"timedOutAt\":0,\"uuid\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"version" +
            "\":0,\"willExpireFlag\":true}";

    @BeforeMethod
    public void setUp() throws Exception {
        data = JSON_SAMPLE.getBytes();
        compression = new CompressionStrategy();
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectIfNullBlobOnPerform() throws TokenStrategyFailedException {
        compression.perform(null);
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void shouldRejectIfNullBlobOnReverse() throws TokenStrategyFailedException {
        compression.reverse(null);
    }

    @Test
    public void shouldCompressContents() throws TokenStrategyFailedException {
        assertThat(compression.perform(data).length).isLessThan(data.length);
    }

    @Test
    public void shouldDecompressCompressedContents() throws TokenStrategyFailedException {
        assertThat(compression.reverse(compression.perform(data))).isEqualTo(data);
    }
}
