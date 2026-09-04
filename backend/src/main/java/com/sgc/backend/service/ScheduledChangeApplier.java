package com.sgc.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Faz as alteracoes programadas entrarem em vigor quando a data chega (RF10).
 *
 * <p>Deliberadamente fino: toda a logica esta em
 * {@link EmploymentChangeService#applyDue(LocalDate)}, e este componente so
 * decide <b>quando</b> chama-la. Isso mantem a regra testavel sem esperar por
 * relogio -- o teste invoca o service diretamente, passando a data que quiser.
 *
 * <p><b>Limitacao conhecida:</b> com mais de uma instancia da aplicacao no ar,
 * todas executariam o job no mesmo horario. As alteracoes nao seriam aplicadas
 * em duplicidade (a primeira transacao muda o status para APPLIED e as demais
 * nao encontram mais nada pendente), mas havera contencao desnecessaria. Em
 * producao com varias instancias, usar um lock distribuido -- ShedLock resolve
 * isso com uma anotacao.
 */
@Component
public class ScheduledChangeApplier {

    private static final Logger log = LoggerFactory.getLogger(ScheduledChangeApplier.class);

    private final EmploymentChangeService changeService;

    public ScheduledChangeApplier(EmploymentChangeService changeService) {
        this.changeService = changeService;
    }

    /**
     * Roda de madrugada, apos a virada do dia.
     *
     * <p>O cron vem de propriedade para que o ambiente de teste possa desliga-lo
     * ({@code sgc.scheduling.enabled=false}) e nao interferir nas asserções.
     */
    @Scheduled(cron = "${sgc.scheduling.apply-changes-cron:0 5 0 * * *}")
    public void applyDueChanges() {
        LocalDate today = LocalDate.now();
        log.debug("Verificando alteracoes de carreira vencidas ate {}", today);

        int applied = changeService.applyDue(today);

        if (applied > 0) {
            log.info("{} alteracao(oes) de carreira processada(s) com vigencia ate {}", applied, today);
        }
    }
}
