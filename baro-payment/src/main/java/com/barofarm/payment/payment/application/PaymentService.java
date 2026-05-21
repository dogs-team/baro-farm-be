package com.barofarm.payment.payment.application;

import static com.barofarm.payment.payment.domain.Purpose.DEPOSIT_CHARGE;
import static com.barofarm.payment.payment.domain.Purpose.ORDER_PAYMENT;

import com.barofarm.dto.ResponseDto;
import com.barofarm.exception.CustomException;
import com.barofarm.payment.deposit.application.DepositService;
import com.barofarm.payment.deposit.application.dto.request.DepositPaymentCommand;
import com.barofarm.payment.deposit.application.dto.response.DepositPaymentInfo;
import com.barofarm.payment.payment.application.dto.request.TossPaymentConfirmCommand;
import com.barofarm.payment.payment.application.dto.response.TossPaymentConfirmInfo;
import com.barofarm.payment.payment.domain.Payment;
import com.barofarm.payment.payment.domain.PaymentOutboxEvent;
import com.barofarm.payment.payment.domain.PaymentOutboxEventRepository;
import com.barofarm.payment.payment.domain.PaymentRepository;
import com.barofarm.payment.payment.exception.PaymentErrorCode;
import com.barofarm.payment.payment.infrastructure.kafka.producer.dto.PaymentConfirmedEvent;
import com.barofarm.payment.payment.infrastructure.rest.TossPaymentClient;
import com.barofarm.payment.payment.infrastructure.rest.dto.TossPaymentResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentOutboxEventRepository paymentOutboxEventRepository;
    private final ObjectMapper objectMapper;
    private final DepositService depositService;

    @Transactional
    public ResponseDto<TossPaymentConfirmInfo> confirmPayment(UUID userId, TossPaymentConfirmCommand command) {
        Payment existingByPaymentKey = paymentRepository.findByPaymentKey(command.paymentKey())
            .orElse(null);
        if (existingByPaymentKey != null) {
            return ResponseDto.ok(TossPaymentConfirmInfo.from(existingByPaymentKey));
        }

        validateOrderPaymentNotProcessed(UUID.fromString(command.orderId()));

        TossPaymentResponse tossPayment = tossPaymentClient.confirm(command);

        Payment payment = Payment.of(userId, tossPayment, ORDER_PAYMENT);
        Payment saved = saveOrderPayment(payment);

        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
            saved.getOrderId(),
            saved.getAmount()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);

            PaymentOutboxEvent outbox = PaymentOutboxEvent.pending(
                "PAYMENT",
                saved.getId().toString(),
                "payment-confirmed",
                saved.getOrderId().toString(),
                payload
            );
            paymentOutboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new CustomException(PaymentErrorCode.OUTBOX_SERIALIZATION_FAILED);
        }

        return ResponseDto.ok(TossPaymentConfirmInfo.from(saved));
    }

    @Transactional
    public ResponseDto<TossPaymentConfirmInfo> confirmDeposit(UUID userId, TossPaymentConfirmCommand command) {
        TossPaymentResponse tossPayment = tossPaymentClient.confirm(command);

        UUID chargeId = UUID.fromString(tossPayment.orderId());

        Payment payment = Payment.of(userId, tossPayment, DEPOSIT_CHARGE);
        Payment saved = paymentRepository.save(payment);

        depositService.markDepositCharge(userId, chargeId);

        return ResponseDto.ok(TossPaymentConfirmInfo.from(saved));
    }

    @Transactional
    public ResponseDto<DepositPaymentInfo> payDeposit(UUID userId, DepositPaymentCommand command) {
        validateOrderPaymentNotProcessed(command.orderId());

        var deposit = depositService.withdraw(userId, command.amount());

        Payment payment = Payment.of(userId, command.orderId(), command.amount());
        Payment saved = saveOrderPayment(payment);

        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
            command.orderId(),
            command.amount()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);

            PaymentOutboxEvent outbox = PaymentOutboxEvent.pending(
                "PAYMENT",
                saved.getId().toString(),
                "payment-confirmed",
                saved.getOrderId().toString(),
                payload
            );
            paymentOutboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new CustomException(PaymentErrorCode.OUTBOX_SERIALIZATION_FAILED);
        }

        return ResponseDto.ok(
            DepositPaymentInfo.of(
                command.orderId(),
                command.amount(),
                deposit.getAmount()
            )
        );
    }

    private void validateOrderPaymentNotProcessed(UUID orderId) {
        Payment existingOrderPayment = paymentRepository.findByOrderId(orderId)
            .orElse(null);

        if (existingOrderPayment != null) {
            throw new CustomException(PaymentErrorCode.TOSS_PAYMENT_CONFLICT);
        }
    }

    private Payment saveOrderPayment(Payment payment) {
        try {
            return paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(PaymentErrorCode.TOSS_PAYMENT_CONFLICT);
        }
    }
}
