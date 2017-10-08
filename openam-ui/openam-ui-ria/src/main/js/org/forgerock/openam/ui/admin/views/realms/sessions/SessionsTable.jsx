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
import { Button, ButtonToolbar, ControlLabel, Panel, Table } from "react-bootstrap";
import { t } from "i18next";
import Block from "components/Block";
import React, { Component, PropTypes } from "react";
import SessionsTableRow from "./SessionsTableRow";
import store from "store/index";

const findOwnSession = (dataList) =>
    _.find(dataList, (data) => data.sessionHandle === store.getState().session.sessionHandle);

class SessionsTable extends Component {
    constructor (props) {
        super(props);
        this.handleSelectAll = this.handleSelectAll.bind(this);
        this.handleSelectRow = this.handleSelectRow.bind(this);
        this.handleDeleteRow = this.handleDeleteRow.bind(this);
        this.handleDeleteSelected = this.handleDeleteSelected.bind(this);
        this.state = {
            checked: [],
            ownSession: findOwnSession(this.props.data)
        };
    }

    componentWillReceiveProps (nextProps) {
        const updated = _.findByValues(nextProps.data, "sessionHandle", _.pluck(this.state.checked, "sessionHandle"));
        this.setState({
            checked: updated,
            ownSession: findOwnSession(nextProps.data)
        });
    }

    handleDeleteRow (session) {
        this.props.onSessionsInvalidate([session]);
    }

    handleDeleteSelected () {
        this.props.onSessionsInvalidate(this.state.checked);
    }

    handleSelectAll (e) {
        this.setState({
            checked: e.target.checked
            ? _.without(this.props.data, this.state.ownSession)
            : []
        });
    }

    handleSelectRow (session, checked) {
        const updated = checked ? this.state.checked.concat(session) : _.without(this.state.checked, session);
        this.setState({ checked: updated });
    }

    render () {
        const isChecked = (session) => _.includes(this.state.checked, session);
        const isAllChecked = () => {
            const numberOfDeletableSessions =
                this.state.ownSession ? this.props.data.length - 1 : this.props.data.length;
            return numberOfDeletableSessions > 0 && this.state.checked.length === numberOfDeletableSessions;
        };

        return (
            <div>
                <ButtonToolbar className="page-toolbar">
                    <Button disabled={ !this.state.checked.length } onClick={ this.handleDeleteSelected }>
                        <span className="fa fa-close" /> { t("console.sessions.invalidateSelected") }
                    </Button>
                </ButtonToolbar>

                <Panel>
                    <Block header={ this.props.userId }>
                        <Table>
                            <thead>
                                <tr>
                                    <th className="select-all-header-cell">
                                        <ControlLabel htmlFor="selectAll" srOnly>
                                            { t("common.form.selectAll") }
                                        </ControlLabel>
                                        <input
                                            checked={ isAllChecked() }
                                            id="selectAll"
                                            onChange={ this.handleSelectAll }
                                            type="checkbox"
                                        />
                                    </th>
                                    <th>{ t("console.sessions.table.headers.0") }</th>
                                    <th>{ t("console.sessions.table.headers.1") }</th>
                                    <th>{ t("console.sessions.table.headers.2") }</th>
                                    <th className="fr-col-btn-1" />
                                </tr>
                            </thead>
                            <tbody>
                                { _.map(this.props.data, (session) =>
                                    <SessionsTableRow
                                        checked={ isChecked(session) }
                                        data={ session }
                                        onDelete={ this.handleDeleteRow }
                                        onSelect={ this.handleSelectRow }
                                        sessionHandle={ store.getState().session.sessionHandle }
                                    />
                                ) }
                            </tbody>
                        </Table>
                    </Block>
                </Panel>
            </div>
        );
    }
}

SessionsTable.propTypes = {
    data: PropTypes.arrayOf(PropTypes.shape({
        idleTime: PropTypes.string,
        maxIdleExpirationTime: PropTypes.string,
        maxSessionExpirationTime: PropTypes.string,
        sessionHandle: PropTypes.string
    })),
    onSessionsInvalidate: PropTypes.func.isRequired,
    userId: PropTypes.string.isRequired
};

export default SessionsTable;
