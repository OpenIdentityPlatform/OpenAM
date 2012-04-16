/**
 * UserManagerServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.aspace.ftress.interfaces.soap;

public class UserManagerServiceLocator extends org.apache.axis.client.Service implements com.aspace.ftress.interfaces.soap.UserManagerService {

    public UserManagerServiceLocator() {
    }


    public UserManagerServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public UserManagerServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for UserManager11
    private java.lang.String UserManager11_address = "http://129.156.84.96:8081/4TRESSSoap/services/UserManager-11";

    public java.lang.String getUserManager11Address() {
        return UserManager11_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String UserManager11WSDDServiceName = "UserManager-11";

    public java.lang.String getUserManager11WSDDServiceName() {
        return UserManager11WSDDServiceName;
    }

    public void setUserManager11WSDDServiceName(java.lang.String name) {
        UserManager11WSDDServiceName = name;
    }

    public com.aspace.ftress.interfaces.soap.UserManager getUserManager11() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(UserManager11_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getUserManager11(endpoint);
    }

    public com.aspace.ftress.interfaces.soap.UserManager getUserManager11(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            com.aspace.ftress.interfaces.soap.UserManager11SoapBindingStub _stub = new com.aspace.ftress.interfaces.soap.UserManager11SoapBindingStub(portAddress, this);
            _stub.setPortName(getUserManager11WSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setUserManager11EndpointAddress(java.lang.String address) {
        UserManager11_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (com.aspace.ftress.interfaces.soap.UserManager.class.isAssignableFrom(serviceEndpointInterface)) {
                com.aspace.ftress.interfaces.soap.UserManager11SoapBindingStub _stub = new com.aspace.ftress.interfaces.soap.UserManager11SoapBindingStub(new java.net.URL(UserManager11_address), this);
                _stub.setPortName(getUserManager11WSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("UserManager-11".equals(inputPortName)) {
            return getUserManager11();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "UserManagerService");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://soap.interfaces.ftress.aspace.com", "UserManager-11"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("UserManager11".equals(portName)) {
            setUserManager11EndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
