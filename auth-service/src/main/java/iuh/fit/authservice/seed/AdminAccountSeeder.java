package iuh.fit.authservice.seed;

import iuh.fit.authservice.account.entity.Account;
import iuh.fit.authservice.account.enums.AccountRole;
import iuh.fit.authservice.account.enums.AccountStatus;
import iuh.fit.authservice.account.enums.AuthProvider;
import iuh.fit.authservice.account.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminAccountSeeder.class);
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "12345678";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountSeeder(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accountRepository.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            return;
        }

        Account account = new Account();
        account.setEmail(ADMIN_EMAIL);
        account.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        account.setRole(AccountRole.ADMIN);
        account.setStatus(AccountStatus.ACTIVE);
        account.setProvider(AuthProvider.LOCAL);
        account.setEmailVerified(true);

        accountRepository.save(account);
        logger.info("Seeded admin account: {}", ADMIN_EMAIL);
    }
}
