package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.EpsProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EpsProfileRepository extends JpaRepository<EpsProfile, String> {
    Optional<EpsProfile> findByCode(String id);
}
