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
 * Copyright 2015-2016 ForgeRock AS.
 */


import $ from "jquery";
import "bootstrap-tabdrop";  // jquery dependency

import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import AuthenticationService from "org/forgerock/openam/ui/admin/services/realm/AuthenticationService";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import Form from "org/forgerock/openam/ui/admin/models/Form";
import Messages from "org/forgerock/commons/ui/common/components/Messages";
import Promise from "org/forgerock/openam/ui/common/util/Promise";


class ListRealmsView extends AbstractView {
    constructor () {
        super();
        this.template = "templates/admin/views/realms/authentication/modules/EditModuleViewTemplate.html";
        this.events = {
            "click [data-revert]"          : "revert",
            "click [data-save]"            : "save",
            "show.bs.tab ul.nav.nav-tabs a": "renderTab"
        };
    }
    render ([realmPath, type, name]) {

        this.data.realmPath = realmPath;
        this.data.type = type;
        this.data.name = name;

        Promise.all([
            AuthenticationService.authentication.modules.schema(this.data.realmPath, this.data.type),
            AuthenticationService.authentication.modules.get(
                this.data.realmPath, this.data.name, this.data.type,
                { errorsHandlers: { "Not Found": { status: 404 } } }),
            AuthenticationService.authentication.modules.types.get(this.data.realmPath, this.data.type)
        ]).then(([schemaData, valuesData, moduleType]) => {
            this.data.schema = schemaData;
            this.data.values = valuesData;
            this.data.typeDescription = moduleType.name;

            this.parentRender(() => {
                if (!this.data.schema.grouped) {
                    this.data.form = new Form(this.$el.find("#tabpanel")[0], this.data.schema, this.data.values);
                }

                this.$el.find("ul.nav a:first").tab("show");
                this.$el.find(".tab-menu .nav-tabs").tabdrop();
            });
        }, () => {
            this.data.error = $.t("console.authentication.modules.notFound", { name });
            this.parentRender();
        });
    }
    save () {
        AuthenticationService.authentication.modules
            .update(this.data.realmPath, this.data.name, this.data.type, this.data.form.data())
            .then(() => {
                EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
            }, (response) => {
                Messages.addMessage({ type: Messages.TYPE_DANGER, response });
            });
    }
    revert () {
        this.data.form.reset();
    }
    renderTab (event) {
        const id = $(event.target).attr("href").slice(1);
        const schema = this.data.schema.properties[id];
        const element = this.$el.find("#tabpanel").empty().get(0);

        this.data.form = new Form(element, schema, this.data.values);
    }
}

export default ListRealmsView;
