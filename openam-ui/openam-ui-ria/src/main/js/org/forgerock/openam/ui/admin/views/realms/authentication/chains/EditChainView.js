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

/*global define*/

define("org/forgerock/openam/ui/admin/views/realms/authentication/chains/EditChainView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/admin/delegates/SMSDelegate",
    "org/forgerock/openam/ui/admin/views/realms/authentication/chains/LinkView",
    "org/forgerock/openam/ui/admin/views/realms/authentication/chains/PostProcessView",
    "org/forgerock/openam/ui/admin/utils/FormHelper"
], function($, _, AbstractView, SMSDelegate, LinkView, PostProcessView, FormHelper) {

    var EditChainView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/chains/EditChainTemplate.html",
        events: {

            'click #saveChanges':     'saveChanges',
            'click #addModuleLink':   'addModuleLink'
        },

        render: function(args, callback) {

            var self = this;

            SMSDelegate.RealmAuthenticationChains.getChainWithType(args[1]).done(function (data) {

                self.data.chainData = data.chainData;
                self.data.allModules = data.modulesData;

                self.data.allCriteria = [{
                    REQUIRED : $.t('console.authentication.editChains.criteria.0')
                },{
                    OPTIONAL : $.t('console.authentication.editChains.criteria.1')
                },{
                    REQUISITE : $.t('console.authentication.editChains.criteria.2')
                },{
                    SUFFICIENT : $.t('console.authentication.editChains.criteria.3')
                }];

                self.parentRender(function(){

                    var sortable = self.$el.find("ol#sortable");

                    _.each(self.data.chainData.authChainConfiguration, function(linkConfig, index){
                        var el = $('<li class="chain-link" />');
                        sortable.append(el);
                        new LinkView().render(self.data, index, el);
                    });

                    self.$el.find('[data-toggle="popover"]').popover();
                    self.initSortable();

                    PostProcessView.render(self.data.chainData);

                    if (self.data.chainData.authChainConfiguration.length === 0) {
                        self.addModuleLink();
                    }
                });

            })
            .fail(function () {
                // TODO: Add failure condition
            });
        },

        saveChanges: function(e){
            e.preventDefault();

            var self = this,
                sorted = [],
                promise,
                chainlinks = this.$el.find('.chain-link');

            _.each(chainlinks, function(chainlink){
                sorted.push(_.findWhere(self.data.chainData.authChainConfiguration, {
                    id: $(chainlink).children().data().id
                }));

            });

            this.data.chainData.authChainConfiguration = sorted;
            this.data.chainData.loginSuccessUrl[0] = this.$el.find('#loginSuccessUrl').val();
            this.data.chainData.loginFailureUrl[0] = this.$el.find('#loginFailureUrl').val();

            promise = SMSDelegate.RealmAuthenticationChains.save(this.data.chainData._id, this.data.chainData);
            promise.fail(function(e) {
                // TODO: Add failure condition
                console.error(e);
            });

            FormHelper.bindSavePromiseToElement(promise, event.target);
        },

        addModuleLink: function(e) {
            if(e){
                e.preventDefault();
            }
            var el = $('<li class="chain-link" />'),
                moduleLink = new LinkView(),
                index = this.data.chainData.authChainConfiguration.length;
            this.data.chainData.authChainConfiguration.push({});

            this.$el.find("ol#sortable").removeClass("empty").append(el);
            moduleLink.render(this.data, index, el);
        },

        initSortable: function(){

            var self = this,
                dropText = $.t('console.authentication.editChains.dropHere');

            this.$el.find("ol#sortable").nestingSortable({
                exclude:'li:not(.chain-link)',
                delay: 100,
                vertical:true,
                placeholder:'<li class="placeholder"><i class="fa fa-download"></i>'+ dropText +'</li>',

                onDrag: function (item, position) {
                    item.css({
                        left: position.left - self.adjustment.left,
                        top: position.top - self.adjustment.top
                    });
                },

                onDragStart: function (item, container, _super) {

                    var offset = item.offset(),
                        pointer = container.rootGroup.pointer;

                    self.adjustment = {
                        left: pointer.left - offset.left + 5,
                        top: pointer.top - offset.top
                    };

                    item.addClass("dragged");
                    $("body").addClass("dragging");

                },

                onDrop: function  (item, container, _super, event) {
                    var clonedItem = $('<li/>').css({height: item.height() + 6, backgroundColor: 'transparent', borderColor: 'transparent'});

                    item.before(clonedItem);
                    item.animate( clonedItem.position(), 300, function  () {
                        clonedItem.detach();
                        _super(item, container);
                    });
                }
            });
        }
    });

    return EditChainView;

});
