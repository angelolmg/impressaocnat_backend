package com.dticnat.controleimpressao;

import com.dticnat.controleimpressao.service.RequestService;
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
    private RequestService requestService;

    // Tarefa cronometrada de deleção de arquivos
    // Somente arquivos associados a solicitações obsoletas serão deletados
    // E.g. solicitação fechada a mais de 3 dias
    @Scheduled(fixedRateString = "${arquivos.cleanup-rate-hours}",
                initialDelayString = "${arquivos.cleanup-rate-hours}",
                timeUnit = TimeUnit.HOURS)
    public void cleanupFiles() {
        logger.info("Limpando arquivos obsoletos...");
        logger.info("[{}] arquivos obsoletos removidos", requestService.removeStaleFiles());
    }
}
