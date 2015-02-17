/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
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

/*global define, $, _ */

define("org/forgerock/openam/ui/uma/ShareView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/uma/delegates/UmaDelegate",
    "org/forgerock/openam/ui/uma/util/UmaUtils"
], function(AbstractView, conf, eventManager, uiUtils, constants, umaDelegate, umaUtils) {

    var ShareView = AbstractView.extend({

        template: "templates/uma/UmaInnerTemplate.html",
        events: {},
        render: function(args, callback) {

            var self = this,
                promise = umaUtils.getResourceSet(args[0], self.data.resourceSet);

            $.when(promise).done(function(resourceSet){

                self.data.resourceSet = resourceSet;

                self.parentRender(function(){

                    self.$el.find('#selectUser select').selectize({
                        valueField: 'name',
                        labelField: 'name',
                        searchField: 'name',
                        create: false,
                        load: function(query, callback) {
                            if (query.length < self.MIN_QUERY_LENGTH) {
                                return callback();
                            }
                            var selectize = this, list, obj;
                            umaDelegate.queryIdentities('users', query).done(function(data){
                                list = [];
                                _.each(data.result, function(result){
                                    obj = {};
                                    obj.name = result;
                                    list.push(obj);
                                });
                                callback(list);
                            }).error(function(e){
                                console.error('error', e);
                                callback();
                            });
                        },

                        onChange: function(value){
                            self.enableShareButton();
                        }
                    });

                    self.$el.find('#selectPermission select').selectize({

                        plugins: ['restore_on_backspace'],
                        delimiter: ',',
                        persist: false,
                        create: false,
                        hideSelected: true,
                        onChange: function(value){
                            self.enableShareButton();
                        }
                    });

                });

            });
        },

        enableShareButton: function(){
            var disabled = false;
            _.each(this.$el.find('.selectize-input'), function(selectize){
                if (! $(selectize).hasClass('has-items')){
                    disabled = true;
                }
            });
            this.$el.find('input#shareButton').prop('disabled', disabled);
        }

    });

    return ShareView;
});
