package com.kopal.smartknowledgebase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the application.
 *
 * WHY THIS FILE EXISTS:
 * Every Spring Boot app needs exactly one class annotated with @SpringBootApplication.
 * This annotation is actually a shortcut for three annotations combined:
 *   - @Configuration      -> this class can define Spring beans
 *   - @EnableAutoConfiguration -> Spring Boot guesses and configures beans
 *                                 based on the dependencies on your classpath
 *                                 (e.g. seeing spring-boot-starter-web makes it
 *                                 configure an embedded Tomcat server)
 *   - @ComponentScan      -> Spring scans this package and all sub-packages
 *                            for classes annotated with @Component, @Service,
 *                            @Repository, @Controller/@RestController, etc.,
 *                            and registers them as beans it manages.
 *
 * Because @ComponentScan only scans this package and everything below it,
 * this class MUST stay at the root of your package tree
 * (com.kopal.smartknowledgebase), with controller/service/repository/etc.
 * as sub-packages. If you move it, Spring will stop finding your beans.
 */
@SpringBootApplication
public class SmartKnowledgeBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartKnowledgeBaseApplication.class, args);
    }

}
