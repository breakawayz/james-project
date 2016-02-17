/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.jmap.methods;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Date;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.apache.james.jmap.model.CreationMessage;
import org.apache.james.jmap.model.Emailer;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.stream.Field;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class MIMEMessageConverterTest {
    @Test
    public void convertToMimeShouldAddInReplyToHeaderWhenProvided() {
        // Given
        MIMEMessageConverter sut = new MIMEMessageConverter();

        String matchingMessageId = "unique-message-id";
        CreationMessage messageHavingInReplyTo = CreationMessage.builder()
                .inReplyToMessageId(matchingMessageId)
                .mailboxIds(ImmutableList.of("Hey!"))
                .subject("subject")
                .build();

        // When
        Message result = sut.convertToMime(new MessageWithId.CreationMessageEntry(
                "user|mailbox|1", messageHavingInReplyTo));

        // Then
        assertThat(result.getHeader().getFields("In-Reply-To")).extracting(Field::getBody)
                .containsOnly(matchingMessageId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertToMimeShouldThrowWhenMessageIsNull() {
        MIMEMessageConverter sut = new MIMEMessageConverter();

        sut.convertToMime(new MessageWithId.CreationMessageEntry("any", null));
    }

    @Test
    public void convertToMimeShouldSetBothFromAndSenderHeaders() {
        // Given
        MIMEMessageConverter sut = new MIMEMessageConverter();

        String joesEmail = "joe@example.com";
        CreationMessage testMessage = CreationMessage.builder()
                .mailboxIds(ImmutableList.of("deadbeef-dead-beef-dead-beef"))
                .subject("subject")
                .from(Emailer.builder().email(joesEmail).name("joe").build())
                .build();

        // When
        Message result = sut.convertToMime(new MessageWithId.CreationMessageEntry(
                "user|mailbox|1", testMessage));

        // Then
        assertThat(result.getFrom()).extracting(Mailbox::getAddress).allMatch(f -> f.toString().equals(joesEmail));
        assertThat(result.getSender().getAddress()).isEqualTo(joesEmail);
    }

    @Test
    public void convertToMimeShouldSetCorrectLocalDate() {
        // Given
        MIMEMessageConverter sut = new MIMEMessageConverter();

        Instant now = Instant.now();
        ZonedDateTime messageDate = ZonedDateTime.ofInstant(now, ZoneId.systemDefault());

        CreationMessage testMessage = CreationMessage.builder()
                .mailboxIds(ImmutableList.of("dead-bada55"))
                .subject("subject")
                .date(messageDate)
                .build();

        // When
        Message result = sut.convertToMime(new MessageWithId.CreationMessageEntry(
                "user|mailbox|1", testMessage));

        // Then
        assertThat(result.getDate()).isEqualToIgnoringMillis(Date.from(now));
    }
}