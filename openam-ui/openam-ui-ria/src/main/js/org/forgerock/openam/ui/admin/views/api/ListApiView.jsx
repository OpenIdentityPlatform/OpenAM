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

import { Grid, Panel, Row, Col } from "react-bootstrap";
import React, { Component } from "react";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import _ from "lodash";

import { getGroupedPaths } from "org/forgerock/openam/ui/admin/services/global/ApiService";
import SideNavGroupItem from "./SideNavGroupItem";

class ListApiView extends Component {
    constructor (props) {
        super(props);
        this.handleSelectItem = this.handleSelectItem.bind(this);
        this.state = {
            items: [],
            iFrameSrc: "",
            activeItem: undefined
        };
    }
    componentDidMount () {
        getGroupedPaths().then((response) => {
            this.setState({
                items: response
            });
        });
    }
    handleSelectItem (path) {
        this.setState({
            iFrameSrc: `/${Constants.context}/api?url=/${Constants.context}/json${path}?_api`,
            activeItem : path
        });
    }
    render () {
        const groups = _.map(this.state.items, (item) =>
            <SideNavGroupItem
                activeItem={ this.state.activeItem }
                group={ item }
                onItemClick={ this.handleSelectItem }
            />
        );

        return (
            <Grid className="api-section">
                <Row >
                    <Col md={ 4 }>
                        <Panel className="list-panel-style">
                            <nav className="sidenav" >
                                <ol className="list-unstyled" >
                                    { groups }
                                </ol>
                            </nav>
                        </Panel>
                    </Col>
                    <Col md={ 8 }>
                        <Panel className="iframe-panel-style">
                            <iframe className="api-explorer-style" src={ this.state.iFrameSrc } />
                        </Panel>
                    </Col>
                </Row>
            </Grid>
        );
    }
}

export default ListApiView;
