package hu.bankmonitor.paymentservice.transfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    List<Transfer> findAllByOrderByCreatedAtDesc();

    List<Transfer> findBySourceAccountIdOrTargetAccountIdOrderByCreatedAtDesc(UUID sourceAccountId, UUID targetAccountId);

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}
