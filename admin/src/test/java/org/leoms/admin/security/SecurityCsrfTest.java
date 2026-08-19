package org.leoms.admin.security;

import org.junit.jupiter.api.Test;
import org.leoms.admin.backup.BackupService;
import org.leoms.admin.config.SecurityConfig;
import org.leoms.admin.web.BackupController;
import org.leoms.admin.web.LoginController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({BackupController.class, LoginController.class})
@Import({SecurityConfig.class, LoginAttemptService.class})
class SecurityCsrfTest {
    private static final Path hashFile;
    @Autowired MockMvc mvc;
    @MockBean BackupService backups;

    static {
        try {
            hashFile = Files.createTempFile("leoms-test-admin", ".hash");
            Files.writeString(hashFile, new BCryptPasswordEncoder(12).encode("temporary test password"));
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("leoms.admin.password-hash-file", () -> hashFile.toString());
        registry.add("leoms.admin.username", () -> "owner");
    }

    @Test
    void mutationWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/backups/request").with(user("owner").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedMutationWithCsrfIsAccepted() throws Exception {
        mvc.perform(post("/backups/request").with(user("owner").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
