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

import React, { PropTypes } from "react";
import { t } from "i18next";
import { Button, ButtonGroup } from "react-bootstrap";
import moment from "moment";


const SessionsTableRow = ({
    onDelete,
    onSelect,
    checked,
    data
}) => {
    const handleSelect = (e) => {
        onSelect(data, e.target.checked);
    };

    const handleDelete = () => {
        onDelete(data);
    };

    return (
        <tr className={ checked ? "selected" : "" } >

            <td>
                <input
                    checked={ checked }
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
                <ButtonGroup className="pull-right">
                    <Button
                        bsStyle="link"
                        onClick={ handleDelete }
                        title={ t("common.form.delete") }
                    >
                        <i className="fa fa-close" />
                    </Button>
                </ButtonGroup>
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
    onSelect: PropTypes.func.isRequired
};

export default SessionsTableRow;
