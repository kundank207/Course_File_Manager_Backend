package com.mitmeerut.CFM_Portal.Repository;

import com.mitmeerut.CFM_Portal.Model.PasswordResetToken;
import com.mitmeerut.CFM_Portal.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // Find latest valid token by user
    Optional<PasswordResetToken> findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(User user);

    // Find by user and OTP
    Optional<PasswordResetToken> findByUserAndOtpAndIsUsedFalse(User user, String otp);

    // Find all tokens by user
    List<PasswordResetToken> findByUser(User user);

    // Delete expired tokens (cleanup)
    void deleteByIsUsedTrue();
}
