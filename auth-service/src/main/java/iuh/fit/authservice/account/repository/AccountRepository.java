package iuh.fit.authservice.account.repository;

import iuh.fit.authservice.account.entity.Account;
import iuh.fit.authservice.account.enums.AccountRole;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Account> findByEmailIgnoreCase(String email);

    List<Account> findByRole(AccountRole role);

    Optional<Account> findById(UUID id);
}
