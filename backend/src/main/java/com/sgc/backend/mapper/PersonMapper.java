package com.sgc.backend.mapper;

import com.sgc.backend.domain.entity.Address;
import com.sgc.backend.domain.entity.Person;
import com.sgc.backend.dto.request.AddressRequest;
import com.sgc.backend.dto.response.AddressResponse;
import com.sgc.backend.dto.response.PersonResponse;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;

/**
 * Conversao dos dados pessoais.
 *
 * <p>Concentra tres decisoes que valem para toda a API:
 * <ul>
 *   <li><b>mascaramento do CPF</b> -- minimizacao de dados pessoais (RNF06);</li>
 *   <li><b>calculo da idade</b> -- derivado, nunca armazenado, senao ficaria
 *       desatualizado no dia seguinte;</li>
 *   <li><b>normalizacao</b> de UF e CEP na entrada.</li>
 * </ul>
 */
public final class PersonMapper {

    private PersonMapper() {
    }

    public static PersonResponse toResponse(Person person, List<com.sgc.backend.dto.response.EmploymentSummaryResponse> employments) {
        if (person == null) {
            return null;
        }
        return new PersonResponse(
                person.getId(),
                maskCpf(person.getCpf()),
                person.getRg(),
                person.getFullName(),
                person.getBirthDate(),
                calculateAge(person.getBirthDate()),
                person.getPersonalEmail(),
                person.getGender(),
                person.getPhone(),
                person.getDependentsCount(),
                person.getDriverLicenseCategories(),
                toAddressResponse(person.getAddress()),
                employments);
    }

    public static AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(
                address.getId(),
                address.getStreet(),
                address.getNumber(),
                address.getComplement(),
                address.getDistrict(),
                address.getCity(),
                address.getState(),
                address.getZipCode());
    }

    /**
     * Aplica os dados do endereco, reaproveitando a instancia existente.
     *
     * <p>Criar um {@code Address} novo a cada edicao geraria delete + insert e
     * descartaria a auditoria original do registro.
     */
    public static Address applyAddress(Address existing, AddressRequest request) {
        if (request == null) {
            return null;
        }
        Address address = existing == null ? new Address() : existing;
        address.setStreet(request.street());
        address.setNumber(request.number());
        address.setComplement(request.complement());
        address.setDistrict(request.district());
        address.setCity(request.city());
        address.setState(request.state() == null ? null : request.state().toUpperCase(Locale.ROOT));
        address.setZipCode(digitsOnly(request.zipCode()));
        return address;
    }

    /**
     * Devolve o CPF como {@code ***.982.247-**}: o suficiente para o gestor
     * conferir que achou a pessoa certa, sem expor o documento inteiro.
     */
    public static String maskCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return "***.%s.%s-**".formatted(cpf.substring(3, 6), cpf.substring(6, 9));
    }

    public static Integer calculateAge(LocalDate birthDate) {
        return birthDate == null ? null : Period.between(birthDate, LocalDate.now()).getYears();
    }

    private static String digitsOnly(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }
}
