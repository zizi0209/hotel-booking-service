package com.ctu.service;

import com.ctu.exception.PaymentFailedException;
import com.ctu.exception.RoomNotAvailableException;
import com.ctu.model.BookingReceipt;
import com.ctu.model.Room;
import com.ctu.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Test Suite")
class BookingServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private BookingService bookingService;

    // =========================================================================
    // 1. LUỒNG THÀNH CÔNG (HAPPY PATH)
    // =========================================================================
    @Test
    @DisplayName("Dat phong thanh cong: cap nhat isAvailable = false, charge & save duoc goi dung 1 lan")
    void testBookRoom_Success() {
        // 1. ARRANGE
        String roomId = "ROOM_101";
        int nights = 3;
        double pricePerNight = 500000.0;
        double expectedTotal = pricePerNight * nights; // 1,500,000.0
        String creditCard = "1234-5678-9012-3456";

        Room availableRoom = new Room(roomId, pricePerNight, true);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(availableRoom));
        when(paymentGateway.charge(creditCard, expectedTotal)).thenReturn(true);

        // 2. ACT
        BookingReceipt receipt = bookingService.bookRoom(roomId, nights, creditCard);

        // 3. ASSERT
        // Kiem tra receipt hop le
        assertNotNull(receipt, "Receipt khong duoc null");
        assertNotNull(receipt.getBookingId(), "Booking ID khong duoc null");
        assertEquals(roomId, receipt.getRoomId(), "Room ID trong receipt phai khop");
        assertEquals(expectedTotal, receipt.getTotalAmount(), 0.001, "Tong tien phai dung theo don gia * so dem");

        // Kiem tra trang thai phong chuyen thanh false
        assertFalse(availableRoom.isAvailable(), "Phong phai chuyen sang trang thai khong con trong (isAvailable = false)");

        // VERIFY: Mock interactions
        verify(roomRepository, times(1)).findById(roomId);
        verify(paymentGateway, times(1)).charge(creditCard, expectedTotal);
        verify(roomRepository, times(1)).save(availableRoom);
        verifyNoMoreInteractions(roomRepository, paymentGateway);
    }

    // =========================================================================
    // 2. KIỂM THỬ NGOẠI LỆ NGHIỆP VỤ (BUSINESS EXCEPTIONS)
    // =========================================================================
    @Nested
    @DisplayName("Kiem thu Ngoai le Nghiep vu")
    class BusinessExceptionsTest {

        @Test
        @DisplayName("Nem RoomNotAvailableException khi findById() tra ve Optional.empty()")
        void testBookRoom_RoomNotFound_ThrowsException() {
            // ARRANGE
            String roomId = "ROOM_404";
            when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

            // ACT & ASSERT
            RoomNotAvailableException exception = assertThrows(
                    RoomNotAvailableException.class,
                    () -> bookingService.bookRoom(roomId, 2, "1234-5678-9012-3456")
            );

            assertEquals("Khong tim thay phong: " + roomId, exception.getMessage());

            // VERIFY: Khong tien hanh thanh toan hay luu DB
            verify(roomRepository, times(1)).findById(roomId);
            verify(paymentGateway, never()).charge(anyString(), anyDouble());
            verify(roomRepository, never()).save(any(Room.class));
        }

        @Test
        @DisplayName("Nem RoomNotAvailableException khi phong da co nguoi dat (isAvailable == false)")
        void testBookRoom_RoomAlreadyBooked_ThrowsException() {
            // ARRANGE
            String roomId = "ROOM_102";
            Room bookedRoom = new Room(roomId, 600000.0, false);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(bookedRoom));

            // ACT & ASSERT
            RoomNotAvailableException exception = assertThrows(
                    RoomNotAvailableException.class,
                    () -> bookingService.bookRoom(roomId, 2, "1234-5678-9012-3456")
            );

            assertEquals("Phong da duoc dat truoc do: " + roomId, exception.getMessage());

            // VERIFY: Khong goi thanh toan hay luu CSDL
            verify(roomRepository, times(1)).findById(roomId);
            verify(paymentGateway, never()).charge(anyString(), anyDouble());
            verify(roomRepository, never()).save(any(Room.class));
        }

        @Test
        @DisplayName("Nem PaymentFailedException khi paymentGateway.charge() tra ve false")
        void testBookRoom_PaymentFailed_ThrowsException() {
            // ARRANGE
            String roomId = "ROOM_103";
            int nights = 2;
            double pricePerNight = 400000.0;
            double totalAmount = pricePerNight * nights; // 800,000.0
            String creditCard = "9999-8888-7777-6666";

            Room room = new Room(roomId, pricePerNight, true);
            when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
            when(paymentGateway.charge(creditCard, totalAmount)).thenReturn(false);

            // ACT & ASSERT
            PaymentFailedException exception = assertThrows(
                    PaymentFailedException.class,
                    () -> bookingService.bookRoom(roomId, nights, creditCard)
            );

            assertEquals("Thanh toan qua the credit card that bai!", exception.getMessage());

            // VERIFY: Da goi findById va charge, nhung khong luu phong vao CSDL
            verify(roomRepository, times(1)).findById(roomId);
            verify(paymentGateway, times(1)).charge(creditCard, totalAmount);
            verify(roomRepository, never()).save(any(Room.class));
            // Phong van giu trang thai isAvailable = true vi thanh toan that bai truoc khi doi trang thai
            assertTrue(room.isAvailable(), "Trang thai phong khong duoc thay doi khi thanh toan that bai");
        }
    }

    // =========================================================================
    // 3. KIỂM THỬ THAM SỐ BIÊN & EDGE CASES (BOUNDARY CONDITIONS)
    // =========================================================================
    @Nested
    @DisplayName("Kiem thu Tham so Bien (Edge Cases)")
    class EdgeCasesTest {

        @ParameterizedTest(name = "Nights = {0} khong hop le -> Nem IllegalArgumentException")
        @ValueSource(ints = {0, -1, -10})
        @DisplayName("Nem IllegalArgumentException khi so dem nights <= 0")
        void testBookRoom_InvalidNights_ThrowsException(int invalidNights) {
            // ACT & ASSERT
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> bookingService.bookRoom("ROOM_101", invalidNights, "1234-5678-9012-3456")
            );

            assertEquals("Tham so dat phong khong hop le!", exception.getMessage());

            // VERIFY: Khong tuong tac voi repository hay paymentGateway
            verifyNoInteractions(roomRepository, paymentGateway);
        }

        @ParameterizedTest(name = "Credit card: ''{0}'' khong hop le -> Nem IllegalArgumentException")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Nem IllegalArgumentException khi creditCard la null, rong hoac chi chua khoang trang")
        void testBookRoom_NullOrEmptyCreditCard_ThrowsException(String invalidCard) {
            // ACT & ASSERT
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> bookingService.bookRoom("ROOM_101", 2, invalidCard)
            );

            assertEquals("Tham so dat phong khong hop le!", exception.getMessage());

            // VERIFY: Khong tuong tac voi bat ky mock nao
            verifyNoInteractions(roomRepository, paymentGateway);
        }

        @Test
        @DisplayName("Nem IllegalArgumentException khi roomId la null")
        void testBookRoom_NullRoomId_ThrowsException() {
            // ACT & ASSERT
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> bookingService.bookRoom(null, 2, "1234-5678-9012-3456")
            );

            assertEquals("Tham so dat phong khong hop le!", exception.getMessage());

            // VERIFY: Khong tuong tac voi bat ky mock nao
            verifyNoInteractions(roomRepository, paymentGateway);
        }
    }
}
