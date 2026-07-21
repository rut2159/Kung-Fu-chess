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
 * Serves the shared visual assets (board.png, piece sprites) that live in the
 * kung-fu-chess-assets module, at the classpath root - exactly where the
 * desktop client's ImageLoader/SpriteResolver already expect them.
 *
 * Spring Boot's default static-resource serving only covers classpath:/static,
 * classpath:/public, etc. Registering the classpath ROOT as a static-serving
 * location (an earlier attempt) is explicitly rejected in Spring Boot 4.1 as
 * unsafe (it would expose every .class file too). This controller instead
 * whitelists exactly two paths and reads them the same way ImageLoader does
 * on the desktop side (ClassPathResource / getResourceAsStream) - safe,
 * precise, and requires zero changes to the assets module's layout or to the
 * desktop client's existing hardcoded paths.
 */
@RestController
public class AssetController {

    @GetMapping("/board.png")
    public ResponseEntity<Resource> boardImage() {
        return serveClasspathResource("/board.png");
    }

    @GetMapping("/pieces/**")
    public ResponseEntity<Resource> pieceSprite(HttpServletRequest request) {
        // Using the raw request URI (a plain Servlet API, always reliable)
        // rather than a Spring-internal handler-mapping attribute, which can
        // vary in exact meaning depending on the path-matching strategy in use.
        String uri = request.getRequestURI(); // e.g. "/pieces/PW/states/idle/sprites/1.png"
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
