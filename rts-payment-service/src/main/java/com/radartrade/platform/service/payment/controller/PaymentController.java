package com.radartrade.platform.service.payment.controller;

import com.radartrade.platform.service.payment.service.impl.PaymentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Receive request,process subscription then redirect with params to payment gateway
     * @param userId the ID of the user making the payment
     * @return ResponseEntity<redirectUrl_with_params></>
     * params according to: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html#danh-s%C3%A1ch-tham-s%E1%BB%91
     * redirectUrl : https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
     */
     @GetMapping
     public ResponseEntity<Void> initiatePayment(@RequestParam String userId) {
         String redirectUrl = paymentService.createPaymentUrl(userId);

         return ResponseEntity
                 .status(HttpStatus.FOUND)
                 .header(HttpHeaders.LOCATION, redirectUrl)
                 .build();
     }

     @GetMapping("/return-url")
     public ResponseEntity<Void> returnUrl(@RequestParam Map<String, String> vnpParams) {
         // Process the return URL parameters from the payment gateway
         // This is notified used to confirm the payment status
         // You are not allowed to update the subscription status here
         // You should validate the parameters and check the payment status
         // For now, we just return OK
         return ResponseEntity.ok().build();
     }


    /**
     * Instant Payment Notification (IPN) callback endpoint (whether success or failure)
     * This endpoint is called by the payment gateway to notify about the payment status.
     * It should handle the notification and update the subscription status accordingly.
     * @param vnpParams according to (https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html#danh-s%C3%A1ch-tham-s%E1%BB%91-1)
     * requirement: IPN URL must have SSL certificate (https) and domain must be registered with the payment gateway.
     * action: update subscription status in database, send notification to user, etc.
     * Note: This endpoint should be secured and validate the request to ensure it comes from the payment gateway.
     * @return
     */

    @GetMapping("/callbackIPN")
    public ResponseEntity<?> callbackIPN(@RequestParam Map<String, String> vnpParams) {

        return ResponseEntity.ok().build();
    }
}
