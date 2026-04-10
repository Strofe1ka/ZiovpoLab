package ru.ziovpo.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(BackendApplication.class, args);
    }

    /**
     * Loads {@code .env} from the working directory so {@code DB_*}, {@code JWT_*}, etc. apply to
     * {@code application.yml}. Spring Boot does not read {@code .env} by itself.
     * Existing OS environment variables take precedence.
     */
    private static void loadDotenv() {
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(e -> {
            String key = e.getKey();
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, e.getValue());
            }
        });
    }
}
