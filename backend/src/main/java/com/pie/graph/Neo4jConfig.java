package com.pie.graph;

import com.pie.config.PieProperties;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {

  @Bean(destroyMethod = "close")
  Driver neo4jDriver(PieProperties properties) {
    return GraphDatabase.driver(
        properties.neo4j().uri(),
        AuthTokens.basic(properties.neo4j().username(), properties.neo4j().password()));
  }
}
