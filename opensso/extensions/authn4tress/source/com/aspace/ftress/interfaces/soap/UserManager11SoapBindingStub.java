/**
 * UserManager11SoapBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.soap;

public class UserManager11SoapBindingStub extends org.apache.axis.client.Stub implements com.aspace.ftress.interfaces.soap.UserManager {
    private java.util.Vector cachedSerClasses = new java.util.Vector();
    private java.util.Vector cachedSerQNames = new java.util.Vector();
    private java.util.Vector cachedSerFactories = new java.util.Vector();
    private java.util.Vector cachedDeserFactories = new java.util.Vector();

    static org.apache.axis.description.OperationDesc [] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[9];
        _initOperationDesc1();
    }

    private static void _initOperationDesc1(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getUsers");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserSearchResults"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.UserSearchResults.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getUsersReturn"));
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
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("getUser");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "User"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.User.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getUserReturn"));
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
        oper.setName("createUser");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "user"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "User"), com.aspace.ftress.interfaces.ftress.DTO.User.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "User"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.User.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "createUserReturn"));
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
        oper.setName("updateUserAttributes");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "attributes"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_Attribute"), com.aspace.ftress.interfaces.ftress.DTO.Attribute[].class, false, false);
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
        _operations[3] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteUser");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
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
        _operations[4] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("deleteUsers");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode[].class, false, false);
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
        oper.setName("getContactHistory");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "externalReference"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_ContactHistory"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.ContactHistory[].class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "getContactHistoryReturn"));
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
        oper.setName("searchUsers");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "searchCriteria"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserSearchCriteria"), com.aspace.ftress.interfaces.ftress.DTO.UserSearchCriteria.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "securityDomain"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain"), com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserSearchResults"));
        oper.setReturnClass(com.aspace.ftress.interfaces.ftress.DTO.UserSearchResults.class);
        oper.setReturnQName(new javax.xml.namespace.QName("", "searchUsersReturn"));
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
        oper.setName("updateUserExternalReference");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "alsi"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI"), com.aspace.ftress.interfaces.ftress.DTO.ALSI.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "channelCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ChannelCode"), com.aspace.ftress.interfaces.ftress.DTO.ChannelCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "userCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
        oper.addParameter(param);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "newRefCode"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode"), com.aspace.ftress.interfaces.ftress.DTO.UserCode.class, false, false);
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
        _operations[8] = oper;

    }

    public UserManager11SoapBindingStub() throws org.apache.axis.AxisFault {
         this(null);
    }

    public UserManager11SoapBindingStub(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
         this(service);
         super.cachedEndpoint = endpointURL;
    }

    public UserManager11SoapBindingStub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
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
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ALSI");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.ALSI.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "Attribute");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.Attribute.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "AttributeTypeCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.AttributeTypeCode.class;
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

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ContactHistory");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.ContactHistory.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "GroupCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.GroupCode.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "Parameter");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.Parameter.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "PrimeUser");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.PrimeUser.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SearchCriteria");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.SearchCriteria.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SearchResults");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.SearchResults.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "SecurityDomain");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "User");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.User.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.UserCode.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserSearchCriteria");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.UserSearchCriteria.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserSearchResults");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.UserSearchResults.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "ALSIInvalidException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "BusinessException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.BusinessException.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://exception.DTO.ftress.interfaces.ftress.aspace.com", "CreateDuplicateException");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException.class;
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

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_soapenc_string");
            cachedSerQNames.add(qName);
            cls = java.lang.String[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/encoding/", "string");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_Attribute");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.Attribute[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "Attribute");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_ContactHistory");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.ContactHistory[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "ContactHistory");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_GroupCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.GroupCode[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "GroupCode");
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

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_User");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.User[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "User");
            qName2 = null;
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "ArrayOf_tns1_UserCode");
            cachedSerQNames.add(qName);
            cls = com.aspace.ftress.interfaces.ftress.DTO.UserCode[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://DTO.ftress.interfaces.ftress.aspace.com", "UserCode");
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

    public com.aspace.ftress.interfaces.ftress.DTO.UserSearchResults getUsers(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getUsers"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.UserSearchResults) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.UserSearchResults) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.UserSearchResults.class);
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

    public com.aspace.ftress.interfaces.ftress.DTO.User getUser(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getUser"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.User) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.User) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.User.class);
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

    public com.aspace.ftress.interfaces.ftress.DTO.User createUser(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.User user, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "createUser"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, user, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.User) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.User) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.User.class);
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

    public void updateUserAttributes(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.Attribute[] attributes, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateUserAttributes"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, attributes, securityDomain});

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

    public void deleteUser(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "deleteUser"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, securityDomain});

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

    public void deleteUsers(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode[] userCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "deleteUsers"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, securityDomain});

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

    public com.aspace.ftress.interfaces.ftress.DTO.ContactHistory[] getContactHistory(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode externalReference, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[6]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "getContactHistory"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, externalReference, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.ContactHistory[]) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.ContactHistory[]) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.ContactHistory[].class);
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

    public com.aspace.ftress.interfaces.ftress.DTO.UserSearchResults searchUsers(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserSearchCriteria searchCriteria, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[7]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "searchUsers"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, searchCriteria, securityDomain});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.aspace.ftress.interfaces.ftress.DTO.UserSearchResults) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.aspace.ftress.interfaces.ftress.DTO.UserSearchResults) org.apache.axis.utils.JavaUtils.convert(_resp, com.aspace.ftress.interfaces.ftress.DTO.UserSearchResults.class);
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

    public void updateUserExternalReference(com.aspace.ftress.interfaces.ftress.DTO.ALSI alsi, com.aspace.ftress.interfaces.ftress.DTO.ChannelCode channelCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode userCode, com.aspace.ftress.interfaces.ftress.DTO.UserCode newRefCode, com.aspace.ftress.interfaces.ftress.DTO.SecurityDomain securityDomain) throws java.rmi.RemoteException, com.aspace.ftress.interfaces.ftress.DTO.exception.InternalException, com.aspace.ftress.interfaces.ftress.DTO.exception.ObjectNotFoundException, com.aspace.ftress.interfaces.ftress.DTO.exception.NoFunctionPrivilegeException, com.aspace.ftress.interfaces.ftress.DTO.exception.CreateDuplicateException, com.aspace.ftress.interfaces.ftress.DTO.exception.InvalidParameterException, com.aspace.ftress.interfaces.ftress.DTO.exception.ALSIInvalidException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[8]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("");
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "updateUserExternalReference"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {alsi, channelCode, userCode, newRefCode, securityDomain});

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

}
