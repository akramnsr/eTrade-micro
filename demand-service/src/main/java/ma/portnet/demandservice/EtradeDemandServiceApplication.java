package ma.portnet.demandservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableFeignClients
public class EtradeDemandServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EtradeDemandServiceApplication.class, args);
    }

}
