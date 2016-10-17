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
import { t } from "i18next";
import { Table } from "react-bootstrap";
import moment from "moment";
import React, { PropTypes } from "react";

const SessionsTable = ({ data }) => (
    <Table>
        <thead>
            <tr>
                <th>{ t("console.sessions.table.headers.0") }</th>
                <th>{ t("console.sessions.table.headers.1") }</th>
                <th>{ t("console.sessions.table.headers.2") }</th>
            </tr>
        </thead>
        <tbody>
            { _.map(data, ({ idleTime, sessionHandle, maxIdleExpirationTime, maxSessionExpirationTime }) =>
                <tr key={ sessionHandle }>
                    <td>
                        { moment(idleTime).fromNow(true) }
                    </td>
                    <td
                        title={ t("console.sessions.table.expires", {
                            timestamp: moment(maxIdleExpirationTime).toISOString()
                        }) }
                    >
                        { moment(maxIdleExpirationTime).fromNow(true) }
                    </td>
                    <td
                        title={ t("console.sessions.table.expires", {
                            timestamp: moment(maxSessionExpirationTime).toISOString()
                        }) }
                    >
                        { moment(maxSessionExpirationTime).fromNow(true) }
                    </td>
                </tr>
            ) }
        </tbody>
    </Table>
);

SessionsTable.propTypes = {
    data: PropTypes.arrayOf(PropTypes.shape({
        idleTime: PropTypes.string.required,
        maxIdleExpirationTime: PropTypes.string.required,
        maxSessionExpirationTime: PropTypes.string.required,
        sessionHandle: PropTypes.string.required
    })).required
};

export default SessionsTable;
