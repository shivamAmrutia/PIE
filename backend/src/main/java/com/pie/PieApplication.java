package com.pie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PieApplication {

  public static void main(String[] args) {
    SpringApplication.run(PieApplication.class, args);
  }
}
