/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */
package org.forgerock.openam.authentication.callbacks.helpers;

import org.fest.assertions.Assertions;
import org.forgerock.openam.utils.TimeTravelUtil;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class PollingWaitSpamCheckerTest {

    @Test
    public void shouldNotBeInSpammedStateOnCreation() {

        // given
        PollingWaitSpamChecker checker = new PollingWaitSpamChecker(100, 3);
        checker.resetSpamCheck(300);

        // when

        // then
        Assertions.assertThat(checker.isSpammed()).isFalse();

    }

    @Test
    public void shouldNotBeInSpammedAfterSingleOnTimeRequest() {

        // given
        PollingWaitSpamChecker checker = new PollingWaitSpamChecker(100, 3);
        checker.resetSpamCheck(300);

        // when
        TimeTravelUtil.fastForward(250);

        // then
        Assertions.assertThat(checker.isWaitLongEnough()).isTrue();
        Assertions.assertThat(checker.isSpammed()).isFalse();
    }

    @DataProvider
    public  Object[][] expectedFailureTries() {
        return new Object[][]{
                {new Integer(1), Boolean.FALSE},
                {new Integer(2), Boolean.FALSE},
                {new Integer(3), Boolean.FALSE},
                {new Integer(4), Boolean.TRUE},
                {new Integer(5), Boolean.TRUE},
        };
    }

    @Test (dataProvider = "expectedFailureTries")
    public void shouldBeInSpammedStateAfterAproepriateNumberEarlyRequest(Integer spamCount, Boolean expectedResult ) {

        // given
        PollingWaitSpamChecker checker = new PollingWaitSpamChecker(100, 3);
        checker.resetSpamCheck(300);

        // when
        TimeTravelUtil.fastForward(100);
        for (int i = 0; i < spamCount; i++) {
            if (!checker.isWaitLongEnough()) {
                checker.incrementSpamCheck();
            }
        }

        // then
        Assertions.assertThat(checker.isWaitLongEnough()).isFalse();
        Assertions.assertThat(checker.isSpammed()).isEqualTo(expectedResult);
    }

    @Test
    public void shouldResetSpamRequestsOnNewPeriodStart() {

        // given
        PollingWaitSpamChecker checker = new PollingWaitSpamChecker(100, 3);
        checker.resetSpamCheck(300);

        // when
        TimeTravelUtil.fastForward(100);

        checker.incrementSpamCheck();
        checker.incrementSpamCheck();
        checker.incrementSpamCheck();

        TimeTravelUtil.fastForward(150);

        checker.resetSpamCheck(300);
        TimeTravelUtil.fastForward(150);
        checker.incrementSpamCheck();
        TimeTravelUtil.fastForward(150);

        // then
        Assertions.assertThat(checker.isWaitLongEnough()).isTrue();
        Assertions.assertThat(checker.isSpammed()).isFalse();

    }
}