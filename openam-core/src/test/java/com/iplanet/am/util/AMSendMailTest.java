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
 * Copyright 2026 3A Systems, LLC.
 */
package com.iplanet.am.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * GHSA-mw38-8gr7-c4x2: the subject reaching {@link AMSendMail} used to be written into the Subject header as it
 * came, so a CR or an LF in it would end the header and let whatever followed be read as a header of its own.
 *
 * The tests drive the two {@code postMail} overloads that build a message, against a mocked transport, so that they
 * cover the sanitising the methods themselves do rather than a model of it.
 */
@PrepareForTest({ Transport.class, AMSendMail.class, BrowserEncoding.class })
// G11NSettings, which the charset mapping of BrowserEncoding is built on, needs a running server.
@SuppressStaticInitializationFor("com.iplanet.am.util.BrowserEncoding")
public class AMSendMailTest extends PowerMockTestCase {

    private static final String FROM = "openam@example.com";
    private static final String TO = "victim@example.com";
    private static final String BODY = "Follow the link to reset your password.";

    /** What an administrator would have to put in the Email Service of the realm to get a break into a header. */
    private static final String SUBJECT_WITH_A_BREAK = "Password Reset\r\nBcc: attacker@example.org";

    /**
     * A break carried in the display name of an address. The parser takes this apart without complaining, unlike
     * a break outside the quotes, which it rejects.
     */
    private static final String ADDRESS_WITH_A_BREAK = "\"Vic\r\nBcc: attacker@example.org\" <victim@example.com>";

    @DataProvider(name = "headerBreaks")
    public Object[][] headerBreaks() {
        return new Object[][] {
            {SUBJECT_WITH_A_BREAK},
            {"Password Reset\nBcc: attacker@example.org"},
            {"Password Reset\rBcc: attacker@example.org"},
            {"Password Reset\r\n\r\nBcc: attacker@example.org"},
            // A break at the very start of the value is dropped by MimeUtility.fold, so for this row alone the
            // message tests are carried by the library rather than by the sanitising and stay green without it.
            // Kept because the input is a plausible one, and shouldTakeHeaderBreaksOutOfTheSubject does hold it.
            {"\r\nBcc: attacker@example.org"},
        };
    }

    @BeforeMethod
    public void mockTransportAndCharsetMapping() {
        PowerMockito.mockStatic(Transport.class);
        PowerMockito.mockStatic(BrowserEncoding.class);
        PowerMockito.when(BrowserEncoding.mapHttp2JavaCharset(Mockito.anyString())).thenReturn("UTF-8");
    }

    @Test(dataProvider = "headerBreaks")
    public void shouldTakeHeaderBreaksOutOfTheSubject(String subject) {
        String sanitized = AMSendMail.sanitizeHeaderValue(subject);

        assertThat(sanitized).doesNotContain("\r").doesNotContain("\n");
        assertThat(sanitized).hasSameSizeAs(subject);
    }

    @Test
    public void shouldLeaveASubjectWithoutHeaderBreaksAlone() {
        assertThat(AMSendMail.sanitizeHeaderValue("Password Reset")).isEqualTo("Password Reset");
    }

    @Test
    public void shouldPassNullThrough() {
        assertThat(AMSendMail.sanitizeHeaderValue(null)).isNull();
    }

    /** A NUL does not end a header, but it truncates the header block at some agents, which comes to the same. */
    @Test
    public void shouldTakeTheOtherControlCharactersOutButKeepTheTab() {
        assertThat(AMSendMail.sanitizeHeaderValue("Password\0Reset")).isEqualTo("Password Reset");
        assertThat(AMSendMail.sanitizeHeaderValue("Password\013Reset")).isEqualTo("Password Reset");
        assertThat(AMSendMail.sanitizeHeaderValue("Password\tReset")).isEqualTo("Password\tReset");
    }

    /**
     * Jakarta Mail folds an embedded CR or LF into a continuation line, but only while the default
     * mail.mime.foldtext setting is in force. Taking the characters out first means the injected text cannot become
     * a header of its own however the message is later serialised.
     */
    @Test(dataProvider = "headerBreaks")
    public void shouldNotLetTheSubjectIntroduceAHeader(String subject) throws Exception {
        new AMSendMail().postMail(new String[] {TO}, subject, BODY, FROM, "text/plain", "UTF-8");

        // Jakarta Mail would fold a break left in the value into a continuation line, which is why the header as
        // it is held has to be checked too: that is what tells a sanitised subject from one the library papered
        // over, and the folding only holds while the default mail.mime.foldtext setting is in force.
        assertThat(sentMessage().getHeader("Subject")[0]).doesNotContain("\r").doesNotContain("\n");

        String serialized = serializedMessage(sentMessage());
        // The injected text survives inside the Subject value, which is harmless - what must not happen is it
        // starting a line of its own, because that is what makes it a header.
        assertThat(countHeader(serialized, "Bcc:")).isZero();
        assertThat(countHeader(serialized, "Subject:")).isEqualTo(1);
        assertThat(countHeader(serialized, "Content-Type:")).isEqualTo(1);
        assertThat(serialized).contains("attacker@example.org");
    }

    /** The overload takes a different branch when no charset is configured, and that one is sanitised too. */
    @Test(dataProvider = "headerBreaks")
    public void shouldNotLetTheSubjectIntroduceAHeaderWithoutACharset(String subject) throws Exception {
        new AMSendMail().postMail(new String[] {TO}, subject, BODY, FROM, "text/plain", null);

        assertThat(sentMessage().getHeader("Subject")[0]).doesNotContain("\r").doesNotContain("\n");

        String serialized = serializedMessage(sentMessage());
        assertThat(countHeader(serialized, "Bcc:")).isZero();
        assertThat(countHeader(serialized, "Subject:")).isEqualTo(1);
    }

    /** A Content-Type of the attacker's choosing would change how the whole message is rendered. */
    @Test
    public void shouldNotLetTheSubjectIntroduceAContentTypeHeader() throws Exception {
        new AMSendMail().postMail(new String[] {TO}, "Password Reset\r\nContent-Type: text/html", BODY, FROM,
                "text/plain", "UTF-8");

        // The line counting below cannot carry this on its own: Jakarta Mail folds a break left in the subject
        // into a continuation line, and a folded line starts with a space, so the injected header is not counted
        // as one either way. The header as it is held is what keeps the break.
        assertThat(sentMessage().getHeader("Subject")[0]).doesNotContain("\r").doesNotContain("\n");

        String serialized = serializedMessage(sentMessage());
        assertThat(countHeader(serialized, "Content-Type:")).isEqualTo(1);
        assertThat(serialized).contains("Content-Type: text/plain; charset=UTF-8");
    }

    /**
     * Unlike the Subject, the Content-Type is written out by Jakarta Mail with no folding at all, so a CR or an LF
     * inside a quoted parameter of it would reach the message as it came and start a line of its own.
     */
    @Test
    public void shouldNotLetTheContentTypeIntroduceAHeader() throws Exception {
        new AMSendMail().postMail(new String[] {TO}, "Password Reset", BODY, FROM,
                "text/plain; name=\"a\r\nBcc: attacker@example.org\"", "UTF-8");

        String serialized = serializedMessage(sentMessage());
        assertThat(countHeader(serialized, "Bcc:")).isZero();
        assertThat(countHeader(serialized, "Content-Type:")).isEqualTo(1);
        // After the serialisation, not before it: the Content-Type is written into the message by saveChanges(),
        // which the mocked transport never reaches, and which serialising is what triggers instead.
        assertThat(sentMessage().getHeader("Content-Type")[0]).doesNotContain("\r").doesNotContain("\n");
    }

    /** The same, with the empty line that would otherwise end the header block and start the body. */
    @Test
    public void shouldNotLetTheContentTypeEndTheHeaderBlock() throws Exception {
        new AMSendMail().postMail(new String[] {TO}, "Password Reset", BODY, FROM,
                "text/plain; name=\"a\r\n\r\nBcc: attacker@example.org\"", "UTF-8");

        String serialized = serializedMessage(sentMessage());
        assertThat(countHeader(serialized, "Bcc:")).isZero();
        assertThat(countHeader(serialized, "Content-Type:")).isEqualTo(1);
        assertThat(serialized).contains("Subject: Password Reset");
        assertThat(sentMessage().getHeader("Content-Type")[0]).doesNotContain("\r").doesNotContain("\n");
    }

    /**
     * The overload the Email Service of a realm sends through, now that the wording of self service mail comes from
     * the configuration and from nowhere else: a break an administrator left in the configured subject does not
     * reach the header either.
     */
    @Test
    public void shouldNotLetTheConfiguredSubjectIntroduceAHeader() throws Exception {
        new AMSendMail().postMail(new String[] {TO}, SUBJECT_WITH_A_BREAK, BODY, FROM, "text/plain",
                "UTF-8", "smtp.example.com", "25", "openam", "secret", false);

        assertThat(sentMessage().getHeader("Subject")[0]).doesNotContain("\r").doesNotContain("\n");

        String serialized = serializedMessage(sentMessage());
        assertThat(countHeader(serialized, "Bcc:")).isZero();
        assertThat(countHeader(serialized, "Subject:")).isEqualTo(1);
        assertThat(serialized).contains("attacker@example.org");
    }

    /**
     * The recipient is the one field of a self service registration the anonymous caller still fills in himself.
     * A break outside the quotes is rejected by the address parser, but one in the quoted display name goes
     * through it untouched and reaches the To header.
     */
    @Test
    public void shouldNotLetTheRecipientIntroduceAHeader() throws Exception {
        new AMSendMail().postMail(new String[] {ADDRESS_WITH_A_BREAK}, "Password Reset", BODY, FROM, "text/plain",
                "UTF-8");

        assertThat(sentMessage().getHeader("To")[0]).doesNotContain("\r").doesNotContain("\n");

        String serialized = serializedMessage(sentMessage());
        assertThat(countHeader(serialized, "Bcc:")).isZero();
        assertThat(countHeader(serialized, "To:")).isEqualTo(1);
        // The mailbox the message is delivered to is the one it was addressed to, display name or not.
        assertThat(sentMessage().getAllRecipients()).hasSize(1);
        assertThat(((InternetAddress) sentMessage().getAllRecipients()[0]).getAddress()).isEqualTo(TO);
    }

    /** The From address comes from the Email Service, and goes into a header of its own just the same. */
    @Test
    public void shouldNotLetTheFromAddressIntroduceAHeader() throws Exception {
        new AMSendMail().postMail(new String[] {TO}, "Password Reset", BODY,
                "\"OpenAM\r\nBcc: attacker@example.org\" <openam@example.com>", "text/plain", "UTF-8");

        assertThat(sentMessage().getHeader("From")[0]).doesNotContain("\r").doesNotContain("\n");

        String serialized = serializedMessage(sentMessage());
        assertThat(countHeader(serialized, "Bcc:")).isZero();
        assertThat(countHeader(serialized, "From:")).isEqualTo(1);
    }

    /** Whatever the subject carries, the message still goes to the one recipient it was addressed to. */
    @Test(dataProvider = "headerBreaks")
    public void shouldSendToTheIntendedRecipientOnly(String subject) throws Exception {
        new AMSendMail().postMail(new String[] {TO}, subject, BODY, FROM, "text/plain", "UTF-8");

        assertThat(sentMessage().getAllRecipients()).hasSize(1);
        assertThat(sentMessage().getAllRecipients()[0].toString()).isEqualTo(TO);
    }

    /** The message the mocked transport was handed. */
    private MimeMessage sentMessage() throws MessagingException {
        ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
        PowerMockito.verifyStatic(Transport.class);
        Transport.send(sent.capture());
        return (MimeMessage) sent.getValue();
    }

    /**
     * Serialises the message, whole.
     *
     * The header block alone will not do. Where the injected value carries an empty line, that line is itself
     * taken for the end of the block, so the header the injection was meant to introduce falls outside what the
     * assertions then look at - and a test cutting the message there passes whether the value was sanitised or
     * not.
     */
    private String serializedMessage(MimeMessage message) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.writeTo(out);
        return out.toString("UTF-8");
    }

    /** Counts the lines that start a header of the given name; a folded continuation line starts with a space. */
    private int countHeader(String serialized, String name) {
        int count = 0;
        for (String line : serialized.split("\r\n")) {
            if (line.startsWith(name)) {
                count++;
            }
        }
        return count;
    }
}
