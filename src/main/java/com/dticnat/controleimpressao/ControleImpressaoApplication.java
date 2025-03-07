package com.dticnat.controleimpressao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ControleImpressaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ControleImpressaoApplication.class, args);
	}

}
