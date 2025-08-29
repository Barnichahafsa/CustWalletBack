package org.bits.diamabankwalletf.repository;

import org.bits.diamabankwalletf.model.SavingAccount;
import org.bits.diamabankwalletf.utils.SavingAccountId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavingAccountRepository extends JpaRepository<SavingAccount, SavingAccountId> {

    List<SavingAccount> findByWalletNumber(String walletNumber);

}
