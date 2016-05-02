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

import Handlebars from "handlebars";
import { View } from "backbone";

/**
 * A component for rendering a Handlebars template with some associated data.
 *
 * This component is not designed for extension.
 */
export default class TemplateComponent extends View {
    /**
     * Initialise the component.
     *
     * @param {String} template - the Handlebars template (contents not filename).
     */
    initialize ({ template }) {
        this.template = template;
    }
    render () {
        const html = Handlebars.compile(this.template)(this.data);
        this.$el.html(html);
        return this;
    }
    setData (data) {
        this.data = data;
    }
}
