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

        events: {
            'change select#selection' :       'changeType',
            'change select:not(#selection):not(.selectize)' : 'changeInput',
            'change input':                   'changeInput',
            'keyup  input':                   'changeInput',
            'autocompletechange input':       'changeInput',
            'click .buttonControl a.button':  'buttonControlClick',
            'keyup .buttonControl a.button':  'buttonControlClick',
            'click .clockpicker':             'clickClockPicker',
            'click .icon-clock':              'clickClockPicker'
        },

        data: {},
        weekdays:[
            {value:'mon', title:'Monday'},
            {value:'tue', title:'Tuesday'},
            {value:'wed', title:'Wednesday'},
            {value:'thu', title:'Thursday'},
            {value:'fri', title:'Friday'},
            {value:'sat', title:'Saturday'},
            {value:'sun', title:'Sunday'}
        ],

        mode:'append',

        render: function( schema, callback, element, itemID, itemData ) {
            var self = this;
            this.setElement(element);

            this.data = $.extend(true, [], schema);
            this.data.itemID = itemID;

            this.$el.append(uiUtils.fillTemplateWithData("templates/policy/EditEnvironmentTemplate.html", this.data));

            this.setElement('#environment_' + itemID );
            this.delegateEvents();

            if (itemData) {
                // Temporay fix, the name attribute is being added by the server after the policy is created.
                // TODO: Serverside solution required
                delete itemData.name;
                this.$el.data('itemData',itemData);
                this.$el.find('select#selection').val(itemData.type).trigger('change');
            }

            this.$el.find('select#selection').focus();

            if (callback) {callback();}
        },

        createListItem: function(allEnvironments, item){

            item.focus(); //  Required to trigger changeInput.
            this.data.conditions = allEnvironments;
            var html = uiUtils.fillTemplateWithData("templates/policy/ListItem.html", {data:item.data().itemData});
            item.find('.item-data').html(html);
            this.setElement('#'+item.attr('id'));
            this.delegateEvents();
        },

        changeInput: function(e) {

            e.stopPropagation();
            if($(e.currentTarget).parent().children('label').length === 0){
                return; // this is a temporay workaround needed for a event leakage
            }
            var label = $(e.currentTarget).parent().children('label').data().title,
                inputGroup = $(e.currentTarget).closest('div.input-group'),
                ifPopulated = false;

            this.$el.data().itemData[label] = e.currentTarget.value;

            inputGroup.find('input, select').each(function(){
               if(this.value !== ''){
                  ifPopulated = true;
               }
            });

            inputGroup.find('input, select').each(function(){
                $(this).prop('required', ifPopulated);
            });
        },

        buttonControlClick: function(e){
            if (e.type === 'keyup' && e.keyCode !== 13) { return;}
            var target = $(e.currentTarget),
                buttonControl = target.closest('ul.buttonControl'),
                label = buttonControl.prev('label').data().title;
            this.$el.data().itemData[ label ] = e.currentTarget.innerText === "true";
            buttonControl.find('li a').removeClass('selected');
            target.addClass('selected');
        },


        initDatePickers: function() {
          this.$el.find( "#startDate" ).datepicker({
            numberOfMonths: 2,
            onClose: function( selectedDate ) {
              $( "#endDate" ).datepicker( "option", "minDate", selectedDate );
            }
          });
          this.$el.find("#endDate" ).datepicker({
            numberOfMonths: 2,
            onClose: function( selectedDate ) {
              $( "#startDate" ).datepicker( "option", "maxDate", selectedDate );
            }
          });
        },

        initClockPickers: function() {
            this.$el.find('.clockpicker').each(function(){

              var clock = $(this);
              clock.clockpicker({
                  placement: 'top',
                  autoclose: true,
                  //default: 'now',
                  afterDone: function() {
                      clock.trigger('change');
                  }
              });

            });
        },

        clickClockPicker: function(e) {
            e.stopPropagation();
            var target = $(e.currentTarget).is('input') ? $(e.currentTarget) : $(e.currentTarget).prev('input');
            target.clockpicker('show');
        },

        getTimeZones: function(){

            var self = this,
                setTimeZones = function(){
                    self.$el.find('#enforcementTimeZone').autocomplete({
                        source: self.data.timezones
                    });
                };

            if (self.data.timezones) {
                setTimeZones();
                return;
            }

            $.ajax({
                url: 'timezones.json',
                dataType: "json",
                cache: true
            }).then( function(data){
                self.data.timezones = data.timezones;
                setTimeZones();
            });

        },

        initSelectize: function() {

            var self = this,
                title = '';

            this.$el.find('.selectize').each(function(){

                $(this).selectize({
                    plugins: ['restore_on_backspace'],
                    delimiter: ',',
                    persist: false,
                    create: function(input) {
                        return {
                            value: input,
                            text: input
                        };
                    },
                    onChange: function(value){
                        title = this.$input.parent().find('label')[0].dataset.title;
                        if(title !== ''){
                             self.$el.data().itemData[title] = value;
                        }
                    }
                });

            });
        },

        changeType: function(e) {
            e.stopPropagation();
            var self         = this,
                itemData     = {},
                schema       = {},
                html         = '',
                returnVal    = '',
                selectize    = false,
                selectedType = e.target.value,
                delay        = self.$el.find('.field-float-pattern').length > 0 ? 500 : 0,
                buildHTML    = function(schemaProps) {

                    var count = 0,
                        pattern   = null;

                    returnVal = '';

                    if (itemData.type === "SimpleTime") {
                        // 'SimpleTime' is a special case and requires its own template. 
                        // This is because the endpoint only describes the inputs as strings, however in order to build a helpful UI we need to do more.
                        returnVal += uiUtils.fillTemplateWithData("templates/policy/ConditionAttrTimeDate.html", {
                            weekdays:self.weekdays,
                            data:itemData,
                            id:count
                        });

                    } else {

                        returnVal += '<div class="no-float">';

                        _.map(schemaProps, function(value, key) {

                            returnVal += '\n';

                            if (value.type === 'string' || value.type === 'number' || value.type === 'integer') {


                                if (value["enum"]) {

                                    returnVal +=  uiUtils.fillTemplateWithData("templates/policy/ConditionAttrEnum.html", {data:value, title:key, selected:itemData[key], id:count});

                                } else {
                                    if (key === 'startIp' || key === 'endIp') {
                                       pattern="^(((25[0-5])|(2[0-4]\\d)|(1\\d\\d)|([1-9]?\\d)))((\\.((25[0-5])|(2[0-4]\\d)|(1\\d\\d)|([1-9]?\\d))){3}|(\\.((25[0-5])|(2[0-4]\\d)|(1\\d\\d)|([1-9]?\\d))){5})";
                                    } else if( value.type === 'number' ){
                                       pattern="[-+]?[0-9]*[.,]?[0-9]+";
                                    } else if( value.type === 'integer' ){
                                       pattern="\\d+";
                                    } else {
                                       pattern = null;
                                    }
                                    returnVal +=  uiUtils.fillTemplateWithData("templates/policy/ConditionAttrString.html", {data:itemData[key], title:key, id:count, pattern:pattern});
                                }

                            } else if (value.type === 'boolean' ) {
                                // Ignoring the required property and assumming it defaults to false. See AME-4324
                                returnVal +=  uiUtils.fillTemplateWithData("templates/policy/ConditionAttrBoolean.html", {data:value, title:key, selected:itemData[key]});

                            } else if (value.type === 'array' ) {
                                returnVal +=  uiUtils.fillTemplateWithData("templates/policy/ConditionAttrArray.html", {data:itemData[key], title:key, id:count, tempData:self.weekdays});  

                            } else if (value.type === 'object' ) {
                                // TODO
                                console.error('TODO : Data type object');
                                returnVal +=  uiUtils.fillTemplateWithData("templates/policy/ConditionAttrString.html", { title:key, id:count});                      

                            } else {
                                console.error('Unexpected data type:',key,value);

                            }

                            count++;
                        });

                        returnVal += '</div>';
                    }

                    return returnVal;
                };

            schema =  _.findWhere(this.data.conditions, {title: selectedType}) || {};

            if (this.$el.data().itemData && this.$el.data().itemData.type === selectedType) {
                itemData = this.$el.data().itemData;
            } else {
                itemData.type = schema.title;
                _.map(schema.config.properties, function(value,key) {
                    switch (value.type) {
                        case 'string':
                            itemData[key] = '';
                        break;
                        case 'number':
                        case 'integer':
                            itemData[key] = 0;
                        break;
                        case 'boolean':
                            itemData[key] = false;
                        break;
                        case 'array':
                            itemData[key] = [];
                        break;
                        case 'object':
                            itemData[key] = {};
                        break;
                        default:
                            console.error('Unexpected data type:',key,value);
                        break;
                    }
                });
                self.$el.data('itemData',itemData);
            }

            if (itemData) {

                html = buildHTML(schema.config.properties);

                // all items expect the title selector are contained inside either a no-float or a clear-left
                this.$el.find('.no-float').fadeOut(500);
                this.$el.find('.clear-left').fadeOut(500);

                this.$el.find('.field-float-pattern')
                    .find('label').removeClass('showLabel')
                    .next('input') 
                    .addClass('placeholderText');
                    //.prop('readonly', true);

                this.$el.find('.field-float-select select:not(#selection)')
                    .addClass('placeholderText')
                    .prev('label')
                    .removeClass('showLabel');

                // setTimeout needed to delay transitions.
                setTimeout( function() {

                    self.$el.find('.no-float').remove();
                    self.$el.find('.clear-left').remove();

                    self.$el.find('#typeSelector').after( html );

                    if (itemData.type === "SimpleTime") {
                        self.initClockPickers();
                        self.initDatePickers();
                        self.getTimeZones();
                    } else {

                        selectize =  _.find(schema.config.properties, function(item){
                            return item.type === 'array'; 
                        });

                        if(selectize){
                            self.initSelectize();
                        }  

                    }

                    setTimeout( function() {

                        self.$el.find('.field-float-pattern')
                            .find('label').addClass('showLabel')
                            .next('input, div input')
                            .addClass('placeholderText')
                            .prop('readonly', false);

                        self.$el.find('.field-float-select select:not(#selection)')
                            .removeClass('placeholderText')
                            .prop('readonly', false)
                            .prev('label')
                            .addClass('showLabel');

                        self.delegateEvents();


                    }, 10);
                }, delay);

            }
        }

    });

    return EditEnvironmentView; 

});
