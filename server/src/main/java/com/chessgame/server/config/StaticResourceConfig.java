package com.chessgame.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(StaticResourceConfig.class);

    private static final List<String> CANDIDATE_PATHS = List.of(
            "client/src/main/resources/static",
            "../client/src/main/resources/static",
            "Kung-Fu-chess/client/src/main/resources/static",
            "../Kung-Fu-chess/client/src/main/resources/static",
            "../../client/src/main/resources/static"
    );

    private final Path liveDirectory = findLiveClientDirectory();

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (liveDirectory == null) {
            return;
        }
        registry.addResourceHandler("/**")
                .addResourceLocations(liveDirectory.toUri().toString())
                .setCachePeriod(0);
    }

    /**
     * WelcomePageHandlerMapping של Spring Boot ממפה "/" ישירות ל-index.html
     * שב-classpath, ועוקף לגמרי את ה-resource handler שלמעלה. בלי ההפניה
     * הזו, דף ההתחברות היה ממשיך להגיע מה-JAR הישן גם כשכל שאר הקבצים
     * כבר מגיעים מהדיסק - וזה בדיוק סוג הבאג שקשה לאתר.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        if (liveDirectory == null) {
            return;
        }
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    private static Path findLiveClientDirectory() {
        StringBuilder searched = new StringBuilder();
        for (String candidate : CANDIDATE_PATHS) {
            Path path = Path.of(candidate).toAbsolutePath().normalize();
            if (Files.isDirectory(path) && Files.isRegularFile(path.resolve("game.html"))) {
                log.info("CLIENT FILES: serving live from {} - edits take effect on browser refresh, no mvn install needed", path);
                return path;
            }
            searched.append("\n    ").append(path);
        }
        // חשוב לצעוק גם בכישלון: אחרת ההתנהגות היא "שום דבר לא קרה",
        // וזה נראה בדיוק כמו שהקובץ הזה לא נטען בכלל.
        log.warn("CLIENT FILES: no live client directory found - falling back to the JAR on the classpath."
                + " Client edits will NOT take effect until you run 'mvn clean install'."
                + " Working directory is {}. Searched:{}", Path.of("").toAbsolutePath(), searched);
        return null;
    }
}