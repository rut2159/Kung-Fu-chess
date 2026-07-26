package com.chessgame.server.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * מגישה את הנכסים המשותפים (לוח וספרייטים) מתוך ה-classpath, כלומר מתוך
 * המודול assets, לדפדפן.
 *
 * שימי לב לאנוטציה: בלעדיה Spring לא רושם את המחלקה הזו בכלל, ואף אחת
 * מהמפות למטה לא קיימת - כל בקשה ל-/board.png או ל-/pieces/** חוזרת 404
 * והלוח מוצג ריק לגמרי, בלי שום שגיאה בצד השרת.
 */
@RestController
public class AssetController {

    @GetMapping("/board.png")
    public ResponseEntity<Resource> boardImage() {
        return serveClasspathResource("/board.png");
    }

    @GetMapping("/pieces/**")
    public ResponseEntity<Resource> pieceSprite(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return serveClasspathResource(uri);
    }

    private ResponseEntity<Resource> serveClasspathResource(String classpathPath) {
        ClassPathResource resource = new ClassPathResource(classpathPath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }
}
