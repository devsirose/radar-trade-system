package com.radartrade.platform.service.payment.service;

import java.util.Map;

public interface PaymentService {
    String createPaymentUrl(String userId, String subscriptionPlanId);
    Map<String, String> handleIPNCallback(Map<String, String> vnpParams);
}
