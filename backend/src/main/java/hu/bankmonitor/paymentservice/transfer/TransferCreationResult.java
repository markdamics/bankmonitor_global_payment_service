package hu.bankmonitor.paymentservice.transfer;

import hu.bankmonitor.paymentservice.transfer.dto.TransferResponse;

public record TransferCreationResult(TransferResponse transfer, boolean newlyCreated) {
}
