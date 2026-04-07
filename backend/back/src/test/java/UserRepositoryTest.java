import com.controledegastos.backend.BackendApplication; // Imports the application class explicitly for Spring Boot test startup.
import com.controledegastos.backend.user.User;
import com.controledegastos.backend.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest; // Alternativa infalível
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BackendApplication.class) // Loads the exact Spring Boot application class even from the default package.
@ActiveProfiles("test") // Forces the test to use the isolated test profile instead of the development profile.
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUser() {
        String email = "test+" + System.nanoTime() + "@test.com"; // Generates a unique email so repeated in-memory runs do not collide.
        User user = User.builder()
                .name("Test")
                .email(email)
                .password("123456")
                .role(User.Role.USER) // Sets the role explicitly to avoid null values in the entity.
                .build();

        userRepository.save(user);
        assertTrue(userRepository.findByEmail(email).isPresent());
    }
}
