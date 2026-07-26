package com.chessgame.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * ברירת המחדל של Spring היא חוצץ יציאה של 512KB לכל session, ומעליו
     * ה-session נסגר לאלתר:
     *
     *   Terminating 'WebSocketServerSockJsSession[...]':
     *   Buffer size 525402 bytes exceeds the allowed limit 524288
     *
     * זה בדיוק מה שקרה: הלקוח נתקע לרגע (טאב ברקע, GC, רשת), ההודעות
     * הצטברו, והשרת ניתק אותו. בלי חיבור-מחדש בצד הלקוח, הלוח פשוט קפא.
     *
     * הרחבת החוצץ קונה סובלנות להאטות רגעיות, אבל היא לא התיקון האמיתי -
     * הוא נמצא ב-GameService, שלא משדר יותר מצב שלא השתנה.
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setSendBufferSizeLimit(2 * 1024 * 1024);
        registration.setSendTimeLimit(20 * 1000);
        registration.setMessageSizeLimit(256 * 1024);
    }
}
