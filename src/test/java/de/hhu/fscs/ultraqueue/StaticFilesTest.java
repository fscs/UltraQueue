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
public class StaticFilesTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    UltraQueueProperties props;

    @Test
    @DisplayName("song folder is served to admin user")
    public void songFolderAdmin() throws Exception {
        mvc.perform(get("/files/nonexistentFile").with(httpBasic(props.admin().username(), props.admin().password())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("song folder is served to privileged user")
    public void songFolderPrivileged() throws Exception {
        mvc.perform(get("/files/nonexistentFile").with(httpBasic(props.privilegedUser().username(), props.privilegedUser().password())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("song folder is not served to normal users")
    public void songFolderNormalUser() throws Exception {
        mvc.perform(get("/files/nonexistentFile").with(httpBasic(props.privilegedUser().username(), "wrongpassword")))
                .andExpect(status().isUnauthorized());
    }
}
