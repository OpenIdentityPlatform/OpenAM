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
import _ from "lodash";

import SideNavChildItem from "./SideNavChildItem";

const SideNavGroupItem = ({ group, activeItem, onItemClick }) => {
    const items = _.map(group.paths, (path) =>
        <SideNavChildItem isActiveItem={ activeItem === path } onItemClick={ onItemClick } path={ path } />
    );
    return (
        <li key={ group._id }>
            <a
                aria-controls={ group._id }
                aria-expanded="false"
                className="text-primary"
                data-toggle="collapse"
                href={ `#${group._id}` }
            >
                { group.name }
            </a>
            <ol className="collapse list-unstyled sidenav-submenu" id={ group._id } >
                { items }
            </ol>
        </li>
    );
};

SideNavGroupItem.propTypes = {
    activeItem: PropTypes.string.isRequired,
    group: PropTypes.shape.isRequired,
    onItemClick: PropTypes.func.isRequired
};

export default SideNavGroupItem;
