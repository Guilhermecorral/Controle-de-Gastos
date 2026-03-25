package com.controledegastos.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

//@Repository: access a data
@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    //JPA generate SQL automatically by the name if method
    //"findBy" + "Email" -> SELECT * FROM users WHERE email = ?
    //Optional<User>: return a user or found - avoid NullPointerException
    Optional<User> findByEmail(String email);

    //verify existence user with this email - used in register
    //avoid duplicates before trying to save
    boolean existsByEmail(String email);
}