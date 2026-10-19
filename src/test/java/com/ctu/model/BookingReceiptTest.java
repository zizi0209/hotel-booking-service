package com.ctu.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BookingReceipt DTO Unit Test Suite")
class BookingReceiptTest {

    @Test
    @DisplayName("Kiem tra khoi tao va cac getter cua BookingReceipt")
    void testBookingReceiptConstructorAndGetters() {
        BookingReceipt receipt = new BookingReceipt("BK001", "R01", 600000.0);

        assertEquals("BK001", receipt.getBookingId());
        assertEquals("R01", receipt.getRoomId());
        assertEquals(600000.0, receipt.getTotalAmount(), 0.001);
    }
}
