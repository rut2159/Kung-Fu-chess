package com.chessgame.server.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class PingController {

    /**
     * A client sends a STOMP message to /app/ping (e.g. any text payload).
     * Spring routes it here, this method returns a response, and Spring
     * broadcasts that response to everyone subscribed to /topic/pong.
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String onPing(String message) {
        return "pong: " + message;
    }
}
