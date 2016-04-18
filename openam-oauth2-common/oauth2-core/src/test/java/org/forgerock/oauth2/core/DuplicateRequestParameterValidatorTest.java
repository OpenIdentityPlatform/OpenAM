package org.forgerock.oauth2.core;


import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashSet;

import org.forgerock.oauth2.core.exceptions.DuplicateRequestParameterException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @since 14.0.0
 */
public class DuplicateRequestParameterValidatorTest {


    private DuplicateRequestParameterValidator duplicateParamValidator;

    @BeforeMethod
    public void setup() {
        duplicateParamValidator = new DuplicateRequestParameterValidator();
    }

    @Test(expectedExceptions = {DuplicateRequestParameterException.class})
    public void shouldThrowInvalidRequestEXceptionWhenDuplicateParameterFound() throws Exception {

        //given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameterNames()).willReturn(new HashSet<String>(Arrays.asList("someParam")));
        given(request.getParameterCount("someParam")).willReturn(2);


        //when
        duplicateParamValidator.validateRequest(request);


        //then
        // invalid request excetption thrown

    }

    @Test
    public void shouldPassValidationWhenNoDuplicatesFound() throws Exception {

        //given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameterNames()).willReturn(new HashSet<String>(Arrays.asList("someParam")));
        given(request.getParameterCount("someParam")).willReturn(1);


        //when
        duplicateParamValidator.validateRequest(request);


        //then
        // request is valid

    }


}
