package com.sgc.backend.service;

import com.sgc.backend.domain.entity.Employment;
import com.sgc.backend.domain.entity.EmploymentChange;
import com.sgc.backend.domain.entity.EmploymentEvent;
import com.sgc.backend.domain.enums.EmploymentEventType;
import com.sgc.backend.domain.enums.TerminationReason;
import com.sgc.backend.repository.EmploymentEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Grava os fatos na linha do tempo (RF11) -- a RN06 em codigo.
 *
 * <p>Isolado num componente proprio para que os services de negocio nao precisem
 * saber como um evento e montado, e para que exista <b>um unico lugar</b> onde a
 * timeline e escrita. Se essa logica ficasse espalhada, seria questao de tempo
 * ate alguma operacao esquecer de registrar.
 *
 * <p>Os mapas {@code previousValue}/{@code newValue} guardam apenas os campos que
 * de fato mudaram, com os valores convertidos para texto. Texto e proposital: o
 * conteudo e heterogeneo (datas, dinheiro, nomes) e serve para exibicao e
 * auditoria, nao para calculo.
 */
@Service
public class EmploymentEventRecorder {

    private final EmploymentEventRepository eventRepository;

    public EmploymentEventRecorder(EmploymentEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // ------------------------------------------------------------------
    // RF08 -- admissao e desligamento
    // ------------------------------------------------------------------

    /** Admissao. A data efetiva e a propria data de admissao, nao "hoje". */
    public void recordHire(Employment employment) {
        EmploymentEvent event = base(employment, EmploymentEventType.HIRE, employment.getHireDate());
        event.setNewValue(map(
                "hireDate", employment.getHireDate(),
                "registrationNumber", employment.getRegistrationNumber(),
                "salary", employment.getSalary(),
                "jobPosition", nameOf(employment.getJobPosition() == null ? null : employment.getJobPosition().getTitle()),
                "department", nameOf(employment.getDepartment() == null ? null : employment.getDepartment().getName())));
        eventRepository.save(event);
    }

    public void recordTermination(Employment employment, LocalDate date, TerminationReason reason, String notes) {
        EmploymentEvent event = base(employment, EmploymentEventType.TERMINATION, date);
        event.setReason(reason == null ? null : reason.name());
        event.setNotes(notes);
        event.setNewValue(map(
                "terminationDate", date,
                "reason", reason));
        eventRepository.save(event);
    }

    // ------------------------------------------------------------------
    // RF06 -- inativacao
    // ------------------------------------------------------------------

    public void recordInactivation(Employment employment) {
        eventRepository.save(base(employment, EmploymentEventType.INACTIVATION, LocalDate.now()));
    }

    public void recordActivation(Employment employment) {
        eventRepository.save(base(employment, EmploymentEventType.ACTIVATION, LocalDate.now()));
    }

    // ------------------------------------------------------------------
    // RF05 / RN06 -- alteracoes
    // ------------------------------------------------------------------

    /**
     * Registra uma alteracao pontual com o tipo especifico.
     *
     * <p>Usado quando salario, cargo ou setor mudam: a RN06 exige que essas tres
     * mudancas aparecam na timeline, e com tipo proprio elas nao se perdem no
     * meio das edicoes cadastrais.
     */
    public void recordFieldChange(Employment employment,
                                  EmploymentEventType type,
                                  String field,
                                  Object previous,
                                  Object current) {
        EmploymentEvent event = base(employment, type, LocalDate.now());
        event.setPreviousValue(map(field, previous));
        event.setNewValue(map(field, current));
        eventRepository.save(event);
    }

    /**
     * Registra o efeito de uma alteracao de carreira aplicada (RF10 / RN06).
     *
     * <p>Uma alteracao pode mexer em salario, cargo e setor ao mesmo tempo. Cada
     * um vira um evento proprio, com seu tipo especifico, todos apontando para a
     * mesma {@link EmploymentChange} e com a mesma data de vigencia -- assim o
     * relatorio consegue contar "quantas promocoes" separado de "quantos
     * reajustes", sem perder o vinculo entre eles.
     */
    public void recordAppliedChange(EmploymentChange change,
                                    EmploymentEventType type,
                                    String field,
                                    Object previous,
                                    Object current) {

        EmploymentEvent event = base(change.getEmployment(), type, change.getEffectiveDate());
        event.setReason(change.getReason().name());
        event.setNotes(change.getNotes());
        event.setEmploymentChange(change);
        event.setPreviousValue(map(field, previous));
        event.setNewValue(map(field, current));
        eventRepository.save(event);
    }

    /**
     * Registra a edicao de dados cadastrais, com o conjunto de campos alterados.
     *
     * <p>Nao grava nada quando o diff esta vazio: um PUT que reenvia os mesmos
     * valores nao e uma alteracao e nao deve poluir a timeline.
     */
    public void recordProfileUpdate(Employment employment,
                                    Map<String, Object> previous,
                                    Map<String, Object> current) {
        if (current == null || current.isEmpty()) {
            return;
        }
        EmploymentEvent event = base(employment, EmploymentEventType.PROFILE_UPDATE, LocalDate.now());
        event.setPreviousValue(previous);
        event.setNewValue(current);
        eventRepository.save(event);
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private static EmploymentEvent base(Employment employment, EmploymentEventType type, LocalDate effectiveDate) {
        EmploymentEvent event = new EmploymentEvent();
        event.setEmployment(employment);
        event.setEventType(type);
        event.setEffectiveDate(effectiveDate == null ? LocalDate.now() : effectiveDate);
        // performedBy e createdAt sao preenchidos pelo JPA Auditing.
        return event;
    }

    /**
     * Monta o mapa do evento a partir de pares chave/valor, descartando os nulos.
     *
     * <p>{@code LinkedHashMap} para que a ordem dos campos na resposta seja
     * estavel -- diff instavel e ruim de ler.
     */
    private static Map<String, Object> map(Object... keyValuePairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            Object value = keyValuePairs[i + 1];
            if (value != null) {
                result.put(String.valueOf(keyValuePairs[i]), String.valueOf(value));
            }
        }
        return result.isEmpty() ? null : result;
    }

    private static String nameOf(String value) {
        return value;
    }

    /** Comparacao null-safe usada pelos services para montar o diff. */
    public static boolean changed(Object previous, Object current) {
        return !Objects.equals(previous, current);
    }
}
