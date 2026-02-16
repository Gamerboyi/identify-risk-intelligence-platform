package com.vedant.eurds.repository;

import com.vedant.eurds.model.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    // Get all login logs for a user, newest first
    List<LoginLog> findByUserIdOrderByLoginTimestampDesc(UUID userId);

    // Count login attempts in last 24 hours — used for ML feature
    @Query("SELECT COUNT(l) FROM LoginLog l WHERE l.userId = :userId " +
            "AND l.loginTimestamp > :since")
    long countRecentLogins(@Param("userId") UUID userId,
                           @Param("since") LocalDateTime since);

    // Get all IPs this user has ever logged in from — used to detect new IP
    @Query("SELECT DISTINCT l.ipAddress FROM LoginLog l WHERE l.userId = :userId")
    List<String> findDistinctIpsByUserId(@Param("userId") UUID userId);

    // Get all devices this user has ever used — used to detect new device
    @Query("SELECT DISTINCT l.deviceInfo FROM LoginLog l WHERE l.userId = :userId")
    List<String> findDistinctDevicesByUserId(@Param("userId") UUID userId);

    // Count failed logins from a specific IP in a time window — used for brute force detection
    @Query("SELECT COUNT(l) FROM LoginLog l WHERE l.ipAddress = :ip " +
            "AND l.successFlag = false AND l.loginTimestamp > :since")
    long countFailedLoginsByIpSince(@Param("ip") String ip,
                                     @Param("since") LocalDateTime since);

    // Count failed logins for a user in a time window — used for ML feature extraction
    @Query("SELECT COUNT(l) FROM LoginLog l WHERE l.userId = :userId " +
            "AND l.successFlag = false AND l.loginTimestamp > :since")
    long countRecentFailedLogins(@Param("userId") UUID userId,
                                  @Param("since") LocalDateTime since);
}