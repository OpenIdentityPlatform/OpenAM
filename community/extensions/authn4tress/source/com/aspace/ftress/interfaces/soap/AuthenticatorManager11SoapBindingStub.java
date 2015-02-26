/**
 * AuthenticatorManager11SoapBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.soap;

public class AuthenticatorManager11SoapBindingStub extends org.apache.axis.client.Stub implements com.aspace.ftress.interfaces.soap.AuthenticatorManager {
    private java.util.Vector cachedSerClasses = new java.util.Vector();
    private java.util.Vector cachedSerQNames = new java.util.Vector();
    private java.util.Vector cachedSerFactories = new java.util.Vector();
    private java.util.Vector cachedDeserFactories = new java.util.Vector();

    static org.apache.axis.description.OperationDesc [] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[43];
        _initOperationDesc1();
        _initOperationDesc2();
        _initOperationDesc3();
        _initOperationDesc4();
        _initOperationDesc5();
    }

    private static void _initOperationDesc1(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("cloneAuthenticator");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "upAuthenticatorClone"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticatorClone"), com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticatorClone.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ConstraintFailedException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidChannelException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorCloneException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorCloneException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "CreateDuplicateException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("createDeviceAuthenticator");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "tokenAuthenticator"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticator"), com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "criteria"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceSearchCriteria"), com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidChannelException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ConstraintFailedException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "DeviceException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "CreateDuplicateException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[1] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("createMDAuthenticator");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "mdAuthenticator"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticator"), com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ConstraintFailedException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidChannelException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.MDAnswerException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "MDAnswerException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "CreateDuplicateException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[2] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getMDAuthenticatorForCreationForUser");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticator"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getMDAuthenticatorForCreationForUserReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[3] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("createUPAuthenticator");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "upAuthenticator"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticator"), com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidChannelException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ConstraintFailedException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "CreateDuplicateException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[4] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteDeviceAuthenticator");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[5] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getDeviceAuthenticator");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticator"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getDeviceAuthenticatorReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[6] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteDeviceAuthenticators");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCodes"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode[].class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[7] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteMDAuthenticator");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[8] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteMDAuthenticators");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCodes"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode[].class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[9] = oper;

    }

    private static void _initOperationDesc2(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteUPAuthenticator");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[10] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteUPAuthenticators");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCodes"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode[].class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[11] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getAllDeviceAuthenticatorsForUser");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns3_DeviceAuthenticator"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getAllDeviceAuthenticatorsForUserReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[12] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getAllUPAuthenticatorsForUser");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_UPAuthenticator"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getAllUPAuthenticatorsForUserReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[13] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getAllUserMDAuthenticators");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_MDAuthenticator"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getAllUserMDAuthenticatorsReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[14] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getDeviceAuthenticatorForCreation");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticator"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getDeviceAuthenticatorForCreationReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[15] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getDeviceAuthenticators");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "id"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceId"), com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns3_DeviceAuthenticator"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getDeviceAuthenticatorsReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[16] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getMDAuthenticatorForCreation");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticator"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getMDAuthenticatorForCreationReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[17] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getUPAuthenticator");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticator"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getUPAuthenticatorReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidChannelException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[18] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getUPAuthenticatorForCreation");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticator"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getUPAuthenticatorForCreationReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[19] = oper;

    }

    private static void _initOperationDesc3(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getUserMDAuthenticator");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticator"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getUserMDAuthenticatorReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[20] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("indirectResetUPAuthenticatorFailedAuthenticationCount");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "palsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[21] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("resetDeviceAuthenticatorFailedAuthenticationCount");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[22] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("resetMDAuthenticatorFailedAuthenticationCount");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[23] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("resetUPAuthenticatorFailedAuthenticationCount");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[24] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateAuthenticatorPrimaryBlockedChannels");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "primaryChannelBlocks"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode[].class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidChannelException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[25] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateAuthenticatorSecondaryBlockedChannels");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "secondaryChannelBlocks"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode[].class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidChannelException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        _operations[26] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateDeviceAuthenticatorMaxUsages");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "expiryThreshold"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[27] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateDeviceAuthenticatorStatus");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "status"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorStatus"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticatorStatus.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[28] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateDeviceAuthenticatorValidFrom");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "validFrom"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[29] = oper;

    }

    private static void _initOperationDesc4(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateDeviceAuthenticatorValidPeriod");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "validFrom"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "validTo"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[30] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateDeviceAuthenticatorValidTo");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "validTo"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[31] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateMDAuthenticatorStatus");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "status"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorStatus"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticatorStatus.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[32] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateMDAuthenticatorValidFrom");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "validFrom"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[33] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateMDAuthenticatorValidPeriod");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "validFrom"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "validTo"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[34] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateMDAuthenticatorValidTo");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "validTo"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[35] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateUPAuthenticatorMaxUsages");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "expiryThreshold"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[36] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateUPAuthenticatorStatus");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "status"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorStatus"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticatorStatus.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[37] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateUPAuthenticatorValidFrom");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "validFrom"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[38] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateUPAuthenticatorValidPeriod");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "validFrom"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "validTo"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[39] = oper;

    }

    private static void _initOperationDesc5(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("updateUPAuthenticatorValidTo");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "validTo"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "dateTime"), java.util.Calendar.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[40] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("indirectResetMDAuthenticatorFailedAuthenticationCount");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "palsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[41] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("indirectResetDeviceAuthenticatorFailedAuthenticationCount");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "palsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException"), 
                      true
                     ));
        _operations[42] = oper;

    }

    public AuthenticatorManager11SoapBindingStub() throws org.apache.axis.AxisFault {
         this(null);
    }

    public AuthenticatorManager11SoapBindingStub(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
         this(service);
         super.cachedEndpoint = endpointURL;
    }

    public AuthenticatorManager11SoapBindingStub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
        if (service == null) {
            super.service = new org.apache.axis.client.Service();
        } else {
            super.service = service;
        }
        ((org.apache.axis.client.Service)super.service).setTypeMappingVersion("1.2");
            java.lang.Class cls;
            javax.xml.namespace.QName qName;
            javax.xml.namespace.QName qName2;
            java.lang.Class beansf = org.apache.axis.encoding.ser.BeanSerializerFactory.class;
            java.lang.Class beandf = org.apache.axis.encoding.ser.BeanDeserializerFactory.class;
            java.lang.Class enumsf = org.apache.axis.encoding.ser.EnumSerializerFactory.class;
            java.lang.Class enumdf = org.apache.axis.encoding.ser.EnumDeserializerFactory.class;
            java.lang.Class arraysf = org.apache.axis.encoding.ser.ArraySerializerFactory.class;
            java.lang.Class arraydf = org.apache.axis.encoding.ser.ArrayDeserializerFactory.class;
            java.lang.Class simplesf = org.apache.axis.encoding.ser.SimpleSerializerFactory.class;
            java.lang.Class simpledf = org.apache.axis.encoding.ser.SimpleDeserializerFactory.class;
            java.lang.Class simplelistsf = org.apache.axis.encoding.ser.SimpleListSerializerFactory.class;
            java.lang.Class simplelistdf = org.apache.axis.encoding.ser.SimpleListDeserializerFactory.class;
            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "Device");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.Device.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticator");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAutoSynchronisationRequest");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAutoSynchronisationRequest.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceGroupCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.DeviceGroupCode.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceId");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceManualSynchronisationRequest");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.DeviceManualSynchronisationRequest.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceSearchCriteria");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceStatus");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.DeviceStatus.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceSynchronisationRequest");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSynchronisationRequest.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceTypeCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.DeviceTypeCode.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.ALSI.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationStatistics");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticationStatistics.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "Authenticator");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.Authenticator.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorStatus");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticatorStatus.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelsBlocked");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.ChannelsBlocked.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "Code");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.Code.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ConstraintFailure");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.ConstraintFailure.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "Id");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.Id.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAnswer");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDAnswer.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticator");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDPrompt");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDPrompt.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDPromptCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDPromptCode.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "Parameter");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.Parameter.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SearchCriteria");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.SearchCriteria.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticator");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticatorClone");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticatorClone.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.UserCode.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorCloneException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorCloneException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticatorException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "BusinessException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.BusinessException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ConstraintFailedException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "CreateDuplicateException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "DeviceException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "FTRESSException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.FTRESSException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InternalException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidChannelException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "MDAnswerException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.MDAnswerException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "NoFunctionPrivilegeException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ObjectNotFoundException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_AuthenticationTypeCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_ChannelCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.ChannelCode[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_ConstraintFailure");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.ConstraintFailure[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ConstraintFailure");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_MDAnswer");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDAnswer[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAnswer");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_MDAuthenticator");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticator");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_MDPrompt");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDPrompt[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDPrompt");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_Parameter");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.Parameter[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "Parameter");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_UPAuthenticator");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticator");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns3_Device");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.Device[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "Device");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns3_DeviceAuthenticator");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticator");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns3_DeviceGroupCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.DeviceGroupCode[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceGroupCode");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_xsd_boolean");
            cachedSerQNames.add(qName);
            cls = boolean[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

    }

    protected org.apache.axis.client.Call createCall() throws java.rmi.RemoteException {
        try {
            org.apache.axis.client.Call _call = super._createCall();
            if (super.maintainSessionSet) {
                _call.setMaintainSession(super.maintainSession);
            }
            if (super.cachedUsername != null) {
                _call.setUsername(super.cachedUsername);
            }
            if (super.cachedPassword != null) {
                _call.setPassword(super.cachedPassword);
            }
            if (super.cachedEndpoint != null) {
                _call.setTargetEndpointAddress(super.cachedEndpoint);
            }
            if (super.cachedTimeout != null) {
                _call.setTimeout(super.cachedTimeout);
            }
            if (super.cachedPortName != null) {
                _call.setPortName(super.cachedPortName);
            }
            java.util.Enumeration keys = super.cachedProperties.keys();
            while (keys.hasMoreElements()) {
                java.lang.String key = (java.lang.String) keys.nextElement();
                _call.setProperty(key, super.cachedProperties.get(key));
            }
            // All the type mapping information is registered
            // when the first call is made.
            // The type mapping information is actually registered in
            // the TypeMappingRegistry of the service, which
            // is the reason why registration is only needed for the first call.
            synchronized (this) {
                if (firstCall()) {
                    // must set encoding style before registering serializers
                    _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
                    _call.setEncodingStyle(org.apache.axis.Constants.URI_SOAP11_ENC);
                    for (int i = 0; i < cachedSerFactories.size(); ++i) {
                        java.lang.Class cls = (java.lang.Class) cachedSerClasses.get(i);
                        javax.xml.namespace.QName qName =
                                (javax.xml.namespace.QName) cachedSerQNames.get(i);
                        java.lang.Object x = cachedSerFactories.get(i);
                        if (x instanceof Class) {
                            java.lang.Class sf = (java.lang.Class)
                                 cachedSerFactories.get(i);
                            java.lang.Class df = (java.lang.Class)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                        else if (x instanceof javax.xml.rpc.encoding.SerializerFactory) {
                            org.apache.axis.encoding.SerializerFactory sf = (org.apache.axis.encoding.SerializerFactory)
                                 cachedSerFactories.get(i);
                            org.apache.axis.encoding.DeserializerFactory df = (org.apache.axis.encoding.DeserializerFactory)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                    }
                }
            }
            return _call;
        }
        catch (java.lang.Throwable _t) {
            throw new org.apache.axis.AxisFault("Failure trying to get the Call object", _t);
        }
    }

    public void cloneAuthenticator(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticatorClone upAuthenticatorClone, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorCloneException, com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "cloneAuthenticator"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, upAuthenticatorClone, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorCloneException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorCloneException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void createDeviceAuthenticator(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator tokenAuthenticator, com.aspace.ftress.interfaces.ftress.DTO.device.DeviceSearchCriteria criteria, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException, com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "createDeviceAuthenticator"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, tokenAuthenticator, criteria, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void createMDAuthenticator(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator mdAuthenticator, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.MDAnswerException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException, com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "createMDAuthenticator"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, mdAuthenticator, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.MDAnswerException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.MDAnswerException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator getMDAuthenticatorForCreationForUser(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authTypeCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getMDAuthenticatorForCreationForUser"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, authTypeCode, userCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void createUPAuthenticator(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator upAuthenticator, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException, com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "createUPAuthenticator"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, upAuthenticator, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ConstraintFailedException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void deleteDeviceAuthenticator(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "deleteDeviceAuthenticator"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator getDeviceAuthenticator(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[6]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getDeviceAuthenticator"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void deleteDeviceAuthenticators(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode[] authenticationTypeCodes, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[7]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "deleteDeviceAuthenticators"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCodes, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void deleteMDAuthenticator(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[8]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "deleteMDAuthenticator"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void deleteMDAuthenticators(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode[] authenticationTypeCodes, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[9]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "deleteMDAuthenticators"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCodes, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void deleteUPAuthenticator(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[10]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "deleteUPAuthenticator"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void deleteUPAuthenticators(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode[] authenticationTypeCodes, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[11]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "deleteUPAuthenticators"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCodes, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator[] getAllDeviceAuthenticatorsForUser(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[12]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getAllDeviceAuthenticatorsForUser"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator[] getAllUPAuthenticatorsForUser(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[13]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getAllUPAuthenticatorsForUser"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator[] getAllUserMDAuthenticators(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[14]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getAllUserMDAuthenticators"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator getDeviceAuthenticatorForCreation(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[15]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getDeviceAuthenticatorForCreation"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator[] getDeviceAuthenticators(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.device.DeviceId id, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[16]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getDeviceAuthenticators"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, id, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.device.DeviceAuthenticator[].class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator getMDAuthenticatorForCreation(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[17]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getMDAuthenticatorForCreation"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, authTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator getUPAuthenticator(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[18]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getUPAuthenticator"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator getUPAuthenticatorForCreation(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[19]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getUPAuthenticatorForCreation"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticator.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator getUserMDAuthenticator(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[20]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getUserMDAuthenticator"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticator.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void indirectResetUPAuthenticatorFailedAuthenticationCount(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ALSI palsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[21]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "indirectResetUPAuthenticatorFailedAuthenticationCount"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, palsi, channelCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void resetDeviceAuthenticatorFailedAuthenticationCount(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[22]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "resetDeviceAuthenticatorFailedAuthenticationCount"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void resetMDAuthenticatorFailedAuthenticationCount(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[23]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "resetMDAuthenticatorFailedAuthenticationCount"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void resetUPAuthenticatorFailedAuthenticationCount(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[24]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "resetUPAuthenticatorFailedAuthenticationCount"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateAuthenticatorPrimaryBlockedChannels(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode[] primaryChannelBlocks, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[25]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateAuthenticatorPrimaryBlockedChannels"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, primaryChannelBlocks, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateAuthenticatorSecondaryBlockedChannels(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode[] secondaryChannelBlocks, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[26]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateAuthenticatorSecondaryBlockedChannels"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, secondaryChannelBlocks, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateDeviceAuthenticatorMaxUsages(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, int expiryThreshold, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[27]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateDeviceAuthenticatorMaxUsages"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, new java.lang.Integer(expiryThreshold), securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateDeviceAuthenticatorStatus(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticatorStatus status, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[28]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateDeviceAuthenticatorStatus"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, status, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateDeviceAuthenticatorValidFrom(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, java.util.Calendar validFrom, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[29]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateDeviceAuthenticatorValidFrom"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, validFrom, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateDeviceAuthenticatorValidPeriod(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, java.util.Calendar validFrom, java.util.Calendar validTo, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[30]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateDeviceAuthenticatorValidPeriod"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, validFrom, validTo, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateDeviceAuthenticatorValidTo(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, java.util.Calendar validTo, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[31]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateDeviceAuthenticatorValidTo"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, validTo, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateMDAuthenticatorStatus(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticatorStatus status, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[32]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateMDAuthenticatorStatus"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, status, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateMDAuthenticatorValidFrom(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, java.util.Calendar validFrom, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[33]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateMDAuthenticatorValidFrom"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, validFrom, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateMDAuthenticatorValidPeriod(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, java.util.Calendar validFrom, java.util.Calendar validTo, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[34]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateMDAuthenticatorValidPeriod"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, validFrom, validTo, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateMDAuthenticatorValidTo(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, java.util.Calendar validTo, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[35]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateMDAuthenticatorValidTo"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, validTo, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateUPAuthenticatorMaxUsages(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, int expiryThreshold, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[36]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateUPAuthenticatorMaxUsages"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, new java.lang.Integer(expiryThreshold), securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateUPAuthenticatorStatus(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticatorStatus status, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[37]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateUPAuthenticatorStatus"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, status, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticatorException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateUPAuthenticatorValidFrom(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, java.util.Calendar validFrom, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[38]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateUPAuthenticatorValidFrom"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, validFrom, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateUPAuthenticatorValidPeriod(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, java.util.Calendar validFrom, java.util.Calendar validTo, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[39]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateUPAuthenticatorValidPeriod"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, validFrom, validTo, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void updateUPAuthenticatorValidTo(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, java.util.Calendar validTo, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[40]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateUPAuthenticatorValidTo"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authenticationTypeCode, validTo, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void indirectResetMDAuthenticatorFailedAuthenticationCount(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ALSI palsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[41]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "indirectResetMDAuthenticatorFailedAuthenticationCount"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, palsi, channelCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void indirectResetDeviceAuthenticatorFailedAuthenticationCount(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ALSI palsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[42]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "indirectResetDeviceAuthenticatorFailedAuthenticationCount"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, palsi, channelCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        extractAttachments(_call);
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

}
