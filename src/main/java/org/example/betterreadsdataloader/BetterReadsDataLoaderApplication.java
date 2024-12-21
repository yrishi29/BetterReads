package org.example.betterreadsdataloader;

import org.example.betterreadsdataloader.connection.DatastaxAstraProperties;
import jakarta.annotation.PostConstruct;
import org.example.betterreadsdataloader.model.Author;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;
import org.example.betterreadsdataloader.repository.AuthorRepository;

import java.nio.file.Path;

@SpringBootApplication
@EnableConfigurationProperties(DatastaxAstraProperties.class)

public class BetterReadsDataLoaderApplication {



    public static void main(String[] args) {
        SpringApplication.run(BetterReadsDataLoaderApplication.class, args);
    }

    @Bean
    public CqlSessionBuilderCustomizer sessionBuilderCustomizer(DatastaxAstraProperties astraProperties) {
        Path bundle = astraProperties.getSecureConnectBundle().toPath();
        return builder -> builder.withCloudSecureConnectBundle(bundle);
    }


}
