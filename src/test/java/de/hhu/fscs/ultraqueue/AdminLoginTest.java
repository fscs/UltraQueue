package de.hhu.fscs.ultraqueue;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminLoginTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UltraQueueProperties props;

    @Test
    @DisplayName("/admin/ is protected")
    void testAdminLoginSuccess() throws Exception {
        mvc.perform(get("/admin/").with(httpBasic(props.admin().username(), props.admin().password())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("/admin/ is protected")
    void testAdminLoginFailure() throws Exception {
        mvc.perform(get("/admin/").with(httpBasic(props.admin().username(), "wrongpassword")))
                .andExpect(status().isUnauthorized());
    }
}
