package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.SecretQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecretQuestionRepository extends JpaRepository<SecretQuestion, String> {
}
