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
            subjects: [
            {
                type: "Virtual Subject",
                list: [
                    { name: "iplanet-am-session-get-valid-sessions" },
                    { name: "sunIdentityServerPPFacadegreetmesound" },
                    { name: "iplanet-am-user-password-reset-question-answer" },
                    { name: "iplanet-am-user-admin-start-dn" },
                    { name: "iplanet-am-user-success-url" },
                    { name: "sunIdentityServerPPDemographicsDisplayLanguage" },
                    { name: "iplanet-am-user-federation-info" }
                ]
            },
            {
                type: "Attribute Subject",
                list: [

                    { name: "sunIdentityServerPPCommonNameMN" },
                    { name: "iplanet-am-session-get-valid-sessions" },
                    { name: "sunIdentityServerPPFacadegreetmesound" },
                    { name: "iplanet-am-user-password-reset-question-answer" },
                    { name: "iplanet-am-user-admin-start-dn" }
                ]
            },
            {
                type: "Identity Repository User",
                list: [
                    { name: "sunIdentityServerPPInformalName" },
                    { name: "sunIdentityServerPPFacadeGreetSound" },
                    { name: "sunIdentityServerPPLegalIdentityGender" }
                ]
            },
            {
                type: "Identity Repository Group",
                list: [
                    { name: "iplanet-am-user-password-reset-question-answer" },
                    { name: "iplanet-am-user-admin-start-dn" },
                    { name: "iplanet-am-user-success-url" },
                    { name: "sunIdentityServerPPDemographicsDisplayLanguage" },
                    { name: "iplanet-am-user-federation-info" }
                ]
            }],


            "result" :
            [
              {
                "title" : "OR",
                "logical" : true,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "conditions" : {
                      "type" : "array",
                      "items" : {
                        "type" : "any"
                      }
                    }
                  }
                }
              },
              {
                "title" : "Time",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "startTime" : {
                      "type" : "string"
                    },
                    "endTime" : {
                      "type" : "string"
                    },
                    "startDay" : {
                      "type" : "string"
                    },
                    "endDay" : {
                      "type" : "string"
                    },
                    "startDate" : {
                      "type" : "string"
                    },
                    "endDate" : {
                      "type" : "string"
                    },
                    "enforcementTimeZone" : {
                      "type" : "string"
                    }
                  }
                }
              },
              {
                "title" : "IP",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "startIp" : {
                      "type" : "string"
                    },
                    "endIp" : {
                      "type" : "string"
                    }
                  }
                }
              },
              {
                "title" : "StringAttribute",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "attributeName" : {
                      "type" : "string"
                    },
                    "value" : {
                      "type" : "string"
                    },
                    "caseSensitive" : {
                      "type" : "boolean",
                      "required" : true
                    }
                  }
                }
              },
              {
                "title" : "Policy",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "className" : {
                      "type" : "string"
                    },
                    "properties" : {
                      "type" : "object"
                    }
                  }
                }
              },
              {
                "title" : "DNSName",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "domainNameMask" : {
                      "type" : "string"
                    }
                  }
                }
              },
              {
                "title" : "AttributeLookup",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "key" : {
                      "type" : "string"
                    },
                    "value" : {
                      "type" : "string"
                    }
                  }
                }
              },
              {
                "title" : "AND",
                "logical" : true,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "conditions" : {
                      "type" : "array",
                      "items" : {
                        "type" : "any"
                      }
                    }
                  }
                }
              },
              {
                "title" : "NumericAttribute",
                "logical" : false,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "attributeName" : {
                      "type" : "string"
                    },
                    "operator" : {
                      "type" : "string",
                      "enum" : [ "LESS_THAN", "LESS_THAN_OR_EQUAL", "EQUAL", "GREATER_THAN_OR_EQUAL", "GREATER_THAN" ]
                    },
                    "value" : {
                      "type" : "number"
                    }
                  }
                }
              },
              {
                "title" : "NOT",
                "logical" : true,
                "config" : {
                  "type" : "object",
                  "properties" : {
                    "condition" : {
                      "type" : "object",
                      "properties" : {
                      }
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
