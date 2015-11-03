package org.forgerock.openam.radius.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.forgerock.openam.radius.common.AccessRequest;
import org.forgerock.openam.radius.common.Authenticator;
import org.forgerock.openam.radius.common.Packet;
import org.forgerock.openam.radius.common.UserNameAttribute;
import org.testng.annotations.Test;

public class RadiusRequestTest {

    @Test
    public void getAttribute() {
        // Given
        UserNameAttribute una = new UserNameAttribute("testUser");
        AccessRequest packet = new AccessRequest();
        packet.addAttribute(una);
        RadiusRequest request = new RadiusRequest(packet);
        // When
        UserNameAttribute attribute = (UserNameAttribute) request.getAttribute(UserNameAttribute.class);
        // then
        assertThat(attribute).isSameAs(una);
    }

    @Test
    public void getRequestId() {
        // Given
        AccessRequest packet = new AccessRequest((short) 1, mock(Authenticator.class));
        RadiusRequest request = new RadiusRequest(packet);
        // Then
        String reqId = request.getRequestId();

        String reqId2 = request.getRequestId();
        assertThat(reqId2).isEqualTo(reqId);
    }

    @Test
    public void getRequestPacket() {
        // Given
        AccessRequest packet = new AccessRequest((short) 1, mock(Authenticator.class));
        RadiusRequest request = new RadiusRequest(packet);
        // When
        Packet returned = request.getRequestPacket();
        // Then
        assertThat(returned).isSameAs(packet);
    }

    @Test
    public void getUsername() {
        // Given
        String userName = "testUser";
        AccessRequest packet = new AccessRequest((short) 1, mock(Authenticator.class));
        UserNameAttribute userNameAttribute = new UserNameAttribute(userName);
        packet.addAttribute(userNameAttribute);
        RadiusRequest request = new RadiusRequest(packet);

        // when
        String returnedUserName = request.getUsername();

        // Then
        assertThat(returnedUserName).isEqualTo(userName);
    }

}
