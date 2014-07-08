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
 * @author Aleanora Kaladzinskaya
 * @author Eugenia Sergueeva
 */

/*global window, define, $, form2js, _, js2form, document, console */

define("org/forgerock/openam/ui/policy/EditPolicyView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/components/Accordion",
    "org/forgerock/openam/ui/policy/ManageSubjectsView",
    "org/forgerock/openam/ui/policy/ManageEnvironmentsView"
], function (AbstractView, uiUtils, Accordion, manageSubjects, manageEnvironments) {
    var EditPolicyView = AbstractView.extend({
        baseTemplate: "templates/policy/BaseTemplate.html",
        template: "templates/policy/EditPolicyTemplate.html",
        events: {
            'click input[name=nextButton]': 'openNextStep'
        },
        data:{              // Mock data to be replaced later
        
            "result" : [
              {
                "title" : "AND",
                "logical" : true,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "subjects" : {
                      "type" : "array",
                      "items" : {
                        "type" : "any"
                      }
                    }
                  }
                }
              },
              {
                "title" : "AnyUser",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                  }
                }
              },
              {
                "title" : "Attribute",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "value" : {
                      "type" : "string"
                    },
                    "id" : {
                      "type" : "string"
                    }
                  }
                }
              },
              {
                "title" : "Group",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "id" : {
                      "type" : "string"
                    }
                  }
                }
              },
              {
                "title" : "NONE",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                  }
                }
              },
              {
                "title" : "NOT",
                "logical" : true,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "subject" : {
                      "type" : "object",
                      "properties" : {
                      }
                    }
                  }
                }
              },
              {
                "title" : "OR",
                "logical" : true,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "subjects" : {
                      "type" : "array",
                      "items" : {
                        "type" : "any"
                      }
                    }
                  }
                }
              }, {
                "title" : "Policy",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "name" : {
                      "type" : "string"
                    },
                    "className" : {
                      "type" : "string"
                    },
                    "values" : {
                      "type" : "array",
                      "items" : {
                        "type" : "string"
                      }
                    }
                  }
                }
              },
              {
                "title" : "Role",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "id" : {
                      "type" : "string"
                    }
                  }
                }
              },
              {
                "title" : "User",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "id" : {
                      "type" : "string"
                    }
                  }
                }
              }
        ]

        },

        render: function (args, callback) {
            var self = this,
                appName = uiUtils.getCurrentHash().split('/', 2)[1];

            this.parentRender(function () {
                self.$el.find("#cancelButton").attr("href","#app/"+appName+"/policies/");
                self.initAccordion();
                manageSubjects.render(this.data);
                manageEnvironments.render(this.data);

                if(callback) {
                    callback();
                }
            });
        },

        /**
         * Initializes accordion.
         */
        initAccordion: function () {
            this.accordion = new Accordion(this.$el.find('.accordion'));
        },

        /**
         * Opens next accordion step.
         * TODO: some validation probably will be done here
         */
        openNextStep: function (e) {
            this.accordion.setActive(this.accordion.getActive() + 1);
        }
    });

    return new EditPolicyView();
});
