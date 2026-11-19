package com.eventhall.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventhall.dao.BookingDao;
import com.eventhall.dao.CustomerDao;
import com.eventhall.dao.PaymentDao;
import com.eventhall.dao.VenueDao;
import com.eventhall.exception.HallAlreadyBookedException;
import com.eventhall.model.Customer;
import com.eventhall.model.MarriageHall;
import com.eventhall.model.Venue;
import com.eventhall.model.VenueStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Optional;
import org.mockito.MockMakers;
import org.junit.jupiter.api.Test;

class BookingServiceTest {
    @Test
    void rejectsUnavailableDateBeforeInsertingBooking() throws Exception {
        BookingDao bookingDao = mock(BookingDao.class);
        CustomerDao customerDao = mock(CustomerDao.class);
        VenueDao venueDao = mock(VenueDao.class);
        PaymentDao paymentDao = mock(PaymentDao.class);
        Connection connection = mock(Connection.class, withSettings().mockMaker(MockMakers.PROXY));
        Customer customer = new Customer(1, "Test Customer", "555-0199", null, null);
        Venue venue = new MarriageHall(2, "Test Hall", 300, new BigDecimal("1000.00"), "", VenueStatus.ACTIVE);
        when(customerDao.findById(1)).thenReturn(Optional.of(customer));
        when(venueDao.findById(2)).thenReturn(Optional.of(venue));
        when(connection.getAutoCommit()).thenReturn(true);
        when(bookingDao.isAvailable(connection, 2, LocalDate.now().plusDays(2), "EVENING")).thenReturn(false);

        BookingService service = new BookingService(bookingDao, customerDao, venueDao, paymentDao, () -> connection);

        HallAlreadyBookedException exception = assertThrows(HallAlreadyBookedException.class, () -> service.createBooking(
                1, 2, LocalDate.now().plusDays(2), "EVENING", "Reception", 100,
                new BigDecimal("200.00")));
        org.junit.jupiter.api.Assertions.assertNotNull(exception.getMessage());
        verify(bookingDao, never()).create(any(), any());
        verify(connection).rollback();
    }
}
