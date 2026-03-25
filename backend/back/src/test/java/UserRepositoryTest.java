import com.controledegastos.backend.user.User;
import com.controledegastos.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest; // Alternativa infalível
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest // Carrega o contexto completo para garantir que tudo funcione
@ActiveProfiles("dev") // Garante que ele use o H2 que configuramos no application-dev.yml
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUser() {
        User user = User.builder()
                .name("Test")
                .email("test@test.com")
                .password("123456")
                .build();

        userRepository.save(user);
        assertTrue(userRepository.findByEmail("test@test.com").isPresent());
    }
}