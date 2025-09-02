package com.radartrade.platform.service.payment.controller;

import com.radartrade.platform.service.payment.service.impl.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * FE gọi để tạo URL thanh toán VNPay.
     * Gọi: GET /api/v1/payment/create?userId=...&subscriptionPlanId=...
     * Backend sẽ trả về 302 + Location để redirect sang VNPay.
     */
    @GetMapping("/create")
    public ResponseEntity<Void> createPayment(
            @RequestParam String userId,
            @RequestParam String subscriptionPlanId) {

        String paymentUrl = paymentService.createPaymentUrl(userId, subscriptionPlanId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(paymentUrl))
                .build();
    }

    /**
     * IPN endpoint – VNPay gọi tới (GET với query params).
     * Trả về JSON {"RspCode":"00","Message":"Confirm Success"} nếu hợp lệ.
     */
    @GetMapping(value = "/ipn", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> ipn(@RequestParam Map<String, String> vnpParams) {
        log.info("[VNPay IPN] params = {}", vnpParams);
        Map<String, String> response = paymentService.handleIPNCallback(vnpParams);
        return ResponseEntity.ok(response);
    }

    /**
     * Return URL – người dùng được VNPay redirect về sau khi thanh toán.
     * FE có thể đọc code từ query string để hiển thị kết quả.
     */
    @GetMapping("/return")
    public ResponseEntity<Void> returnUrl(@RequestParam Map<String, String> vnpParams) {
        log.info("[VNPay RETURN] params = {}", vnpParams);
        String code = vnpParams.get("vnp_ResponseCode");
        // Redirect về FE kèm trạng thái
        String frontendUrl = "https://your-frontend.example.com/payment-result?code=" + code;
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl))
                .build();
    }
}
