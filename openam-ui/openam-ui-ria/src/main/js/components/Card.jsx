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

import React from "react";

/**
 * A card.
 * @module components/Card
 * @param {Object} props Properties passed to this component
 * @param {string} props.href The link to the associated page
 * @param {ReactNode} props.children Children to add within this component
 * @param {string} props.icon Icon to display on the card
 * @returns {ReactElement} Renderable React element
 */
const Card = ({ href, children, icon }) => (
    <div data-panel-card className="panel-default panel am-panel-card">
        <a href={href}>
            <div className="card-body">
                <div className="card-icon-circle card-icon-circle-sm bg-primary">
                    <i className={`fa ${icon}`}></i>
                </div>
                { children }
            </div>
        </a>
    </div>
);

Card.propTypes = {
    link: React.PropTypes.string.isRequired,
    icon: React.PropTypes.string.isRequired,
    children: React.PropTypes.node
};

export default Card;
