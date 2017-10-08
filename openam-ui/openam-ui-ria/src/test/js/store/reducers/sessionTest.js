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
    "store/actions/types",
    "store/reducers/session"
], (types, reducer) => {
    reducer = reducer.default;

    describe("store/reducers/session", () => {
        it("returns the initial state", () => {
            expect(
                reducer(undefined, {})
            ).eql({
                realm: undefined,
                sessionHandle: undefined
            });
        });

        it(`handles ${types.SESSION_ADD_INFO}`, () => {
            const realm = "/realmA";
            const sessionHandle = "sessionHandle";

            expect(
                reducer({}, {
                    type: types.SESSION_ADD_INFO,
                    realm,
                    sessionHandle
                })
            ).eql({
                realm: realm.toLowerCase(),
                sessionHandle
            });
        });

        it(`handles ${types.SESSION_REMOVE_INFO}`, () => {
            expect(
                reducer({}, {
                    type: types.SESSION_REMOVE_INFO
                })
            ).eql({});
        });
    });
});
