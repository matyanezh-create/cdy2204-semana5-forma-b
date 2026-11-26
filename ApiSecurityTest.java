package cl.duoc.eventos;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityTest {
    private static final String ROUTE = "/api/eventos";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder decoder;

    @Test
    void missingAndInvalidTokensReturn401() throws Exception {
        when(decoder.decode("invalid")).thenThrow(new BadJwtException("JWT inválido"));

        mvc.perform(get(ROUTE)).andExpect(status().isUnauthorized());
        mvc.perform(get(ROUTE).header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessRequiresTheConfiguredScope() throws Exception {
        when(decoder.decode("read")).thenReturn(token("read", "openid"));
        when(decoder.decode("access")).thenReturn(token("access", "openid resumen/acceso"));

        mvc.perform(get(ROUTE).header("Authorization", "Bearer read"))
                .andExpect(status().isForbidden());
        mvc.perform(get(ROUTE).header("Authorization", "Bearer access"))
                .andExpect(status().isOk());
    }

    private Jwt token(String value, String scope) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(value).header("alg", "RS256")
                .subject("demo").claim("scope", scope)
                .issuedAt(now).expiresAt(now.plusSeconds(300)).build();
    }
}

