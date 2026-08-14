package io.github.oneofwolvesbilly.orca.referencecore.web;

import io.github.oneofwolvesbilly.orca.organization.application.OrganizationApplicationFailure;
import io.github.oneofwolvesbilly.orca.organization.application.OrganizationFailureCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrganizationFailureTranslationTest {

    private final GlobalApiExceptionHandler handler = new GlobalApiExceptionHandler();

    @Test
    void translates_typed_meaning_independently_of_message_wording() {
        assertTranslation(OrganizationFailureCategory.NOT_FOUND, "first wording", 404, "NOT_FOUND");
        assertTranslation(OrganizationFailureCategory.NOT_FOUND, "completely different", 404, "NOT_FOUND");
        assertTranslation(OrganizationFailureCategory.FORBIDDEN, "anything", 403, "FORBIDDEN");
        assertTranslation(OrganizationFailureCategory.APPLICATION_REJECTED, null, 400, "APPLICATION_REJECTED");
    }

    private void assertTranslation(
            OrganizationFailureCategory category,
            String message,
            int status,
            String code
    ) {
        var response = handler.organizationFailure(new OrganizationApplicationFailure(category, message));

        assertEquals(status, response.getStatusCode().value());
        assertEquals(code, response.getBody().code());
    }
}
