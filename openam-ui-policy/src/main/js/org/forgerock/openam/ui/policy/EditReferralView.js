/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/**
 * @author JKigwana
 */

/*global window, define, $, form2js, _, js2form, document, console */

define("org/forgerock/openam/ui/policy/EditReferralView", [
    "org/forgerock/openam/ui/policy/AbstractEditView",
    "org/forgerock/openam/ui/policy/ResourcesListView",
    "org/forgerock/openam/ui/policy/AddNewResourceView",
    "org/forgerock/openam/ui/policy/ReviewInfoView",
    "org/forgerock/openam/ui/policy/SelectRealmsView",
    "org/forgerock/openam/ui/policy/PolicyDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/components/Accordion",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/components/Messages"
], function (AbstractEditView, resourcesListView, addNewResourceView, reviewInfoView, selectRealmsView, policyDelegate, uiUtils, Accordion, constants, eventManager, router, messager) {
    var EditReferralView = AbstractEditView.extend({
        template: "templates/policy/EditReferralTemplate.html",
        reviewTemplate: "templates/policy/ReviewReferralStepTemplate.html",
        data: {},
        validationFields: ["name", "resources", "realms"], 

        render: function (args, callback) {

            var self = this,
                data = self.data,
                referralName = args[1],
                referalPromise  = this.getReferral(referralName),
                appPromise      = policyDelegate.getApplicationByName(args[0]),
                allRealmsPromise = policyDelegate.getAllRealms();

            $.when(appPromise, referalPromise, allRealmsPromise).done(function(app, referral, allRealms){

                if (referralName) {
                    data.entity = referral;
                    data.entityName = referralName;
                    data.entity.resources = _.clone(data.entity.resources[args[0]]);

                } else {
                    data.entity = {};
                    data.entityName = null;
                }

                data.options = {};
                data.options.appName = args[0];
                data.options.realm = app[0].realm;
                data.options.resourcePatterns = _.sortBy(app[0].resources);
                data.options.filteredRealms =  self.filterRealms(allRealms[0].result);
          
                self.parentRender(function () {

                    addNewResourceView.render(data);
                    resourcesListView.render(data);
                    selectRealmsView.render(data);
                    self.validateThenRenderReview();
                    self.initAccordion();

                    if (callback) {
                        callback();
                    }
                });

            });

        },

        updateFields: function () {
            var entity = this.data.entity,
                dataFields = this.$el.find('[data-field]');

            _.each(dataFields, function (field, key, list) {
                entity[field.getAttribute('data-field')] = field.value;
            });

        },

        filterRealms: function (allRealms) {

            var currentRealm = this.data.options.realm,
                lastFSlash = currentRealm.lastIndexOf('/'),
                parentRealm = currentRealm.substring(0,lastFSlash),
                realmDepth =  lastFSlash === 0 ? 1 : currentRealm.match(/\//g).length,
                filtered = [],
                realmLength = 0;

            _.each(allRealms, function(realm){

                realmLength = realm.match(/\//g).length;
                if (realm !== currentRealm && 
                   ((realm.indexOf(currentRealm + '/') === 0 && realmLength === realmDepth + 1) || //children
                    (realm.indexOf(parentRealm + '/')  === 0 && realmLength === realmDepth && realm !== "/") //sibligns
                    )) {
                  
                  filtered.push(realm);
                }

            });

            filtered = _.sortBy(filtered);
            return filtered;

        },

        getReferral: function (name) {
            var self = this,
                deferred = $.Deferred(),
                referral = {};

            if (name) {
                policyDelegate.getReferralByName(name).done(function (referral) {
                    deferred.resolve(referral);
                });
            } else {
                deferred.resolve(referral);
            }
            return deferred.promise(referral);
        },

        submitForm: function () {
    
            var self = this,
                persisted = _.clone(this.data.entity),
                resources = _.clone(persisted.resources);

            persisted.resources = {};
            persisted.resources[this.data.options.appName] = resources;

            if (this.data.entityName) {
                policyDelegate.updateReferral( this.data.entityName, persisted )
                .done(function (e) {
                    router.routeTo(router.configuration.routes.managePolicies, {args: [self.data.options.appName], trigger: true});
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "referralUpdated");
                })
                .fail(function (e) {
                    self.errorHandler(e);
                });
            } else {
                policyDelegate.createReferral(persisted)
                .done(function (e) {
                    router.routeTo(router.configuration.routes.managePolicies, {args: [self.data.options.appName], trigger: true});
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "referralCreated");
                })
                .fail(function (e) {
                    self.errorHandler(e);
                });
            }
        },

        errorHandler: function (e) {
            //TODO
            var obj = { message: JSON.parse(e.responseText).message, type: "error"};
            messager.messages.addMessage( obj ); 
            console.error(e.responseJSON, e.responseText, e);
        }
    });

    return new EditReferralView();
});
