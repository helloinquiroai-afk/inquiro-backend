package com.inquiro.communication.messenger;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessengerEventParserTest {
    private final MessengerEventParser parser = new MessengerEventParser(new ObjectMapper());

    @Test void readsAllEntriesAndIgnoresEchoesAndUnsupportedEvents() throws Exception {
        var events = parser.parse("""
                {"object":"page","entry":[
                  {"id":"100","messaging":[
                    {"sender":{"id":"200"},"recipient":{"id":"100"},"timestamp":123,
                     "message":{"mid":"a","text":"Hello"}},
                    {"sender":{"id":"201"},"recipient":{"id":"100"},"message":{"mid":"echo","is_echo":true,"text":"echo"}},
                    {"sender":{"id":"202"},"recipient":{"id":"100"},"message":{"attachments":[]}},
                    {"delivery":{"mids":["a"]}},
                    {"sender":{"id":"203"},"recipient":{"id":"100"},"message":{"mid":"b","text":"Room please"}}]},
                  {"id":"101","messaging":[
                    {"sender":{"id":"200"},"recipient":{"id":"101"},"message":{"mid":"c","text":"Table please"}}]}
                ]}
                """.getBytes(StandardCharsets.UTF_8));
        assertEquals(3, events.size());
        assertEquals("a", events.get(0).messageId());
        assertEquals(123, events.get(0).timestamp());
        assertEquals("101", events.get(2).pageId());
    }

    @Test void ignoresMalformedEventsSafely() throws Exception {
        assertTrue(parser.parse("""
                {"object":"page","entry":[{"id":"100","messaging":[{},null,
                {"sender":{"id":"100"},"recipient":{"id":"100"},"message":{"text":"echo"}},
                {"sender":{"id":"200"},"recipient":{"id":"999"},"message":{"text":"wrong page"}},
                {"sender":{"id":"200"},"recipient":{"id":"100"},"message":{"text":" "}}]}]}
                """.getBytes(StandardCharsets.UTF_8)).isEmpty());
        assertTrue(parser.parse("{\"object\":\"other\"}".getBytes(StandardCharsets.UTF_8)).isEmpty());
    }

    @Test void deduplicationIncludesPageAndHasTimestampFallback() {
        var event = new MessengerEvent("200", "100", "Hello", "same-id", 1);
        assertEquals(MessengerMessageProcessor.eventKey(event), MessengerMessageProcessor.eventKey(event));
        assertNotEquals(MessengerMessageProcessor.eventKey(event),
                MessengerMessageProcessor.eventKey(new MessengerEvent("200", "101", "Hello", "same-id", 1)));
        var noId = new MessengerEvent("200", "100", "Hello", "", 123);
        assertEquals(MessengerMessageProcessor.eventKey(noId), MessengerMessageProcessor.eventKey(noId));
    }
}