package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.constant.OtpPurpose;
import com.nhom7.coworkingspace.entity.OtpToken;
import com.nhom7.coworkingspace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    void deleteByUserAndPurpose(User user, OtpPurpose purpose);
}
