-- Create bookings table for storing user amenity bookings
CREATE TABLE IF NOT EXISTS public.bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name TEXT NOT NULL,
    booking_date TEXT NOT NULL,
    booking_time TEXT NOT NULL,
    number_of_guests TEXT,
    special_request TEXT,
    booked_by TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at BIGINT DEFAULT (EXTRACT(epoch FROM now()) * 1000)::bigint,
    updated_at TIMESTAMP DEFAULT now()
);

-- Enable Row Level Security
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Anyone can view bookings" ON public.bookings;
DROP POLICY IF EXISTS "Anyone can insert bookings" ON public.bookings;
DROP POLICY IF EXISTS "Anyone can update bookings" ON public.bookings;
DROP POLICY IF EXISTS "Anyone can delete bookings" ON public.bookings;

-- Create policies - Allow all operations for now
CREATE POLICY "Anyone can view bookings" ON public.bookings
    FOR SELECT USING (true);

CREATE POLICY "Anyone can insert bookings" ON public.bookings
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Anyone can update bookings" ON public.bookings
    FOR UPDATE USING (true);

CREATE POLICY "Anyone can delete bookings" ON public.bookings
    FOR DELETE USING (true);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS bookings_status_idx ON public.bookings(status);
CREATE INDEX IF NOT EXISTS bookings_service_name_idx ON public.bookings(service_name);
CREATE INDEX IF NOT EXISTS bookings_created_at_idx ON public.bookings(created_at);
CREATE INDEX IF NOT EXISTS bookings_booked_by_idx ON public.bookings(booked_by);

-- Grant permissions
GRANT ALL ON public.bookings TO authenticated;
GRANT ALL ON public.bookings TO anon;

