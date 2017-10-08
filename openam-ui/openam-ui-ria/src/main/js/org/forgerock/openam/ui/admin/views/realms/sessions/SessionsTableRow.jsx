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
import { Badge, Button, ButtonGroup, ControlLabel } from "react-bootstrap";
import { t } from "i18next";
import moment from "moment";
import React, { PropTypes } from "react";

const SessionsTableRow = ({ checked, data, onDelete, onSelect, sessionHandle }) => {
    const handleDelete = () => onDelete(data);
    const handleSelect = (event) => onSelect(data, event.target.checked);
    const selectId = _.uniqueId("select");
    const rowActions = data.sessionHandle !== sessionHandle
        ? <ButtonGroup className="pull-right">
            <Button bsStyle="link" onClick={ handleDelete } title={ t("console.sessions.invalidate") }>
                <i className="fa fa-close" />
            </Button>
        </ButtonGroup>
        : <Badge>{ t("console.sessions.yourSession") }</Badge>;

    return (
        <tr className={ checked ? "selected" : undefined } >
            <td>
                <ControlLabel htmlFor={ selectId } srOnly>{ t("common.form.select") }</ControlLabel>
                <input
                    checked={ checked }
                    disabled={ data.sessionHandle === sessionHandle }
                    id={ selectId }
                    onChange={ handleSelect }
                    type="checkbox"
                />
            </td>

            <td>{ moment(data.latestAccessTime).fromNow(true) }</td>

            <td
                title={ t("console.sessions.table.expires", {
                    timestamp: moment(data.maxIdleExpirationTime).toISOString()
                }) }
            >
                { moment(data.maxIdleExpirationTime).fromNow(true) }
            </td>

            <td
                title={ t("console.sessions.table.expires", {
                    timestamp: moment(data.maxSessionExpirationTime).toISOString()
                }) }
            >
                { moment(data.maxSessionExpirationTime).fromNow(true) }
            </td>

            <td className="fr-col-btn-1">
                { rowActions }
            </td>
        </tr>
    );
};

SessionsTableRow.propTypes = {
    checked: PropTypes.bool.isRequired,
    data: PropTypes.arrayOf(PropTypes.shape({
        latestAccessTime: PropTypes.string.isRequired,
        maxIdleExpirationTime: PropTypes.string.isRequired,
        maxSessionExpirationTime: PropTypes.string.isRequired,
        sessionHandle: PropTypes.string.isRequired
    })).isRequired,
    onDelete: PropTypes.func.isRequired,
    onSelect: PropTypes.func.isRequired,
    sessionHandle: PropTypes.string.isRequired
};

export default SessionsTableRow;
