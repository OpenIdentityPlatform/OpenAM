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

import { ListGroup, ListGroupItem } from "react-bootstrap";
import React, { Component } from "react";

import ScriptsService from "org/forgerock/openam/ui/admin/services/global/ScriptsService";

class ScriptsList extends Component {
    constructor (props) {
        super(props);

        this.state = {
            items: []
        };
    }
    componentDidMount () {
        ScriptsService.scripts.getAllDefault(this.props.subSchemaType).then((response) => {
            this.setState({ items: response });
        });
    }
    render () {
        const items = this.state.items.map((item) => (
            <ListGroupItem href={ `#realms/%2F/scripts/edit/${item._id}` } key={ item._id }>
                { item.name }
            </ListGroupItem>)
        );

        return (
            <ListGroup>
                { items }
            </ListGroup>
        );
    }
}

ScriptsList.propTypes = {
    subSchemaType: React.PropTypes.string.isRequired
};

export default ScriptsList;
