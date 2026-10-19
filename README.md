# BÀI THỰC HÀNH 3 - BÀI TẬP 1: HOTEL BOOKING SERVICE UNIT TESTING
**Môn học:** Nguyên lý xây dựng phần mềm (CT240)  
**Công nghệ sử dụng:** Java 17+, Maven, JUnit 5, Mockito 5, Jacoco Plugin

---

## 1. Cấu trúc Dự án (Project Structure)

```text
hotel-booking-service/
├── .mvn/wrapper/                  # Maven Wrapper configuration & jar
├── pom.xml                        # Cấu hình dependencies JUnit 5, Mockito & Jacoco Plugin
├── mvnw / mvnw.cmd                # Maven Wrapper scripts
├── src/
│   ├── main/java/com/ctu/
│   │   ├── model/
│   │   │   ├── Room.java                  # Model phòng khách sạn
│   │   │   └── BookingReceipt.java        # DTO biên lai đặt phòng
│   │   ├── exception/
│   │   │   ├── RoomNotAvailableException.java  # Ngoại lệ phòng không khả dụng / đã đặt
│   │   │   └── PaymentFailedException.java     # Ngoại lệ thanh toán thất bại
│   │   ├── repository/
│   │   │   └── RoomRepository.java        # Interface CSDL phòng (Dependency)
│   │   └── service/
│   │       ├── PaymentGateway.java        # Interface cổng thanh toán (Dependency)
│   │       └── BookingService.java        # Nghiệp vụ đặt phòng cốt lõi (Target)
│   └── test/java/com/ctu/
│       ├── service/
│       │   └── BookingServiceTest.java    # Bộ Unit Test đầy đủ cho BookingService
│       └── model/
│           ├── RoomTest.java              # Unit Test cho Room Entity
│           └── BookingReceiptTest.java    # Unit Test cho BookingReceipt DTO
└── target/
    └── site/jacoco/index.html     # Báo cáo độ bao phủ mã kiểm thử (Jacoco HTML)
```

---

## 2. Chi tiết các Ca Kiểm thử (Test Cases)

Bộ kiểm thử tuân thủ **Mô hình 3A (Arrange - Act - Assert)** và **Nguyên tắc FIRST**:
- **Fast:** Thực thi toàn bộ test suite trong dưới 1 giây.
- **Independent / Isolated:** Không phụ thuộc database thật hay mạng bên ngoài (cô lập với Mockito `@Mock`).
- **Repeatable:** Chạy lại ở bất kỳ môi trường nào đều cho ra kết quả đồng nhất.
- **Self-validating:** Đánh giá tự động qua các câu lệnh `assertEquals`, `assertTrue`, `assertThrows`, `verify`.
- **Timely:** Thiết kế bám sát từng nhánh điều kiện và ngoại lệ nghiệp vụ.

### Danh sách Test Cases trong `BookingServiceTest`:

1. **Luồng thành công (Happy Path):**
   - `testBookRoom_Success`:
     - Kiểm tra đặt phòng thành công, tính đúng tổng tiền (`pricePerNight * nights`).
     - Kiểm tra trạng thái phòng được cập nhật thành `isAvailable == false`.
     - Dùng `verify()` kiểm tra `roomRepository.findById()`, `paymentGateway.charge()` và `roomRepository.save()` đều được gọi đúng **1 lần**.

2. **Kiểm thử Ngoại lệ Nghiệp vụ (Business Exceptions):**
   - `testBookRoom_RoomNotFound_ThrowsException`: Khi `findById()` trả về `Optional.empty()`, ném ra `RoomNotAvailableException("Khong tim thay phong: ...")`. Không tiến hành thanh toán hay lưu CSDL.
   - `testBookRoom_RoomAlreadyBooked_ThrowsException`: Khi phòng đã có người đặt (`isAvailable() == false`), ném ra `RoomNotAvailableException("Phong da duoc dat truoc do: ...")`. Không tiến hành thanh toán hay lưu CSDL.
   - `testBookRoom_PaymentFailed_ThrowsException`: Khi `charge()` trả về `false`, ném ra `PaymentFailedException("Thanh toan qua the credit card that bai!")`. Không lưu phòng vào CSDL và trạng thái phòng giữ nguyên.

3. **Kiểm thử Tham số Biên & Rẽ nhánh (Boundary & Edge Cases):**
   - `testBookRoom_InvalidNights_ThrowsException`: Dùng `@ParameterizedTest` với các giá trị `nights = 0, -1, -10`. Ném ra `IllegalArgumentException`.
   - `testBookRoom_NullOrEmptyCreditCard_ThrowsException`: Dùng `@ParameterizedTest` với `@NullAndEmptySource` và `@ValueSource(strings = {"   ", "\t", "\n"})`. Ném ra `IllegalArgumentException`.
   - `testBookRoom_NullRoomId_ThrowsException`: Kiểm tra `roomId == null`. Ném ra `IllegalArgumentException`.

---

## 3. Hướng dẫn Chạy Kiểm thử & Xem Báo cáo Jacoco

### Cách 1: Chạy bằng dòng lệnh (Terminal / PowerShell)

Trong thư mục `hotel-booking-service`:

```powershell
# Sử dụng Maven Wrapper có sẵn trong thư mục
.\mvnw.cmd clean test

# Hoặc nếu máy đã cấu hình biến môi trường Maven:
mvn clean test
```

### Cách 2: Chạy trong IntelliJ IDEA / Eclipse

1. Mở dự án `hotel-booking-service` vào IntelliJ IDEA.
2. Mở tab **Maven** ở góc phải màn hình > `Lifecycle` > Nhấp đúp vào **clean**, sau đó nhấp đúp vào **test**.
3. Chạy trực tiếp file `BookingServiceTest.java` bằng cách bấm chuột phải > chọn **Run 'BookingServiceTest'**.

---

## 4. Kết quả Thực thi & Độ bao phủ (Jacoco Coverage)

- **Trạng thái:** 15/15 tests PASS (100% Green Bar).
- **Line Coverage:** **100%** (Vượt chỉ tiêu >= 90%).
- **Branch Coverage:** **100%** (Vượt chỉ tiêu >= 85%).

Báo cáo chi tiết có thể mở trực tiếp tại trình duyệt:
`hotel-booking-service/target/site/jacoco/index.html`
