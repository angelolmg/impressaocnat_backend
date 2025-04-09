package com.dticnat.controleimpressao;

import com.dticnat.controleimpressao.service.SolicitationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ScheduledTasks {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired
    private SolicitationService solicitationService;

    /**
     * Tarefa agendada para remover arquivos associados a solicitações obsoletas.
     *
     * Este método é executado periodicamente, conforme configurado pelas propriedades
     * `arquivos.cleanup-rate-hours` (frequência) e `arquivos.cleanup-rate-hours` (delay inicial).
     * Somente arquivos associados a solicitações obsoletas serão deletados
     * E.g. solicitação fechada a mais de 3 dias
     */
    @Scheduled(fixedRateString = "${arquivos.cleanup-rate-hours}",
            initialDelayString = "${arquivos.cleanup-rate-hours}",
            timeUnit = TimeUnit.HOURS)
    public void cleanupFiles() {
        logger.info("Iniciando a limpeza de arquivos obsoletos...");
        int deletedFiles = solicitationService.removeStaleFiles();
        logger.info("Limpeza de arquivos obsoletos concluída. [{}] arquivos removidos.", deletedFiles);
    }
}
