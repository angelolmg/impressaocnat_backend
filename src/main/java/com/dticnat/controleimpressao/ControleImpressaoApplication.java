package com.dticnat.controleimpressao;

import com.dticnat.controleimpressao.controller.ReportController;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ControleImpressaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ControleImpressaoApplication.class, args);

		// Incluir ReportController na documentação
		// Isto deve ser feito manualmente porque o SpringDoc ignora endpoints do tipo @Controller
		SpringDocUtils.getConfig().addRestControllers(ReportController.class);
	}

}
