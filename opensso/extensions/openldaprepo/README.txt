This submission modifies the LDAPsdk classes to provide support for the RFC's
3771 (Intermediate Response Message) and 4533 (Content Synchrionization
Operation). RFC 4533 is dependent on RFC 3771. RFC 4533 is used by OpenLDAP for
content synchronization (replication) and can also be used as an extended
search operation. OpenSSO requires that its configured LDAP server support LDAP
Persistent Search. By adding support for RFC 4533 to the LDAPsdk, the OpenSSO
ldapv3 plugin is then modified to use this as an alternative extended search
mechanism.


Note that minimal modifications have been made to the existing LDAPsdk classes.
Specifically the following are changed:

LDAPMessage: to introduce and handle the INTERMEDIATE_RESPONSE message type
JDAPBERTagDecoder: to handle the INTERMEDIATE_RESPONSE message type
JDAPProtocolOp: to introduce the INTERMEDIATE_RESPONSE message type

The newly introduced LDAPsdk classes only come into play when an INTERMEDIATE RESPONSE
message is received or when a Content Sunchronization operation is initiated by the
caller.






