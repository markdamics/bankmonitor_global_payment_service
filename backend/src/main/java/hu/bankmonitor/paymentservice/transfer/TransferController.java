package hu.bankmonitor.paymentservice.transfer;

import hu.bankmonitor.paymentservice.transfer.dto.CreateTransferRequest;
import hu.bankmonitor.paymentservice.transfer.dto.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(@Valid @RequestBody CreateTransferRequest request) {
        TransferCreationResult result = transferService.createTransfer(request);
        HttpStatus status = result.newlyCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.transfer());
    }

    @GetMapping
    public List<TransferResponse> listTransfers(@RequestParam(required = false) UUID accountId) {
        if (accountId != null) {
            return transferService.listTransfersForAccount(accountId);
        }
        return transferService.listTransfers();
    }

    @GetMapping("/{id}")
    public TransferResponse getTransfer(@PathVariable UUID id) {
        return transferService.getTransfer(id);
    }
}
