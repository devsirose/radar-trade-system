🔹 Các business flow cần Strong Consistency

    Những flow mà kết quả của step sau phụ thuộc trực tiếp và ngay lập tức vào step trước, hoặc user/client không thể tiếp tục nếu dữ liệu chưa đồng bộ.
    
    Đăng ký User + Khởi tạo Account mặc định
    
    Nếu yêu cầu: sau khi đăng ký, user phải login và thấy ngay account mặc định.
    
    Nếu account chưa có ngay → UX hỏng.
    
    → Strong consistency → AuthService phải gọi trực tiếp AccountService (gRPC/REST).
    
    Giao dịch tiền (Transfer / Buy / Sell)
    
    Debit từ tài khoản A, Credit vào tài khoản B.
    
    Nếu chỉ debit mà chưa credit (hoặc ngược lại) → mất tiền / tạo inconsistency nghiêm trọng.
    
    → Dùng distributed transaction / 2PC / Saga orchestration.
    
    Tùy design: có thể giữ ở trong 1 bounded context (Account Service quản lý cả debit & credit).
    
    Thanh toán (Payment / Subscription)
    
    Khi user thanh toán thành công qua cổng Payment → phải chắc chắn:
    
    Transaction ghi nhận = SUCCESS.
    
    Kích hoạt gói dịch vụ/subscription ngay.
    
    Nếu không strong consistency → user có thể bị mất tiền nhưng chưa có dịch vụ.
    
    Authentication & Authorization
    
    Token issued phải mapping đúng với user hiện tại ngay lập tức.
    
    Nếu auth service trả token nhưng user chưa tồn tại trong DB → nguy hiểm.
    
    Order Placement (Trading/Booking/E-commerce)
    
    Khi đặt lệnh trade (Buy/Sell order) → cần chắc chắn order được ghi nhận trước khi trả về client.
    
    Nếu eventual consistency → user thấy “đặt lệnh thành công” nhưng order chưa vào order book → gây thiệt hại.

🔹 Các business flow có thể Eventual Consistency

    Những flow mà dữ liệu có thể đến trễ vài giây/phút mà không ảnh hưởng đến core logic ngay lập tức.
    
    Gửi email/sms chào mừng sau khi đăng ký
    
    User signup thành công → có thể nhận email sau 5–10 giây vẫn OK.
    
    Cập nhật Analytics / Log / Audit
    
    Ví dụ: thống kê số user đăng ký, số giao dịch → không cần ngay real-time.
    
    Sync dữ liệu giữa các service phụ trợ
    
    Ví dụ: AccountService phát event AccountCreated để NotificationService hiển thị thông báo.
    
    Recommendation / ML Pipeline
    
    User hành động (buy/sell), data gửi sang Kafka cho ML service training model → không cần real-time.
    
🔹 Nguyên tắc chọn

    Nếu failure gây mất tiền, mất đơn hàng, sai quyền truy cập → Strong Consistency.
    
    Nếu failure chỉ ảnh hưởng đến thông tin phụ, báo cáo, trải nghiệm phụ → Eventual Consistency.
    
    👉 Tóm lại trong hệ thống trading như Radar Trade System:
    
    Strong consistency: Auth + Account creation, Order placement, Transfer money, Payment.
    
    Eventual consistency: Notification, Analytics, Audit, Email/SMS, Machine learning events.