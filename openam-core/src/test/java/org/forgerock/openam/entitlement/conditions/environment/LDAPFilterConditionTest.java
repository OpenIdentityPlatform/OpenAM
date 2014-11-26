package org.forgerock.openam.entitlement.conditions.environment;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class LDAPFilterConditionTest {

    private LDAPFilterCondition condition;

    @BeforeMethod
    public void setup() throws Exception {
        SSOToken mockSsoToken = mock(SSOToken.class);
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(mockSsoToken);

        condition = new LDAPFilterCondition();
    }

    @Test
    public void testCanSerializeStateToJsonAndBack() throws EntitlementException {

        // Given
        String ldapFilter = "cn=Barbara Jensen";
        condition.setLdapFilter(ldapFilter);
        LDAPFilterCondition copy = new LDAPFilterCondition();

        // When
        copy.setState(condition.getState());

        // Then
        assertThat(copy.getLdapFilter()).isEqualTo(condition.getLdapFilter());
    }
}
