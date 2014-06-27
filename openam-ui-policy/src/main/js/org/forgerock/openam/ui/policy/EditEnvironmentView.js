/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/**
 * @author JKigwana
 */

/*global window, define, $, form2js, _, js2form, document, console */

define( "org/forgerock/openam/ui/policy/EditEnvironmentView", [
        "org/forgerock/commons/ui/common/main/AbstractView",
        "org/forgerock/commons/ui/common/util/UIUtils",
        "org/forgerock/commons/ui/common/main/EventManager",
        "org/forgerock/commons/ui/common/util/Constants",
        "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, uiUtils, eventManager, constants, conf) {

    var EditEnvironmentView = AbstractView.extend({

        events: {},
        data:{},
        editItem: null,

        render: function(data, callback, element) {
            this.setElement(element);
            this.data = data;
            if (callback) {callback();}
        },

        clearListItem: function(){
            //TODO : unbind events
            this.$el.empty();
        },

        newListItem: function(){
            this.$el.html(uiUtils.fillTemplateWithData("templates/policy/EditEnvironmentTemplate.html", this.data));
            this.$el.find('.icon-remove').bind("click", this.onDelete);
        },

        onDelete: function(e){
            var item = $(e.currentTarget).closest('li');
            //TODO : unbind events
            item.animate({height: 0, paddingTop: 0, paddingBottom: 0,marginTop: 0,marginBotttom: 0, opacity:0}, function(){
                item.remove();
            });
        }

    });


    return new EditEnvironmentView();
});
