package com.sgc.backend.service;

import com.sgc.backend.domain.entity.Employment;
import com.sgc.backend.domain.entity.Person;
import com.sgc.backend.dto.response.EmploymentSummaryResponse;
import com.sgc.backend.dto.response.PersonResponse;
import com.sgc.backend.exception.ResourceNotFoundException;
import com.sgc.backend.mapper.EmploymentMapper;
import com.sgc.backend.mapper.PersonMapper;
import com.sgc.backend.repository.EmploymentRepository;
import com.sgc.backend.repository.PersonRepository;
import com.sgc.backend.validation.CpfValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consulta da pessoa fisica e do seu historico de vinculos.
 *
 * <p>Esta visao passou a existir quando a readmissao virou um vinculo novo:
 * antes, "o colaborador" e "a pessoa" eram a mesma linha, e a pergunta "essa
 * pessoa ja trabalhou aqui?" nao tinha onde ser respondida.
 */
@Service
public class PersonService {

    private final PersonRepository personRepository;
    private final EmploymentRepository employmentRepository;

    public PersonService(PersonRepository personRepository, EmploymentRepository employmentRepository) {
        this.personRepository = personRepository;
        this.employmentRepository = employmentRepository;
    }

    /** A pessoa e todos os seus periodos de contrato, do mais recente ao mais antigo. */
    @Transactional(readOnly = true)
    public PersonResponse findByCpf(String cpf) {
        String normalized = CpfValidator.normalize(cpf);

        Person person = personRepository.findByCpf(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma pessoa cadastrada com o CPF informado."));

        return toResponse(person);
    }

    @Transactional(readOnly = true)
    public PersonResponse findById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Pessoa", id));
        return toResponse(person);
    }

    private PersonResponse toResponse(Person person) {
        List<EmploymentSummaryResponse> employments = employmentRepository
                .findByPersonIdOrderByHireDateDescIdDesc(person.getId())
                .stream()
                .map(EmploymentMapper::toEmploymentSummary)
                .toList();

        return PersonMapper.toResponse(person, employments);
    }
}
