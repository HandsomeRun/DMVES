package com.rabbitmq.interfaces;

@FunctionalInterface
public interface MessageHandler {
    void handle(String message);
} 