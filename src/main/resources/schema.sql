PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS admins (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS customers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT,
    address TEXT,
    password_hash TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);
CREATE INDEX IF NOT EXISTS idx_customers_name ON customers(name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_email_unique ON customers(email) WHERE email IS NOT NULL;

CREATE TABLE IF NOT EXISTS venues (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('MARRIAGE_HALL', 'COMMUNITY_CENTER', 'OUTDOOR_LAWN', 'INDOOR_CONVENTION_HALL')),
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    rent NUMERIC NOT NULL CHECK (rent >= 0),
    facilities TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    description TEXT DEFAULT '',
    image_url TEXT
);

CREATE TABLE IF NOT EXISTS bookings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL REFERENCES customers(id),
    venue_id INTEGER NOT NULL REFERENCES venues(id),
    event_date TEXT NOT NULL,
    slot TEXT NOT NULL,
    event_type TEXT NOT NULL,
    guests INTEGER NOT NULL CHECK (guests > 0),
    total_amount NUMERIC NOT NULL CHECK (total_amount >= 0),
    status TEXT NOT NULL CHECK (status IN ('CONFIRMED', 'FULLY_PAID', 'CANCELLED', 'COMPLETED')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_booking_venue_date_slot UNIQUE (venue_id, event_date, slot)
);
CREATE INDEX IF NOT EXISTS idx_bookings_event_date ON bookings(event_date);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);

CREATE TABLE IF NOT EXISTS payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    booking_id INTEGER NOT NULL REFERENCES bookings(id),
    amount NUMERIC NOT NULL CHECK (amount > 0),
    payment_type TEXT NOT NULL CHECK (payment_type IN ('ADVANCE', 'INSTALLMENT')),
    payment_date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_payments_booking ON payments(booking_id);

INSERT OR IGNORE INTO admins (username, password_hash)
VALUES ('admin', '$2a$10$r.s9nSZtnz/egjDMEJ.tDuZwACKpficetwYzb/IZFt46zqRZuyHQq');

INSERT INTO customers (name, phone, email, address)
SELECT 'Aarav Sharma', '555-0100', 'aarav@example.com', '12 Garden Road'
WHERE NOT EXISTS (SELECT 1 FROM customers WHERE phone = '555-0100');

INSERT INTO venues (name, type, capacity, rent, facilities, status, description, image_url)
SELECT 'Rosewood Marriage Hall', 'MARRIAGE_HALL', 500, 2500.00,
       'Stage, catering kitchen, air conditioning, parking', 'ACTIVE',
       'An elegant wedding venue for milestone celebrations with a grand stage and guest-ready amenities.',
       'https://images.unsplash.com/photo-1520854221256-17451cc331bf?auto=format&fit=crop&w=1200&q=80'
WHERE NOT EXISTS (SELECT 1 FROM venues WHERE name = 'Rosewood Marriage Hall');

INSERT INTO venues (name, type, capacity, rent, facilities, status, description, image_url)
SELECT 'Community Pavilion', 'COMMUNITY_CENTER', 180, 800.00,
       'Meeting rooms, projector, accessible entrance', 'ACTIVE',
       'A flexible community venue for gatherings, events, and local celebrations with practical amenities.',
       'https://images.unsplash.com/photo-1511795409834-ef04bbd61622?auto=format&fit=crop&w=1200&q=80'
WHERE NOT EXISTS (SELECT 1 FROM venues WHERE name = 'Community Pavilion');

INSERT INTO venues (name, type, capacity, rent, facilities, status, description, image_url)
SELECT 'Green Valley Garden', 'OUTDOOR_LAWN', 300, 1800.00,
       'Garden lawn, floral arch, outdoor dining, parking, sunset lighting', 'ACTIVE',
       'Perfect for weddings, receptions, and birthday parties with a lush outdoor setting and celebratory atmosphere.',
       'https://images.unsplash.com/photo-1469371670807-013ccf25f16a?auto=format&fit=crop&w=1200&q=80'
WHERE NOT EXISTS (SELECT 1 FROM venues WHERE name = 'Green Valley Garden');

INSERT INTO venues (name, type, capacity, rent, facilities, status, description, image_url)
SELECT 'Grand Royal Convention Centre', 'INDOOR_CONVENTION_HALL', 450, 3400.00,
       'Grand stage, audio-visual system, banquet seating, parking, premium lighting', 'ACTIVE',
       'A premium indoor venue for engagements, weddings, and corporate events with elegant presentation spaces.',
       'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=1200&q=80'
WHERE NOT EXISTS (SELECT 1 FROM venues WHERE name = 'Grand Royal Convention Centre');