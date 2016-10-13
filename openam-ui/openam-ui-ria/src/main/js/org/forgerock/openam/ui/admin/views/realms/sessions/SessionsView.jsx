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

import { t } from "i18next";
import _ from "lodash";

import React, { Component } from "react";
import { PageHeader, Panel } from "react-bootstrap";
import Select from "react-select";

import { getUsers } from "org/forgerock/openam/ui/admin/services/realm/SessionsService";

const MAX_USERS = 10;
const fetchData = _.debounce((searchTerm, callback) => {
    if (_.isEmpty(searchTerm)) {
        callback(null, { options: [] });
    } else {
        getUsers(searchTerm, MAX_USERS).then((response) => {
            callback(null, {
                options: _.map(response, (user) => ({ label: user, value: user }))
            });
        }, (error) => callback(error.statusText));
    }
}, 300);

class SessionsView extends Component {
    constructor (props) {
        super(props);
        this.state = {
            selectedUser: null
        };
    }

    renderActiveSessionsTable (userId) {
        // TODO render table
        console.log(userId);
    }

    onUserSelect (value) {
        const userId = _.get(value, "value");

        this.setState({ selectedUser: userId });
        this.renderActiveSessionsTable(userId);
    }

    render () {
        this.onUserSelect = _.bind(this.onUserSelect, this);

        return (
            <div>
                <PageHeader bsClass="page-header page-header-no-border">
                    { t("console.sessions.title") }
                </PageHeader>
                <Panel>
                    <Select.Async
                        value={this.state.selectedUser}
                        onChange={this.onUserSelect}
                        loadOptions={fetchData}
                        autoload={false}
                        isLoading
                    />
                </Panel>
            </div>
        );
    }
}

export default SessionsView;
