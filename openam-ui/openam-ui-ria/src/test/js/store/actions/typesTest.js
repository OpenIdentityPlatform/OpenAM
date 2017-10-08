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
    "store/actions/types"
], (types) => {
    describe("store/actions/types", () => {
        describe(".SESSION_ADD_INFO", () => {
            const expected = "session/ADD_INFO";
            it(`is "${expected}"`, () => {
                expect(types.SESSION_ADD_INFO).equal(expected);
            });
        });
        describe(".SESSION_REMOVE_INFO", () => {
            const expected = "session/REMOVE_INFO";
            it(`is "${expected}"`, () => {
                expect(types.SESSION_REMOVE_INFO).equal(expected);
            });
        });
        describe(".SERVER_ADD_REALM", () => {
            const expected = "server/ADD_REALM";
            it(`is "${expected}"`, () => {
                expect(types.SERVER_ADD_REALM).equal(expected);
            });
        });
    });
});
