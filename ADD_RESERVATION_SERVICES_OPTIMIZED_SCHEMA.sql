-- ============================================
-- ADD RESERVATION SERVICES TABLE - OPTIMIZED
-- Date: April 3, 2026
-- Purpose: Store admin service offerings for amenity management
-- Columns: Only service_name, description, time_period, max_guests
-- ============================================

-- ============================================
-- IMPORTANT: Before running this script
-- ============================================
-- If the table already exists with old columns, you have two options:
--
-- Option 1: BACKUP & DROP (Recommended if no production data)
-- CREATE TABLE IF NOT EXISTS public.add_reservation_services_backup AS
-- SELECT * FROM public.add_reservation_services;
-- DROP TABLE IF EXISTS public.add_reservation_services CASCADE;
--
-- Option 2: ALTER & MIGRATE (If you have important data to keep)
-- See instructions below
--
-- ============================================

-- Create new table with optimized columns
CREATE TABLE IF NOT EXISTS public.add_reservation_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name TEXT NOT NULL,
    description TEXT NOT NULL,
    time_period TEXT NOT NULL,
    max_guests TEXT NOT NULL,
    created_at BIGINT DEFAULT (EXTRACT(epoch FROM now()) * 1000)::bigint,
    updated_at TIMESTAMP DEFAULT now()
);

-- Enable Row Level Security
ALTER TABLE public.add_reservation_services ENABLE ROW LEVEL SECURITY;

-- ============================================
-- ROW LEVEL SECURITY POLICIES
-- ============================================

-- Anyone can view services
DROP POLICY IF EXISTS "Anyone can view add_reservation_services" ON public.add_reservation_services;
CREATE POLICY "Anyone can view add_reservation_services" ON public.add_reservation_services
    FOR SELECT USING (true);

-- Anyone can insert services (Admin can add)
DROP POLICY IF EXISTS "Anyone can insert add_reservation_services" ON public.add_reservation_services;
CREATE POLICY "Anyone can insert add_reservation_services" ON public.add_reservation_services
    FOR INSERT WITH CHECK (true);

-- Anyone can update services (Admin can edit)
DROP POLICY IF EXISTS "Anyone can update add_reservation_services" ON public.add_reservation_services;
CREATE POLICY "Anyone can update add_reservation_services" ON public.add_reservation_services
    FOR UPDATE USING (true);

-- Anyone can delete services (Admin can delete)
DROP POLICY IF EXISTS "Anyone can delete add_reservation_services" ON public.add_reservation_services;
CREATE POLICY "Anyone can delete add_reservation_services" ON public.add_reservation_services
    FOR DELETE USING (true);

-- ============================================
-- INDEXES FOR PERFORMANCE
-- ============================================

CREATE INDEX IF NOT EXISTS add_reservation_services_service_name_idx
    ON public.add_reservation_services(service_name);

CREATE INDEX IF NOT EXISTS add_reservation_services_created_at_idx
    ON public.add_reservation_services(created_at);

-- ============================================
-- SAMPLE DATA
-- ============================================
-- Insert the three main amenities

INSERT INTO public.add_reservation_services
(service_name, description, time_period, max_guests)
VALUES
    (
        'Swimming Pool',
        'Enjoy a refreshing swim in our outdoor infinity pool with stunning views. Open daily from 6AM to 10PM.',
        '6AM – 10PM',
        'Max 20 guests'
    ),
    (
        'Fitness Center',
        'Fully equipped gym with cardio machines, free weights, and strength training equipment. Professionally maintained.',
        '5AM – 11PM',
        'Max 15 guests'
    ),
    (
        'Restaurant & Dining',
        'Reserve a table at our rooftop restaurant. Enjoy fine dining with panoramic views. A La Carte menu available.',
        '7AM – 11PM',
        'A La Carte'
    )
ON CONFLICT DO NOTHING;

-- ============================================
-- VERIFICATION QUERY
-- ============================================
-- Run this to verify the table was created correctly:
-- SELECT * FROM public.add_reservation_services;
-- SELECT COUNT(*) as total_services FROM public.add_reservation_services;

-- ============================================
-- NOTES
-- ============================================
-- 1. OLD COLUMNS REMOVED:
--    - reservation_date
--    - reservation_time
--    - duration
--    - image_url
--    - status
--    - booked_by
--    - capacity
--
-- 2. NEW COLUMNS:
--    - time_period: Operating hours (e.g., "6AM – 10PM")
--    - max_guests: Maximum capacity (e.g., "Max 20 guests")
--
-- 3. USER BOOKINGS:
--    User bookings are still stored in the "bookings" table
--    This table only manages admin service offerings
--
-- 4. RLS POLICIES:
--    All operations (SELECT, INSERT, UPDATE, DELETE) are allowed
--    Modify policies in Supabase if you need stricter access control
-- ============================================

