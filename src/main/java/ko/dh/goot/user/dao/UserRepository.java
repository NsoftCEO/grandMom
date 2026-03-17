package ko.dh.goot.user.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ko.dh.goot.user.domain.User;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
