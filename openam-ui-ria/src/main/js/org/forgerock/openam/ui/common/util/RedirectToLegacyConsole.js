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
 * Copyright 2015 ForgeRock AS.
 */

/*global define, window*/
define('org/forgerock/openam/ui/common/util/RedirectToLegacyConsole', [
    'jquery',
    'org/forgerock/commons/ui/common/main/AbstractDelegate',
    'org/forgerock/commons/ui/common/main/Configuration',
    'org/forgerock/commons/ui/common/util/Constants'
], function ($, AbstractDelegate, Configuration, Constants) {
    var obj = new AbstractDelegate(Constants.host + '/' + Constants.context);

    obj.global = {
        commonTasks  : function () { obj.global.redirectToTab(0); },
        accessControl: function () { obj.global.redirectToTab(1); },
        federation   : function () { obj.global.redirectToTab(2); },
        configuration: function () { obj.global.redirectToTab(4); },
        sessions     : function () { obj.global.redirectToTab(5); },
        redirectToTab: function (tabIndex) {
            obj.getJATOPageSession().done(function (session) {
                window.location.href = '/' + Constants.context + '/task/Home?' +
                'Home.tabCommon.TabHref=' + tabIndex +
                '&jato.pageSession=' + session;
            });
        }
    };

    obj.realm = {
        general       : function () { obj.realm.redirectToTab(11); },
        autentication : function () { obj.realm.redirectToTab(12); },
        services      : function () { obj.realm.redirectToTab(13); },
        dataStores    : function () { obj.realm.redirectToTab(14); },
        privileges    : function () { obj.realm.redirectToTab(15); },
        policies      : function () { obj.realm.redirectToTab(16); },
        subjects      : function () { obj.realm.redirectToTab(17); },
        agents        : function () { obj.realm.redirectToTab(18); },
        sts           : function () { obj.realm.redirectToTab(19); },
        scripts       : function () { obj.realm.redirectToTab(20); },
        redirectToTab : function (tabIndex) {
            obj.getJATOPageSession().done(function (session) {
                var realm = Configuration.globalData.auth.subRealm || '/';
                window.location.href = '/' + Constants.context + '/realm/RealmProperties?' +
                'RMRealm.tblDataActionHref=' + realm +
                '&RealmProperties.tabCommon.TabHref=' + tabIndex +
                '&jato.pageSession=' + session;
            });
        }
    };

    obj.getJATOPageSession = function () {
        var promise = obj.serviceCall({
            url: '/realm/RMRealm?RMRealm.tblDataActionHref=/',
            dataType: 'html'
        });

        return $.when(promise)
        .then(function (data) {
            return data.match(/jato.pageSession=(.*?)"/)[1];
        });
    };

    return obj;
});
