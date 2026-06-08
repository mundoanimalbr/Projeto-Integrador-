package senai.infoA.com.SMMDS;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import senai.infoA.com.SMMDS.services.ArduinoService;

@SpringBootApplication
public class SmmdsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmmdsApplication.class, args);
	}

	@Bean
    public CommandLineRunner run(ArduinoService arduinoService) {
        return args -> {
            System.out.println("Iniciando a escuta do sensor USB...");
            // CHAMA O SEU MÉTODO DE LEITURA
            arduinoService.iniciarLeitura();
        };
    }

}
