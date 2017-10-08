/**
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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "store/actions/creators",
    "store/actions/types"
], (creators, types) => {
    describe("store/actions/creators", () => {
        describe("#sessionAddInfo", () => {
            it("creates an action to add session info to store.session", () => {
                const realm = "/realmA";
                const sessionHandle = "sessionHandle";

                expect(creators.sessionAddInfo({ realm, sessionHandle })).eql({
                    type: types.SESSION_ADD_INFO,
                    realm,
                    sessionHandle
                });
            });
        });
        describe("#sessionRemoveInfo", () => {
            it("creates an action to remove session info from store.session", () => {
                expect(creators.sessionRemoveInfo()).eql({
                    type: types.SESSION_REMOVE_INFO
                });
            });
        });
        describe("#serverAddRealm", () => {
            it("creates an action to add realm to server.session", () => {
                const realm = "/realmA";

                expect(creators.serverAddRealm(realm)).eql({
                    type: types.SERVER_ADD_REALM,
                    realm
                });
            });
        });
    });
});
