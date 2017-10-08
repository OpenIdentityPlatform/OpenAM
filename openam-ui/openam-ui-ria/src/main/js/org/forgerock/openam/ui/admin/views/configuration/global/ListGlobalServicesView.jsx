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

import { Grid, ListGroup, ListGroupItem, PageHeader } from "react-bootstrap";
import { t } from "i18next";
import React, { Component } from "react";

import PageDescription from "components/PageDescription";
import ServicesService from "org/forgerock/openam/ui/admin/services/global/ServicesService";

class ListGlobalServicesView extends Component {
    constructor (props) {
        super(props);
        this.state = {
            items: []
        };
    }
    componentDidMount () {
        ServicesService.instance.getAll().then((response) => {
            this.setState({ items: response });
        });
    }
    render () {
        const items = this.state.items.map((item) => (
            <ListGroupItem href={ `#configure/global-services/${encodeURIComponent(item._id)}` } key={ item._id }>
                { item.name }
            </ListGroupItem>)
        );

        return (
            <Grid>
                <PageHeader bsClass="page-header page-header-no-border">
                    { t("config.AppConfiguration.Navigation.links.configure.global-services") }
                </PageHeader>

                <PageDescription>{ t("console.configuration.globalServices.description") }</PageDescription>

                <ListGroup>
                    { items }
                </ListGroup>
            </Grid>
        );
    }
}

export default ListGlobalServicesView;
