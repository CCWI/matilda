package edu.hm.ccwi.matilda.auth.repository;


import edu.hm.ccwi.matilda.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  public User findByUsername(String username);

}
