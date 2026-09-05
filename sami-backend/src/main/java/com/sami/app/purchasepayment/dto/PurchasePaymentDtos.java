package com.sami.app.purchasepayment.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
public final class PurchasePaymentDtos { private PurchasePaymentDtos(){}
 public record CreateRequest(@NotNull @DecimalMin("1") BigDecimal amount,@NotBlank @Size(max=1000) String purpose,@Size(max=200) String supplierName,@Size(max=200) String documentReference,Long attachmentFileId){}
 public record DecisionRequest(boolean approved,@Size(max=1000) String rejectionReason,Long accountantId){}
 public record PaymentRequest(@NotNull Long treasuryAccountId,@NotBlank @Pattern(regexp="ACCOUNT_TRANSFER|CARD_TRANSFER|IBAN|FROM_BRANCH") String method,@NotNull @DecimalMin("1") BigDecimal amount,@NotNull Instant paidAt,@NotBlank @Size(max=200) String referenceNumber,Long receiptFileId,@Size(max=1000) String note,boolean completeWithPartialAmount){}
 public record LimitRequest(@NotNull Long treasuryAccountId,@NotNull LocalDate paymentDate,@NotBlank @Pattern(regexp="ACCOUNT_TRANSFER|CARD_TRANSFER|IBAN|FROM_BRANCH") String method,@NotNull @DecimalMin("0") BigDecimal limitAmount){}
 public record DelayRequest(@NotBlank @Size(max=1000) String reason){}
}
