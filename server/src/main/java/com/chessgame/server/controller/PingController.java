package com.chessgame.server.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class PingController {

    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String onPing(String message) {
        return "pong: " + message;
    }
}
