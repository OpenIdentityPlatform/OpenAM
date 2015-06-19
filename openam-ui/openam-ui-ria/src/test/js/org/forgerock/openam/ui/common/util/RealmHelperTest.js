/**
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright (c) 2015 ForgeRock AS. All rights reserved.
*
* The contents of this file are subject to the terms
* of the Common Development and Distribution License
* (the License). You may not use this file except in
* compliance with the License.
*
* You can obtain a copy of the License at
* http://forgerock.org/license/CDDLv1.0.html
* See the License for the specific language governing
* permission and limitations under the License.
*
* When distributing Covered Code, include this CDDL
* Header Notice in each file and include the License file
* at http://forgerock.org/license/CDDLv1.0.html
* If applicable, add the following below the CDDL Header,
* with the fields enclosed by brackets [] replaced by
* your own identifying information:
* "Portions Copyrighted [year] [name of copyright owner]"
*/

require(['squire'], function(Squire) {
    new Squire()
    .store('org/forgerock/commons/ui/common/main/Configuration')
    .store('org/forgerock/commons/ui/common/util/UIUtils')
    .require([
        'org/forgerock/openam/ui/common/util/RealmHelper',
        'mocks'
    ], function(subject, mocks) {
        module('org/forgerock/openam/ui/common/util/RealmHelper', {
          beforeEach: function() {
            // TODO: Don't drop UIUtils into the global namespace
            UIUtils = mocks.store['org/forgerock/commons/ui/common/util/UIUtils'];
            Configuration = mocks.store['org/forgerock/commons/ui/common/main/Configuration'];

            Configuration.globalData = {
                auth: {
                    subRealm: undefined
                }
            };
          }
        });
        // #decorateURLWithOverrideRealm
        test('#decorateURLWithOverrideRealm', sinon.test(function() {
            this.stub(UIUtils, 'getURIQueryString').returns('realm=realm1');

            equal(subject.decorateURLWithOverrideRealm('http://www.example.com'), 'http://www.example.com?realm=realm1', 'appends override realm query string parameter');
        }));
        test('#decorateURLWithOverrideRealm when a query string is present', sinon.test(function() {
            this.stub(UIUtils, 'getURIQueryString').returns('realm=realm1');

            equal(subject.decorateURLWithOverrideRealm('http://www.example.com?key=value'), 'http://www.example.com?key=value&realm=realm1', 'appends override realm query string parameter');
        }));

        // #decorateURIWithRealm
        test('#decorateURIWithRealm', sinon.test(function() {
            Configuration.globalData.auth.subRealm = 'realm1';
            this.stub(UIUtils, 'getURIQueryString').returns('realm=realm2');

            equal(subject.decorateURIWithRealm('http://www.example.com/__subrealm__/'), 'http://www.example.com/realm1/?realm=realm2', 'replaces __subrealm__ with sub realm and appends override realm query string parameter');
        }));

        // #decorateURIWithSubRealm
        test('#decorateURIWithSubRealm', sinon.test(function() {
            Configuration.globalData.auth.subRealm = 'realm1';

            equal(subject.decorateURIWithSubRealm('http://www.example.com/__subrealm__/'), 'http://www.example.com/realm1/', 'replaces __subrealm__ with sub realm');
        }));
        test('#decorateURIWithSubRealm when there is not sub realm', sinon.test(function() {
            Configuration.globalData.auth.subRealm = '';

            equal(subject.decorateURIWithSubRealm('http://www.example.com/__subrealm__/'), 'http://www.example.com/', 'removes __subrealm__');
        }));

        // #getOverrideRealm
        test('#getOverrideRealm when realm override is present in query string', sinon.test(function() {
            this.stub(UIUtils, 'getURIQueryString').returns('realm=realm1');

            equal(subject.getOverrideRealm(), 'realm1', 'returns override realm');
        }));
        test('#getOverrideRealm when realm override is present in fragment query string', sinon.test(function() {
            this.stub(UIUtils, 'getURIFragment').returns('login&realm=realm1');

            equal(subject.getOverrideRealm(), 'realm1', 'returns override realm');
        }));
        test('#getOverrideRealm when realm override is present in query string and fragment query string', sinon.test(function() {
            this.stub(UIUtils, 'getURIQueryString').returns('realm=realm1');
            this.stub(UIUtils, 'getURIFragment').returns('login&realm=realm2');

            equal(subject.getOverrideRealm(), 'realm1', 'returns query string realm');
        }));

        // #getSubRealm
        test('#getSubRealm when page is login', sinon.test(function() {
            this.stub(UIUtils, 'getURIFragment').returns('login/realm1');

            equal(subject.getSubRealm(), 'realm1', 'returns sub realm');
        }));
        test('#getSubRealm when page is not login and subRealm is already set', sinon.test(function() {
            this.stub(UIUtils, 'getURIFragment').returns('other');
            Configuration.globalData.auth.subRealm = 'realm1';

            equal(subject.getSubRealm(), 'realm1', 'returns sub realm');
        }));
        test('#getSubRealm when page is not login and subRealm is not set', sinon.test(function() {
            this.stub(UIUtils, 'getURIFragment').returns('other');
            Configuration.globalData.auth.subRealm = '';

            equal(subject.getSubRealm(), '', 'returns empty string');
        }));
    });
});
