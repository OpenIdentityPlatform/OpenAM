/**
 * Authenticator11SoapBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.soap;

public class Authenticator11SoapBindingStub extends org.apache.axis.client.Stub implements com.aspace.ftress.interfaces.soap.Authenticator {
    private java.util.Vector cachedSerClasses = new java.util.Vector();
    private java.util.Vector cachedSerQNames = new java.util.Vector();
    private java.util.Vector cachedSerFactories = new java.util.Vector();
    private java.util.Vector cachedDeserFactories = new java.util.Vector();

    static org.apache.axis.description.OperationDesc [] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[25];
        _initOperationDesc1();
        _initOperationDesc2();
        _initOperationDesc3();
    }

    private static void _initOperationDesc1(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("ping");
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("logout");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
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
        _operations[1] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getMDAuthenticationPrompts");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationPrompts"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getMDAuthenticationPromptsReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("getAuthenticationChallenge");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationChallenge"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.device.AuthenticationChallenge.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getAuthenticationChallengeReturn"));
        oper.setStyle(org.apache.axis.constants.Style.RPC);
        oper.setUse(org.apache.axis.constants.Use.ENCODED);
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticationException"), 
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "PasswordExpiredException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        _operations[3] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getAuthenticationParameters");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns2_AuthenticationRequestParameter"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getAuthenticationParametersReturn"));
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
        _operations[4] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getPasswordSeedPositions");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "numberOfSeedsRequested"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SeedPositions"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.SeedPositions.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getPasswordSeedPositionsReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "PasswordExpiredException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("getSeededMDAuthenticationPrompts");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authenticationTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "numSeeds"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationPrompts"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getSeededMDAuthenticationPromptsReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("getSessionData");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSISession"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.ALSISession.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getSessionDataReturn"));
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
        oper.setName("indirectGetPasswordSeedPositions");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "numberOfSeedsRequested"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SeedPositions"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.SeedPositions.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "indirectGetPasswordSeedPositionsReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "PasswordExpiredException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("indirectGetSeededMDAuthenticationPrompts");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "externalReference"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "authTypeCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode"), com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "numSeeds"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationPrompts"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "indirectGetSeededMDAuthenticationPromptsReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("indirectLogout");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "palsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
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
        oper.setName("indirectPrimaryAuthenticateDevice");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticationRequest"), com.aspace.ftress.interfaces.ftress.DTO.DeviceAuthenticationRequest.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "indirectPrimaryAuthenticateDeviceReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticationException"), 
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "PasswordExpiredException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTierException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("indirectPrimaryAuthenticateMD");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationRequest"), com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationRequest.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "indirectPrimaryAuthenticateMDReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTierException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("indirectPrimaryAuthenticateUP");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticationRequest"), com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticationRequest.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "indirectPrimaryAuthenticateUPReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "PasswordExpiredException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTierException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("indirectRemoveSecondaryAuthentication");
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
        _operations[14] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("indirectSecondaryAuthenticateDevice");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "palsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticationRequest"), com.aspace.ftress.interfaces.ftress.DTO.DeviceAuthenticationRequest.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "indirectSecondaryAuthenticateDeviceReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticationException"), 
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "PasswordExpiredException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTierException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("indirectSecondaryAuthenticateMD");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "palsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationRequest"), com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationRequest.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "indirectSecondaryAuthenticateMDReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTierException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("indirectSecondaryAuthenticateUP");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "palsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticationRequest"), com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticationRequest.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "indirectSecondaryAuthenticateUPReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "PasswordExpiredException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTierException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("primaryAuthenticateDevice");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticationRequest"), com.aspace.ftress.interfaces.ftress.DTO.DeviceAuthenticationRequest.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "primaryAuthenticateDeviceReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticationException"), 
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "PasswordExpiredException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTierException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("primaryAuthenticateMD");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationRequest"), com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationRequest.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "primaryAuthenticateMDReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTierException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        _operations[19] = oper;

    }

    private static void _initOperationDesc3(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("primaryAuthenticateUP");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticationRequest"), com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticationRequest.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "primaryAuthenticateUPReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "PasswordExpiredException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTierException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "InvalidParameterException"), 
                      true
                     ));
        _operations[20] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("removeSecondaryAuthentication");
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
        _operations[21] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("secondaryAuthenticateDevice");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticationRequest"), com.aspace.ftress.interfaces.ftress.DTO.DeviceAuthenticationRequest.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "secondaryAuthenticateDeviceReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticationException"), 
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "PasswordExpiredException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTierException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("secondaryAuthenticateMD");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationRequest"), com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationRequest.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "secondaryAuthenticateMDReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "PasswordExpiredException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTierException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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
        oper.setName("secondaryAuthenticateUP");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticationRequest"), com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticationRequest.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "secondaryAuthenticateUPReturn"));
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
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "PasswordExpiredException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTierException"), 
                      true
                     ));
        oper.addFault(new org.apache.axis.description.FaultDesc(
                      new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "fault"),
                      "com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException",
                      new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException"), 
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

    }

    public Authenticator11SoapBindingStub() throws org.apache.axis.AxisFault {
         this(null);
    }

    public Authenticator11SoapBindingStub(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
         this(service);
         super.cachedEndpoint = endpointURL;
    }

    public Authenticator11SoapBindingStub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
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
            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationChallenge");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.AuthenticationChallenge.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "Device");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.Device.class;
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

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSISession");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.ALSISession.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationOccurrence");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticationOccurrence.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationParameter");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticationParameter.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationRequest");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequest.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationRequestParameter");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponse");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponseParameter");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponseParameter.class;
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

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "Code");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.Code.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticationRequest");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.DeviceAuthenticationRequest.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "GroupCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.GroupCode.class;
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

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationAnswer");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationAnswer.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationPrompt");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompt.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationPrompts");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationRequest");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationRequest.class;
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

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SeedPositions");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.SeedPositions.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UPAuthenticationRequest");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticationRequest.class;
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

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTierException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "BusinessException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.BusinessException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "DeviceAuthenticationException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException.class;
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

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "PasswordExpiredException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "SeedingException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns2_AuthenticationOccurrence");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticationOccurrence[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationOccurrence");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns2_AuthenticationRequestParameter");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationRequestParameter");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns2_AuthenticationResponseParameter");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponseParameter[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationResponseParameter");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns2_AuthenticationTypeCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AuthenticationTypeCode");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns2_MDAuthenticationAnswer");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationAnswer[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationAnswer");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns2_MDAuthenticationPrompt");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompt[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDAuthenticationPrompt");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns2_MDPromptCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.MDPromptCode[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "MDPromptCode");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns2_Parameter");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.Parameter[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "Parameter");
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

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns3_DeviceGroupCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.device.DeviceGroupCode[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://device.DTO.ftress.interfaces.ftress.aspace.com", "DeviceGroupCode");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_xsd_int");
            cachedSerQNames.add(qName);
            cls = int[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int");
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

    public void ping() throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ping"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {});

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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void logout(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "logout"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, securityDomain});

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

    public com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts getMDAuthenticationPrompts(com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getMDAuthenticationPrompts"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {userCode, channelCode, authTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public com.aspace.ftress.interfaces.ftress.DTO.device.AuthenticationChallenge getAuthenticationChallenge(com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getAuthenticationChallenge"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {channelCode, userCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.device.AuthenticationChallenge) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.device.AuthenticationChallenge) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.device.AuthenticationChallenge.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
    if (axisFaultException.detail != null) {
        if (axisFaultException.detail instanceof java.rmi.RemoteException) {
              throw (java.rmi.RemoteException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException) axisFaultException.detail;
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[] getAuthenticationParameters(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getAuthenticationParameters"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, authenticationTypeCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationRequestParameter[].class);
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

    public com.aspace.ftress.interfaces.ftress.DTO.SeedPositions getPasswordSeedPositions(com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authTypeCode, int numberOfSeedsRequested, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getPasswordSeedPositions"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {channelCode, userCode, authTypeCode, new java.lang.Integer(numberOfSeedsRequested), securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.SeedPositions) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.SeedPositions) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.SeedPositions.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts getSeededMDAuthenticationPrompts(com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, int numSeeds, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[6]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getSeededMDAuthenticationPrompts"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {userCode, channelCode, authenticationTypeCode, new java.lang.Integer(numSeeds), securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public com.aspace.ftress.interfaces.ftress.DTO.ALSISession getSessionData(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[7]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getSessionData"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.ALSISession) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.ALSISession) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.ALSISession.class);
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

    public com.aspace.ftress.interfaces.ftress.DTO.SeedPositions indirectGetPasswordSeedPositions(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authTypeCode, int numberOfSeedsRequested, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[8]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "indirectGetPasswordSeedPositions"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, authTypeCode, new java.lang.Integer(numberOfSeedsRequested), securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.SeedPositions) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.SeedPositions) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.SeedPositions.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts indirectGetSeededMDAuthenticationPrompts(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.UserCode externalReference, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authTypeCode, int numSeeds, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[9]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "indirectGetSeededMDAuthenticationPrompts"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, externalReference, channelCode, authTypeCode, new java.lang.Integer(numSeeds), securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationPrompts.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public void indirectLogout(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ALSI palsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[10]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "indirectLogout"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, palsi, channelCode, securityDomain});

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

    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse indirectPrimaryAuthenticateDevice(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.DeviceAuthenticationRequest request, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[11]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "indirectPrimaryAuthenticateDevice"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, request, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException) axisFaultException.detail;
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse indirectPrimaryAuthenticateMD(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationRequest request, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[12]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "indirectPrimaryAuthenticateMD"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, request, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse indirectPrimaryAuthenticateUP(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticationRequest request, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[13]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "indirectPrimaryAuthenticateUP"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, request, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public void indirectRemoveSecondaryAuthentication(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ALSI palsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[14]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "indirectRemoveSecondaryAuthentication"));

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

    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse indirectSecondaryAuthenticateDevice(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ALSI palsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.DeviceAuthenticationRequest request, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[15]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "indirectSecondaryAuthenticateDevice"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, palsi, channelCode, request, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException) axisFaultException.detail;
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse indirectSecondaryAuthenticateMD(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ALSI palsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationRequest request, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[16]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "indirectSecondaryAuthenticateMD"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, palsi, channelCode, request, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse indirectSecondaryAuthenticateUP(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ALSI palsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticationRequest request, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[17]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "indirectSecondaryAuthenticateUP"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, palsi, channelCode, request, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse primaryAuthenticateDevice(com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.DeviceAuthenticationRequest request, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[18]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "primaryAuthenticateDevice"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {channelCode, request, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException) axisFaultException.detail;
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse primaryAuthenticateMD(com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationRequest request, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[19]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "primaryAuthenticateMD"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {channelCode, request, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse primaryAuthenticateUP(com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticationRequest request, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[20]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "primaryAuthenticateUP"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {channelCode, request, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException) axisFaultException.detail;
         }
   }
  throw axisFaultException;
}
    }

    public void removeSecondaryAuthentication(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationTypeCode authenticationTypeCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[21]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "removeSecondaryAuthentication"));

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

    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse secondaryAuthenticateDevice(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.DeviceAuthenticationRequest request, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[22]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "secondaryAuthenticateDevice"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, request, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.DeviceAuthenticationException) axisFaultException.detail;
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse secondaryAuthenticateMD(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.MDAuthenticationRequest request, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[23]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "secondaryAuthenticateMD"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, request, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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

    public com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse secondaryAuthenticateUP(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UPAuthenticationRequest request, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidChannelException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException, com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException, com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[24]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "secondaryAuthenticateUP"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, request, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.AuthenticationResponse.class);
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
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.PasswordExpiredException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.AuthenticationTierException) axisFaultException.detail;
         }
        if (axisFaultException.detail instanceof com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) {
              throw (com.aspace.ftress.interfaces.ftress.DTO.exception.SeedingException) axisFaultException.detail;
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
