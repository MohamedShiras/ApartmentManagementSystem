-- Create reservations table
CREATE TABLE IF NOT EXISTS public.reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name TEXT NOT NULL,
    description TEXT,
    reservation_date TEXT NOT NULL,
    reservation_time TEXT NOT NULL,
    duration TEXT,
    image_url TEXT,
    status TEXT NOT NULL DEFAULT 'pending',
    booked_by TEXT NOT NULL,
    created_at BIGINT DEFAULT (EXTRACT(epoch FROM now()) * 1000)::bigint,
    updated_at TIMESTAMP DEFAULT now()
);

-- Enable Row Level Security
ALTER TABLE public.reservations ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Anyone can view reservations" ON public.reservations;
DROP POLICY IF EXISTS "Anyone can insert reservations" ON public.reservations;
DROP POLICY IF EXISTS "Anyone can update reservations" ON public.reservations;
DROP POLICY IF EXISTS "Anyone can delete reservations" ON public.reservations;

-- Create policies - Allow all operations for now
CREATE POLICY "Anyone can view reservations" ON public.reservations
    FOR SELECT USING (true);

CREATE POLICY "Anyone can insert reservations" ON public.reservations
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Anyone can update reservations" ON public.reservations
    FOR UPDATE USING (true);

CREATE POLICY "Anyone can delete reservations" ON public.reservations
    FOR DELETE USING (true);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS reservations_status_idx ON public.reservations(status);
CREATE INDEX IF NOT EXISTS reservations_service_name_idx ON public.reservations(service_name);
CREATE INDEX IF NOT EXISTS reservations_created_at_idx ON public.reservations(created_at);

