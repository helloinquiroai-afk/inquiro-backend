package com.inquiro.channel;

import com.inquiro.business.BusinessChannelType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChannelMessageTest {

    @Test
    void requiresAllMessageIdentityFields() {
        assertThrows(IllegalArgumentException.class, () ->
                new ChannelMessage(null, "site", "customer", "hello"));
        assertThrows(IllegalArgumentException.class, () ->
                new ChannelMessage(BusinessChannelType.WEBSITE, "", "customer", "hello"));
        assertThrows(IllegalArgumentException.class, () ->
                new ChannelMessage(BusinessChannelType.WEBSITE, "site", "", "hello"));
        assertThrows(IllegalArgumentException.class, () ->
                new ChannelMessage(BusinessChannelType.WEBSITE, "site", "customer", ""));
    }
}
