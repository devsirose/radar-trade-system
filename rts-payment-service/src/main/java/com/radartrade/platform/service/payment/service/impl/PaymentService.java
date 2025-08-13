package com.radartrade.platform.service.payment.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class PaymentService {
    @Value("${payment.provider.vnpay.terminal-id}")
    private String vnp_TmnCode;
    @Value("${payment.provider.vnpay.secret-key}")
    private String vnp_HashSecret;

    /**
     * Create a payment URL for VNPAY
     * Use userId to identify the user (updated subscription status for user) and subscriptionPlanId to identify the subscription plan (amount)
     * @param userId
     * @param subscriptionPlanId
     * @return
     */
    public String createPaymentUrl(String userId) {
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        // @fixme : use subscriptionPlanId to get the amount from database
        vnp_Params.put("vnp_Amount", "1000000");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String createDate = formatter.format(new Date());
        String expireDate = formatter.format(new Date(System.currentTimeMillis() + 15 * 60 * 1000));

        vnp_Params.put("vnp_CreateDate", createDate);
        vnp_Params.put("vnp_ExpireDate", expireDate);
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang 123");
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_ReturnUrl", "https://radartrade.com/api/v1/payment/return-url");
        vnp_Params.put("vnp_TxnRef", "123");


        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(fieldValue);
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                if (i < fieldNames.size() - 1) {
                    hashData.append('&');
                    query.append('&');
                }
            }
        }


        String vnp_SecureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);

        String returnURL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?" + query;

        return returnURL;
    }

    public static String hmacSHA512(String key, String data)  {
        Mac hmac512 = null;
        try {
            hmac512 = Mac.getInstance("HmacSHA512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        try {
            hmac512.init(secretKey);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        byte[] hashBytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();
        for (byte b : hashBytes) {
            hash.append(String.format("%02x", b));
        }
        return hash.toString();
    }
}
