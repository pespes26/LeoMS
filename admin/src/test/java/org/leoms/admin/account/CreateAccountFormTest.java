package org.leoms.admin.account;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateAccountFormTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsCosmicCompatibleCredentials() {
        CreateAccountForm form = form("Player10", "correct horse battery", "1234", "123456");
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void rejectsInvalidUsernamePinPicAndShortPassword() {
        CreateAccountForm form = form("bad user", "short", "12x4", "12345");
        assertThat(validator.validate(form)).hasSize(4);
    }

    private CreateAccountForm form(String user, String password, String pin, String pic) {
        CreateAccountForm form = new CreateAccountForm();
        form.setUsername(user); form.setPassword(password); form.setPin(pin); form.setPic(pic);
        return form;
    }
}
