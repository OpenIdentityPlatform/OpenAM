#!/bin/ksh 

ECHO=/usr/bin/echo
GETTEXT=/usr/bin/gettext
RM="/bin/rm -rf"
CUT=/usr/bin/cut
OS=`uname`

if [ $OS = "Linux" ]; then
  ECHO="/bin/echo -e"
  GETTEXT=/bin/gettext
  RM="/bin/rm -rf"
  CUT=/bin/cut
elif [ "$OS" = "HP-UX" ]; then
  GETTEXT=/usr/local/bin/gettext
  RM="/usr/bin/rm -r"
fi

PWF=ssoadm.pw.file

msg() {
  $ECHO "$@"
}

OMIT='\c'

# list of new properties to be added
SP_LIST="com.iplanet.am.session.agentSessionIdleTime=0 com.sun.identity.am.cookie.check=false com.sun.identity.authentication.setCookieToAllDomains=true com.sun.identity.policy.resultsCacheMaxSize=10000"

##############################################################################
# Get the path of ssoadm
##############################################################################
get_ssoadm_path() {
  $ECHO "get_ssoadm_path..."
  while [ -z $SSOADM_PATH ]
  do
    msg "Path of ssoadm tool: ${OMIT}"
    read SSOADM_PATH
    if [ ! -x $SSOADM_PATH/ssoadm ]; then
      msg "ssoadm does not exist in $SSOADM_PATH or"
      msg "$SSOADM_PATH/ssoadm is not executable by the current user, please re-enter"
      SSOADM_PATH=
    fi
  done
}

##############################################################################
# Get the password of amadmin
##############################################################################
get_amadmin_password() {
  $ECHO "get_amadmin_password..."
  ADM_PW=
  if [ -f $PWF ]; then
    $RM $PWF
  fi

  msg "amadmin password: ${OMIT}"
  stty -echo
  read ADM_PW
  stty echo
  $ECHO
  $ECHO $ADM_PW > $PWF
  chmod 400 $PWF
}

##############################################################################
# Validate amadmin password
##############################################################################
validate_amadmin_password() {
  $ECHO "validate_amadmin_password..."
  while [ -z $DFTCFG ]
  do
    $ECHO "$SSOADM_PATH/ssoadm list-server-cfg -s default -u amadmin -f $PWF"
    DFTCFG=`$SSOADM_PATH/ssoadm list-server-cfg -s default -u amadmin -f $PWF`
    ret=$?
    if [ -z $DFTCFG ]; then
      msg "Your input is invalid, error code is $ret, please try again"
      get_amadmin_password
    fi
  done
}

##############################################################################
# Add new default server config properties
##############################################################################
add_server_properties() {
  $ECHO "add_server_properties..."
  $ECHO "$SSOADM_PATH/ssoadm update-server-cfg -s default -u amadmin -f $PWF -a $SP_LIST"
  $SSOADM_PATH/ssoadm update-server-cfg -s default -u amadmin -f $PWF -v -a $SP_LIST
}

##############################################################################
# Update default value of sun-idrepo-ldapv3-config-referrals in subschema
# LDAPv3ForAD of service sunIdentityRepositoryService
##############################################################################
update_ldapv3forad() {
  $ECHO "update_ldapv3forad..."
  $ECHO "$SSOADM_PATH/ssoadm set-attr-defs -s sunIdentityRepositoryService -t Organization -c LDAPv3ForAD -u amadmin -f $PWF -v -a sun-idrepo-ldapv3-config-referrals=false"
  $SSOADM_PATH/ssoadm set-attr-defs -s sunIdentityRepositoryService -t Organization -c LDAPv3ForAD -u amadmin -f $PWF -v -a sun-idrepo-ldapv3-config-referrals=false
}

##############################################################################
# Add new attribute schema iplanet-am-session-dnrestrictiononly to
# iPlanetAMSessionService
##############################################################################
update_session() {
  XMLFILE=add_session_attr.xml
  $ECHO "$SSOADM_PATH/ssoadm add-attrs -s iPlanetAMSessionService -t Global -u amadmin -f $PWF -F $XMLFILE"
  $SSOADM_PATH/ssoadm add-attrs -s iPlanetAMSessionService -t Global -u amadmin -f $PWF -F $XMLFILE
}

##############################################################################
# main
##############################################################################
get_ssoadm_path
get_amadmin_password
validate_amadmin_password
add_server_properties
update_ldapv3forad
update_session
$RM $PWF
