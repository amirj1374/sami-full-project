package com.sami.app;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.user.domain.ProfileFieldDefinition;
import com.sami.app.user.domain.ProfileFieldDefinition.FieldType;
import com.sami.app.user.repository.ProfileFieldDefinitionRepository;
import com.sami.app.user.service.ProfileFieldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the dynamic custom-field validation — the mechanism that lets
 * profiles gain fields at runtime without schema changes.
 */
class ProfileFieldValidationTest {

    private ProfileFieldDefinitionRepository repository;
    private ProfileFieldService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ProfileFieldDefinitionRepository.class);
        service = new ProfileFieldService(repository);
    }

    private void definitions(ProfileFieldDefinition... defs) {
        when(repository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(defs));
    }

    private ProfileFieldDefinition.ProfileFieldDefinitionBuilder field(String key, FieldType type) {
        return ProfileFieldDefinition.builder().fieldKey(key).label(key).fieldType(type);
    }

    @Test
    void unknownKeysAreRejected() {
        definitions();
        assertThatThrownBy(() -> service.validateCustomFields(Map.of("rogue", "x")))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void requiredFieldMustBePresent() {
        definitions(field("badge", FieldType.TEXT).required(true).build());
        assertThatThrownBy(() -> service.validateCustomFields(Map.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("required");
    }

    @Test
    void textRulesAreEnforced() {
        definitions(field("badge", FieldType.TEXT).minLength(3).maxLength(5).build());

        assertThatThrownBy(() -> service.validateCustomFields(Map.of("badge", "ab")))
                .hasMessageContaining("at least 3");
        assertThat(service.validateCustomFields(Map.of("badge", "abcd")))
                .containsEntry("badge", "abcd");
    }

    @Test
    void selectMustMatchAnOption() {
        definitions(field("shift", FieldType.SELECT).options(List.of("day", "night")).build());

        assertThat(service.validateCustomFields(Map.of("shift", "day")))
                .containsEntry("shift", "day");
        assertThatThrownBy(() -> service.validateCustomFields(Map.of("shift", "noon")))
                .hasMessageContaining("one of");
    }

    @Test
    void typedValuesAreCoercedAndValidated() {
        definitions(
                field("seats", FieldType.NUMBER).build(),
                field("hired", FieldType.DATE).build(),
                field("remote", FieldType.BOOLEAN).build());

        Map<String, Object> result = service.validateCustomFields(Map.of(
                "seats", "4", "hired", "2026-01-15", "remote", "true"));

        assertThat(result).containsEntry("seats", 4.0d)
                .containsEntry("hired", "2026-01-15")
                .containsEntry("remote", true);
        assertThatThrownBy(() -> service.validateCustomFields(Map.of("hired", "someday")))
                .hasMessageContaining("ISO date");
    }
}
