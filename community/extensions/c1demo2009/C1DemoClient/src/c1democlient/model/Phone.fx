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
 * $Id: Phone.fx,v 1.2 2009/06/11 05:29:45 superpat7 Exp $
 */

package c1democlient.model;

import c1democlient.Constants;

public class Phone {
    public var phoneNumber: String;
    public var accountNumber: String;
    public var userName: String;
    public var headOfHousehold: Boolean;
    public var allocatedMinutes: String;
    public var canDownloadRingtones: Boolean;
    public var canDownloadMusic: Boolean;
    public var canDownloadVideo: Boolean;

    public function marshall(): String {
        return "<phone uri=\"http://{Constants.host}:{Constants.port}/{Constants.contextRoot}/resources/phones/{phoneNumber}/\">"
            "<allocatedMinutes>{allocatedMinutes}</allocatedMinutes>"
            "<canDownloadMusic>{canDownloadMusic}</canDownloadMusic>"
            "<canDownloadRingtones>{canDownloadRingtones}</canDownloadRingtones>"
            "<canDownloadVideo>{canDownloadVideo}</canDownloadVideo>"
            "<headOfHousehold>{headOfHousehold}</headOfHousehold>"
            "<phoneNumber>{phoneNumber}</phoneNumber>"
            "<userName>{userName}</userName>"
        "</phone>";
    }
}
