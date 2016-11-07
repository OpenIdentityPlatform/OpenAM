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

import _ from "lodash";
import { Button, Panel, FormGroup, ControlLabel } from "react-bootstrap";
import { t } from "i18next";
import React, { Component } from "react";
import Select from "react-select";
import {
    getByRealm,
    getByUserIdAndRealm,
    invalidateByHandles
} from "org/forgerock/openam/ui/admin/services/global/SessionsService";
import { getByIdStartsWith } from "org/forgerock/openam/ui/admin/services/global/UsersService";
import CallToAction from "components/CallToAction";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import EventManager from "org/forgerock/commons/ui/common/main/EventManager";
import PageDescription from "components/PageDescription";
import SessionsTable from "./SessionsTable";
import showConfirmationBeforeAction from "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeAction";
import SimplePageHeader from "components/SimplePageHeader";
import withRouter from "org/forgerock/commons/ui/common/components/hoc/withRouter";
import withRouterPropType from "org/forgerock/commons/ui/common/components/hoc/withRouterPropType";

const fetchUsersByPartialId = _.debounce((userId, callback) => {
    if (_.isEmpty(userId)) {
        callback(null, { options: [] });
    } else {
        getByIdStartsWith(userId).then((response) => {
            callback(null, {
                options: _.map(response, (user) => ({ label: user, value: user }))
            });
        }, (error) => callback(error.statusText));
    }
}, 300);

class SessionsView extends Component {
    constructor (props) {
        super(props);

        this.handleSelectAsyncOnChange = this.handleSelectAsyncOnChange.bind(this);
        this.handleClickInvalidateAll = this.handleClickInvalidateAll.bind(this);
        this.handleInvalidateSessions = this.handleInvalidateSessions.bind(this);
        this.fetchSessionsByUserIdAndRealm = this.fetchSessionsByUserIdAndRealm.bind(this);
        this.state = {
            sessions: []
        };
    }

    handleInvalidateSessions (sessions) {
        const handles = _.pluck(sessions, "sessionHandle");
        invalidateByHandles(handles).then(() =>
            this.fetchSessionsByUserIdAndRealm(this.state.userId, this.props.router.params[0]));
    }

    handleClickInvalidateAll () {
        const realm = this.props.router.params[0];
        showConfirmationBeforeAction(
            { message: t("console.sessions.confirmInvalidate", { realm }) },
            () => {
                const getSessionHandles = (users) => _.map(users, (user) => user.sessionHandle);

                getByRealm(realm)
                    .then(getSessionHandles)
                    .then(invalidateByHandles)
                    .then(() => {
                        this.setState({
                            userId: null,
                            sessions: []
                        });

                        EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidationSuccessful");
                    });
            },
            t("console.sessions.invalidate"));
    }

    fetchSessionsByUserIdAndRealm (userId, realm) {
        getByUserIdAndRealm(userId, realm).then((response) => {
            this.setState({ sessions: response });
        });
    }

    handleSelectAsyncOnChange (newValue) {
        const userId = _.get(newValue, "value");
        this.setState({
            sessions: [],
            userId
        });

        if (userId) {
            this.fetchSessionsByUserIdAndRealm(userId, this.props.router.params[0]);
        }
    }

    render () {
        let content;

        if (this.state.sessions.length) {
            content = (
                <SessionsTable
                    data={ this.state.sessions }
                    onSessionsInvalidate={ this.handleInvalidateSessions }
                    userId={ this.state.userId }
                />
            );
        } else if (this.state.userId) {
            content = <Panel><CallToAction><h3>{ t("console.sessions.table.noResults") }</h3></CallToAction></Panel>;
        }

        return (
            <div>
                <SimplePageHeader title={ t("console.sessions.title") }>
                    <Button
                        bsStyle="danger"
                        onClick={ this.handleClickInvalidateAll }
                    >
                        { t("console.sessions.invalidateAll") }
                    </Button>
                </SimplePageHeader>

                <PageDescription>{ t("console.sessions.search.intro") }</PageDescription>

                <FormGroup controlId="findAUser">
                    <ControlLabel srOnly>{ t("console.sessions.search.title") }</ControlLabel>
                    <Select.Async
                        autoload={ false }
                        inputProps={ {
                            id: "findAUser"
                        } }
                        isLoading
                        loadOptions={ fetchUsersByPartialId }
                        noResultsText={ t("console.sessions.search.noResults") }
                        onChange={ this.handleSelectAsyncOnChange }
                        placeholder={ t("console.sessions.search.placeholder") }
                        searchPromptText={ t("console.sessions.search.searchPrompt") }
                        value={ this.state.userId }
                    />
                </FormGroup>
                { content }
            </div>
        );
    }
}

SessionsView.propTypes = {
    router: withRouterPropType
};

export default withRouter(SessionsView);
