package com.inquiro.communication.messenger;

public record MessengerEvent(String senderId, String pageId, String text, String messageId, long timestamp) {}