package com.radartrade.platform.service.payment.service.impl;

import com.radartrade.platform.service.payment.domain.Subscription;
import com.radartrade.platform.service.payment.domain.SubscriptionPlan;
import com.radartrade.platform.service.payment.domain.Transaction;
import com.radartrade.platform.service.payment.domain.User;
import com.radartrade.platform.service.payment.domain.valueobject.SubscriptionStatus;
import com.radartrade.platform.service.payment.domain.valueobject.TransactionStatus;
import com.radartrade.platform.service.payment.domain.valueobject.TransactionType;
import com.radartrade.platform.service.payment.repsitory.SubscriptionPlanRepository;
import com.radartrade.platform.service.payment.repsitory.SubscriptionRepository;
import com.radartrade.platform.service.payment.repsitory.TransactionRepository;
import com.radartrade.platform.service.payment.repsitory.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PaymentService {

    // THÊM: Logger để theo dõi lỗi tốt hơn
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;

    @Value("${vnpay.tmn-code}")
    private String vnp_TmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnp_HashSecret;

    @Value("${vnpay.url}")
    private String vnp_Url;

    @Value("${vnpay.return-url}")
    private String vnp_ReturnUrl;

    public PaymentService(SubscriptionRepository subscriptionRepository,
                          SubscriptionPlanRepository subscriptionPlanRepository,
                          TransactionRepository transactionRepository,
                          UserRepository userRepository,
                          KeycloakAdminService keycloakAdminService) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
    }

    @Transactional(rollbackFor = Exception.class)
    public String createPaymentUrl(String userId, String subscriptionPlanId) {
        // ... (phần code này đã đúng, không cần thay đổi)
        UUID uid = UUID.fromString(userId);
        UUID planId = UUID.fromString(subscriptionPlanId);
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found: " + planId));
        upsertUserIfMissing(uid);
        Subscription subscription = new Subscription();
        subscription.setUser(uid);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCreatedAt(Instant.now());
        subscription.setUpdatedAt(Instant.now());
        subscription = subscriptionRepository.save(subscription);
        Transaction transaction = new Transaction();
        transaction.setUser(uid);
        transaction.setSubscription(subscription);
        transaction.setAmount(plan.getPrice());
        transaction.setCurrency(plan.getCurrency());
        transaction.setType(TransactionType.PAYMENT);
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCreatedAt(Instant.now());
        transaction = transactionRepository.save(transaction);
        String vnp_TxnRef = transaction.getId().toString();
        try {
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String vnp_OrderInfo = "Thanh toan goi dang ky " + plan.getName();
            String orderType = "other";
            String vnp_IpAddr = "127.0.0.1";
            BigDecimal amount = plan.getPrice().multiply(new BigDecimal("100"));
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount.longValue()));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
            vnp_Params.put("vnp_OrderType", orderType);
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString())).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                        query.append('&');
                    }
                }
            }
            String vnp_SecureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
            String queryUrl = query + "&vnp_SecureHash=" + vnp_SecureHash;
            return vnp_Url + "?" + queryUrl;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to create payment URL", e);
        }
    }

    private void upsertUserIfMissing(UUID userId) {
        // ... (phần code này đã đúng, không cần thay đổi)
        userRepository.findById(userId).orElseGet(() -> {
            User u = new User();
            u.setId(userId);
            String prefix = userId.toString().substring(0, 8);
            u.setUsername("user-" + prefix);
            u.setEmail(prefix + "@placeholder.local");
            u.setCreatedAt(Instant.now());
            return userRepository.save(u);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> handleIPNCallback(Map<String, String> vnpParams) {
        Map<String, String> response = new HashMap<>();
        try {
            String vnp_SecureHash = vnpParams.get("vnp_SecureHash");
            vnpParams.remove("vnp_SecureHash");
            vnpParams.remove("vnp_SecureHashType");

            String signValue = hashAllFields(vnpParams);

            if (signValue.equals(vnp_SecureHash)) {
                UUID transactionId = UUID.fromString(vnpParams.get("vnp_TxnRef"));
                Transaction transaction = transactionRepository.findById(transactionId).orElse(null);

                if (transaction == null) {
                    response.put("RspCode", "01");
                    response.put("Message", "Order not found");
                    return response;
                }

                if (transaction.getStatus() != TransactionStatus.PENDING) {
                    response.put("RspCode", "02");
                    response.put("Message", "Order already confirmed");
                    return response;
                }

                String responseCode = vnpParams.get("vnp_ResponseCode");
                Subscription subscription = transaction.getSubscription();

                if ("00".equals(responseCode)) {
                    transaction.setStatus(TransactionStatus.COMPLETED);
                    subscription.setCurrentPeriodStart(Instant.now());
                    subscription.setCurrentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS));
                    log.info("Payment successful for user {}. Preparing to assign VIP role.", subscription.getUser());

                    try {
                        keycloakAdminService.assignVipRoleToUser(subscription.getUser()).block();
                        log.info("Successfully assigned VIP role to user {}", subscription.getUser());
                    } catch (Exception e) {

                        log.error("CRITICAL: Payment for user {} was successful, but assigning VIP role in Keycloak failed.", subscription.getUser(), e);
                    }


                } else {
                    transaction.setStatus(TransactionStatus.FAILED);
                    subscription.setStatus(SubscriptionStatus.CANCELLED);
                    subscription.setCancelledAt(Instant.now());
                }
                transactionRepository.save(transaction);
                subscriptionRepository.save(subscription);

                response.put("RspCode", "00");
                response.put("Message", "Confirm Success");
            } else {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Signature");
            }
        } catch (Exception e) {
            log.error("An unexpected error occurred in handleIPNCallback", e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }
        return response;
    }

    private String hmacSHA512(final String key, final String data) {

        try {
            if (key == null || data == null) throw new NullPointerException();
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            final SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate HMAC SHA512", ex);
        }
    }

    private String hashAllFields(Map<String, String> fields) throws UnsupportedEncodingException {

        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                if (itr.hasNext()) hashData.append('&');
            }
        }
        return hmacSHA512(vnp_HashSecret, hashData.toString());
    }
}