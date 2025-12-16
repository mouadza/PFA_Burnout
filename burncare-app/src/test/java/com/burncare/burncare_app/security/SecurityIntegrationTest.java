package com.burncare.burncare_app.security;

import com.burncare.burncare_app.config.SecurityConfig;
import com.burncare.burncare_app.controllers.AdminController;
import com.burncare.burncare_app.services.AdminService;
import com.burncare.burncare_app.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// On charge le contrôleur Admin pour tester sa protection
@WebMvcTest(controllers = AdminController.class)
// ✅ IMPORTANT : On importe explicitement votre SecurityConfig pour qu'elle soit active dans le test
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // On doit mocker les services car le contrôleur va essayer de les appeler si la sécurité passe
    @MockBean
    private AdminService adminService;

    @MockBean
    private UserService userService;

    @Test
    void accessAdmin_ShouldBeForbidden_ForSimpleUser() throws Exception {
        // ACT & ASSERT
        // Simulation d'un utilisateur avec le rôle "USER" (donc PAS admin)
        // Spring Security ajoute souvent "ROLE_" automatiquement, donc on teste avec ROLE_USER

        mockMvc.perform(get("/api/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden()); // 🛑 Doit retourner 403 Forbidden
    }

    @Test
    void accessAdmin_ShouldBeAllowed_ForAdmin() throws Exception {
        // ACT & ASSERT
        // Simulation d'un utilisateur avec le rôle "ADMIN"

        mockMvc.perform(get("/api/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // ✅ Doit retourner 200 OK
    }

    @Test
    void accessAdmin_ShouldBeUnauthorized_ForAnonymous() throws Exception {
        // ACT & ASSERT
        // Pas de token fourni du tout

        mockMvc.perform(get("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // 🛑 Doit retourner 401 Unauthorized
    }
}