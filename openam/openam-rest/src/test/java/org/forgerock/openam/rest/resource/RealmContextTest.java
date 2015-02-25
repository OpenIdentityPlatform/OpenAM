///*
// * The contents of this file are subject to the terms of the Common Development and
// * Distribution License (the License). You may not use this file except in compliance with the
// * License.
// *
// * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
// * specific language governing permission and limitations under the License.
// *
// * When distributing Covered Software, include this CDDL Header Notice in each file and include
// * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
// * Header, with the fields enclosed by brackets [] replaced by your own identifying
// * information: "Portions copyright [year] [name of copyright owner]".
// *
// * Copyright 2014 ForgeRock AS.
// */
//
//package org.forgerock.openam.rest.resource;
//
//import org.forgerock.json.resource.RootContext;
//import org.testng.annotations.Test;
//
//import static org.fest.assertions.Assertions.assertThat;
//
//public class RealmContextTest {
//
//    @Test
//    public void shouldNotAddNullToRealm() {
//
//        //Given
//        RealmContext context = new RealmContext(new RootContext(), "/");
//
//        //When
//        context.addSubRealm(null);
//
//        //Then
//        assertThat(context.getResolvedRealm()).isEqualTo("/");
//    }
//
//    @Test
//    public void shouldNotAddEmptyStringToRealm() {
//
//        //Given
//        RealmContext context = new RealmContext(new RootContext(), "/");
//
//        //When
//        context.addSubRealm("");
//
//        //Then
//        assertThat(context.getResolvedRealm()).isEqualTo("/");
//    }
//
//    @Test
//    public void shouldAddSubRealm() {
//
//        //Given
//        RealmContext context = new RealmContext(new RootContext(), "/");
//
//        //When
//        context.addSubRealm("realm1");
//
//        //Then
//        assertThat(context.getResolvedRealm()).isEqualTo("/realm1");
//    }
//
//    @Test
//    public void shouldAddSecondSubRealm() {
//
//        //Given
//        RealmContext context = new RealmContext(new RootContext(), "/");
//        context.addSubRealm("realm1");
//
//        //When
//        context.addSubRealm("realm2");
//
//        //Then
//        assertThat(context.getResolvedRealm()).isEqualTo("/realm1/realm2");
//    }
//
//    @Test
//    public void shouldAddSubRealmAfterStrippingLeadingAndTrailingBackslashes() {
//
//        //Given
//        RealmContext context = new RealmContext(new RootContext(), "/");
//
//        //When
//        context.addSubRealm("/realm1/realm2/");
//
//        //Then
//        assertThat(context.getResolvedRealm()).isEqualTo("/realm1/realm2");
//    }
//}
