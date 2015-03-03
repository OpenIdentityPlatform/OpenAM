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

/*global define, $, _, Backgrid, Backbone */

define("org/forgerock/openam/ui/uma/views/resource/EditResource", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/uma/util/BackgridUtils",
    "org/forgerock/openam/ui/uma/util/UmaUtils",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/uma/delegates/UmaDelegate",
    "backgrid"
], function(AbstractView, conf, eventManager, uiUtils, constants, backgridUtils, umaUtils, router, umaDelegate, Backgrid) {
    var EditResource = AbstractView.extend({
        template: "templates/uma/views/resource/EditResource.html",
        baseTemplate: "templates/common/DefaultBaseTemplate.html",
        events: {
            "click a#revokeAll": "revokeAll",
            "click a#share": "share"
        },
        render: function(args, callback) {
            var self = this,
                grid,
                paginator,
                RevokeCell,
                SelectizeCell,
                UserPoliciesCollection,
                resourcesetPromise = umaUtils.getResourceSet(args[0], self.data.resourceSet);

            $.when(resourcesetPromise).done(function(resourceSet){

                self.data.resourceSet = resourceSet;

                var options = [];
                _.each(resourceSet.scopes, function(option){
                    options.push({text:option.displayName, value:option.name});
                });

                UserPoliciesCollection = Backbone.PageableCollection.extend({
                    url: "/" + constants.context + "/json/users/" + conf.loggedUser.username + "/uma/policies/" + args[0],
                    parseRecords: function (data, options) {
                        return data.permissions;
                    },
                    sync: backgridUtils.sync
                });

                RevokeCell = backgridUtils.TemplateCell.extend({
                    template: "templates/uma/backgrid/cell/RevokeCell.html",
                    events: {
                        "click revoke": "revoke",
                        "click save":   "save",
                        "click cancel": "cancel"
                    },
                    revoke: function(e) {
                        // TODO:
                    },
                    save: function(e) {
                        // TODO:
                    },
                    cancel: function(e) {
                        // TODO:
                    }
                });

                SelectizeCell = Backgrid.Cell.extend({
                    className: "selectize-cell",
                    template: "templates/uma/backgrid/cell/SelectizeCell.html",
                    render: function() {
                        var items = this.model.get('scopes'),
                            $select = null,
                            opts = {};

                        this.$el.html(uiUtils.fillTemplateWithData(this.template));

                        opts = {
                            create: false,
                            delimiter: ",",
                            dropdownParent: '#uma',
                            hideSelected: true,
                            persist: false,
                            plugins: ["restore_on_backspace"],
                            items: items,
                            options: options
                        };

                        $select = this.$el.find('select').selectize(opts)[0];

                        /* This an extention of the original positionDropdown method within Selectize. The override is
                         * required because using the dropdownParent 'body' places the dropdown out of scope of the
                         * containing backbone view. However adding the dropdownParent as any other element, has problems
                         * due the offsets and/positioning being incorrecly calucaluted in orignal positionDropdown method.
                         */
                        $select.selectize.positionDropdown = function() {
                            var $control = this.$control,
                                offset = this.settings.dropdownParent ? $control.offset() : $control.position();

                            if (this.settings.dropdownParent) {
                                offset.top  -= ($control.outerHeight(true)*2) + $(this.settings.dropdownParent).position().top;
                                offset.left -= $(this.settings.dropdownParent).offset().left + $(this.settings.dropdownParent).outerWidth() - $(this.settings.dropdownParent).outerWidth(true);
                            } else {
                                offset.top += $control.outerHeight(true);
                            }

                            this.$dropdown.css({
                                width : $control.outerWidth(),
                                top   : offset.top,
                                left  : offset.left
                            });
                        };

                        this.delegateEvents();
                        return this;
                    }
                });

                self.data.userPolicies = new UserPoliciesCollection();

                grid = new Backgrid.Grid({
                    columns: [
                    {
                        name: "subject",
                        label: $.t("uma.resources.show.grid.0"),
                        cell: backgridUtils.UnversalIdToUsername,
                        headerCell: backgridUtils.FilterHeaderCell,
                        editable: false
                    },
                    {
                        name: "lastModifiedBy",
                        label: $.t("uma.resources.show.grid.1"),
                        cell: backgridUtils.DatetimeAgoCell,
                        editable: false
                    },
                    {
                        name: "permissions",
                        label: $.t("uma.resources.show.grid.2"),
                        cell: SelectizeCell,
                        editable: false
                    },
                    {
                        name: "edit",
                        label: "",
                        cell: RevokeCell,
                        editable: false
                    }],

                    collection: self.data.userPolicies,
                    emptyText: $.t("uma.all.grid.empty")
                });

                paginator = new Backgrid.Extension.Paginator({
                    collection: self.data.userPolicies,
                    windowSize: 3
                });

                self.parentRender(function() {
                    self.$el.find("#backgridContainer").append( grid.render().el );
                    self.$el.find("#paginationContainer").append( paginator.render().el );
                    self.data.userPolicies.fetch({reset: true, processData: false});

                    if (callback) { callback(); }
                });

            });
        },

        revokeAll: function() {
            // TODO Use i18n
            uiUtils.jqConfirm($.t("uma.resources.show.revokeAllMessage"), function() {
                // TODO: Make a call to the policy delegate to revoke all access
            }, 325);
        },

        share: function(e) {
            e.preventDefault();
            eventManager.sendEvent(constants.EVENT_SHOW_DIALOG,{
                route: router.configuration.routes.dialogShare,
                args: [this.data.resourceSet.id]
            });
        }
    });

    return new EditResource();
});
