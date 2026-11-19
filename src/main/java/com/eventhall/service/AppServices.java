package com.eventhall.service;

import com.eventhall.dao.JdbcAdminDao;
import com.eventhall.dao.JdbcBookingDao;
import com.eventhall.dao.JdbcCustomerDao;
import com.eventhall.dao.JdbcPaymentDao;
import com.eventhall.dao.JdbcVenueDao;

public final class AppServices {
    private static final JdbcAdminDao ADMINS = new JdbcAdminDao();
    private static final JdbcBookingDao BOOKINGS = new JdbcBookingDao();
    private static final JdbcCustomerDao CUSTOMERS = new JdbcCustomerDao();
    private static final JdbcPaymentDao PAYMENTS = new JdbcPaymentDao();
    private static final JdbcVenueDao VENUES = new JdbcVenueDao();

    private static final AuthenticationService AUTHENTICATION = new AuthenticationService(ADMINS);
    private static final UserAuthenticationService USER_AUTHENTICATION = new UserAuthenticationService(CUSTOMERS);
    private static final BookingService BOOKING = new BookingService(BOOKINGS, CUSTOMERS, VENUES, PAYMENTS);
    private static final CustomerService CUSTOMER = new CustomerService(CUSTOMERS);
    private static final PaymentService PAYMENT = new PaymentService(BOOKINGS, PAYMENTS);
    private static final VenueService VENUE = new VenueService(VENUES);
    private static final ReportService REPORT = new ReportService(BOOKINGS, PAYMENTS);

    private AppServices() { }
    public static AuthenticationService authentication() { return AUTHENTICATION; }
    public static UserAuthenticationService userAuthentication() { return USER_AUTHENTICATION; }
    public static BookingService booking() { return BOOKING; }
    public static CustomerService customer() { return CUSTOMER; }
    public static PaymentService payment() { return PAYMENT; }
    public static VenueService venue() { return VENUE; }
    public static ReportService report() { return REPORT; }
}