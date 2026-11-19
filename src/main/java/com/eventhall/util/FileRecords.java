package com.eventhall.util;

import com.eventhall.model.Booking;
import com.eventhall.model.Payment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

public final class FileRecords {
    private FileRecords() { }

    public static Path writeReceipt(Booking booking, Payment payment) throws IOException {
        Path directory = Path.of("receipts");
        Files.createDirectories(directory);
        Path receipt = directory.resolve("receipt-booking-" + booking.getId() + "-payment-" + payment.getId() + ".txt");
        String text = "EVENT HALL PAYMENT RECEIPT" + System.lineSeparator()
                + "Receipt time: " + LocalDateTime.now() + System.lineSeparator()
                + "Booking: " + booking.getId() + System.lineSeparator()
                + "Customer: " + booking.getCustomer().getName() + System.lineSeparator()
                + "Venue: " + booking.getVenue().getName() + System.lineSeparator()
                + "Event date: " + booking.getEventDate() + " (" + booking.getSlot() + ")" + System.lineSeparator()
                + "Payment type: " + payment.getType() + System.lineSeparator()
                + "Payment amount: " + payment.getAmount() + System.lineSeparator()
                + "Remaining balance: " + booking.getBalance() + System.lineSeparator();
        Files.writeString(receipt, text, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return receipt;
    }

    public static Path exportBookings(List<Booking> bookings) throws IOException {
        Path directory = Path.of("exports");
        Files.createDirectories(directory);
        Path file = directory.resolve("bookings-" + LocalDateTime.now().toString().replace(':', '-') + ".csv");
        StringBuilder csv = new StringBuilder("Booking ID,Customer,Phone,Venue,Date,Slot,Event,Guests,Total,Paid,Balance,Status\n");
        for (Booking booking : bookings) {
            csv.append(booking.getId()).append(',')
                    .append(escape(booking.getCustomer().getName())).append(',')
                    .append(escape(booking.getCustomer().getPhone())).append(',')
                    .append(escape(booking.getVenue().getName())).append(',')
                    .append(booking.getEventDate()).append(',').append(escape(booking.getSlot())).append(',')
                    .append(escape(booking.getEventType())).append(',').append(booking.getGuests()).append(',')
                    .append(booking.getTotalAmount()).append(',').append(booking.getAmountPaid()).append(',')
                    .append(booking.getBalance()).append(',').append(booking.getStatus()).append('\n');
        }
        Files.writeString(file, csv, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return file;
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value.replace("\r", " ").replace("\n", " ");
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}