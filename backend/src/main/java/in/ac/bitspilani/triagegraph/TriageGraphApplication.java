package in.ac.bitspilani.triagegraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TriageGraphApplication {

    public static void main(String[] args) {
        SpringApplication.run(TriageGraphApplication.class, args);
    }
}
