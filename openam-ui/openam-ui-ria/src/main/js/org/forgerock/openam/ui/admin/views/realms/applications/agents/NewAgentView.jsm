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
 * Copyright 2016 ForgeRock AS.
 */

import $ from "jquery";
import AbstractView from "org/forgerock/commons/ui/common/main/AbstractView";
import FlatJSONSchemaView from "org/forgerock/openam/ui/common/views/jsonSchema/FlatJSONSchemaView";
import { create, getInitialState } from "org/forgerock/openam/ui/admin/services/realm/AgentsService";
import Messages from "org/forgerock/commons/ui/common/components/Messages";

class NewAgentView extends AbstractView {
    constructor () {
        super();

        this.template = "templates/admin/views/realms/applications/agents/NewAgentTemplate.html";
        this.partials = [
            "partials/alerts/_Alert.html"
        ];
        this.events = {
            "click [data-create]": "onCreateClick"
        };
    }
    render ([realmPath, agentType]) {

        getInitialState(realmPath, agentType).then((response) => {
            const options = {
                schema: response.schema,
                values: response.values,
                showOnlyRequiredAndEmpty: true
            };

            this.jsonSchemaView = new FlatJSONSchemaView(options);
            this.type = agentType;
            this.data.realmPath = realmPath;
            this.data.title = $.t("console.applications.agents.new.title", { agentType });
            this.parentRender(() => {
                $(this.jsonSchemaView.render().el).appendTo(this.$el.find("[data-json-form]"));
            });
        });

    }
    onCreateClick () {
        create(this.data.realmPath, this.type, this.jsonSchemaView.getData()).then(() => {
            // TODO - Edit Agent Views
        }, (response) => {
            Messages.addMessage({
                response,
                type: Messages.TYPE_DANGER
            });
        });
    }
}

export default NewAgentView;
