-- ============================================
-- CREATE NEW TABLE: add_reservation_services
-- Date: April 3, 2026
-- Columns: Same as reservations table
-- ============================================

CREATE TABLE IF NOT EXISTS public.add_reservation_services (
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
ALTER TABLE public.add_reservation_services ENABLE ROW LEVEL SECURITY;

-- Create policies
CREATE POLICY "Anyone can view add_reservation_services" ON public.add_reservation_services
    FOR SELECT USING (true);

CREATE POLICY "Anyone can insert add_reservation_services" ON public.add_reservation_services
    FOR INSERT WITH CHECK (true);

CREATE POLICY "Anyone can update add_reservation_services" ON public.add_reservation_services
    FOR UPDATE USING (true);

CREATE POLICY "Anyone can delete add_reservation_services" ON public.add_reservation_services
    FOR DELETE USING (true);

-- Create indexes
CREATE INDEX IF NOT EXISTS add_reservation_services_status_idx ON public.add_reservation_services(status);
CREATE INDEX IF NOT EXISTS add_reservation_services_service_name_idx ON public.add_reservation_services(service_name);
CREATE INDEX IF NOT EXISTS add_reservation_services_created_at_idx ON public.add_reservation_services(created_at);

