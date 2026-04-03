-- ==========================================
-- ADMIN LOGIN SETUP SQL SCRIPT
-- Date: April 2, 2026
-- Purpose: Add admin role support to users table
-- ==========================================

-- Step 1: Add 'role' column to users table if it doesn't exist
ALTER TABLE public.users
ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'user';

-- Step 2: Create index for role column (for better query performance)
CREATE INDEX IF NOT EXISTS users_role_idx ON public.users(role);

-- Step 3: Insert Admin User Record
-- Make sure the email matches the Supabase Auth user (admin@gmail.com)
INSERT INTO public.users (apartment_number, email, full_name, phone, role)
VALUES ('ADMIN', 'admin@gmail.com', 'System Admin', '555-0000', 'admin')
ON CONFLICT (apartment_number) DO UPDATE
SET role = 'admin', email = 'admin@gmail.com', full_name = 'System Admin';

-- Step 4: Verify the admin record was created
SELECT * FROM public.users WHERE role = 'admin';

-- ==========================================
-- NOTE FOR SUPABASE DASHBOARD:
-- You must also create the Supabase Auth user:
-- Email: admin@gmail.com
-- Password: admin123
--
-- Steps:
-- 1. Go to Supabase Dashboard
-- 2. Select your project
-- 3. Go to: Authentication → Users
-- 4. Click: "Add User"
-- 5. Email: admin@gmail.com
-- 6. Password: admin123
-- 7. Confirm Password: admin123
-- 8. Click: Create User
-- ==========================================

