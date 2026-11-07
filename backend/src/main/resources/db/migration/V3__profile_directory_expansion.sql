-- =============================================================================
-- Wizards of Teligati: Profiles and Directory Expansion Schema
-- Version: V3__profile_directory_expansion.sql
-- Description: Extends profiles table with portfolio, alumni, and faculty
--              directory attributes and indexes for performant directory querying.
-- =============================================================================

ALTER TABLE profiles ADD COLUMN IF NOT EXISTS bio TEXT;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS skills TEXT;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS projects TEXT;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS github_url VARCHAR(255);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS linkedin_url VARCHAR(255);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS cgpa DOUBLE PRECISION;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS specialization VARCHAR(255);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS clubs VARCHAR(255);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS designation VARCHAR(150);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS research_focus VARCHAR(255);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS lab_name VARCHAR(150);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS publications_count INTEGER DEFAULT 0;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS is_actively_hiring BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS contact_email VARCHAR(100);

-- Indexes for performant directory search & filter operations
CREATE INDEX IF NOT EXISTS idx_profiles_name ON profiles (name);
CREATE INDEX IF NOT EXISTS idx_profiles_department ON profiles (department);
CREATE INDEX IF NOT EXISTS idx_profiles_batch ON profiles (batch);
CREATE INDEX IF NOT EXISTS idx_profiles_company ON profiles (current_company);
CREATE INDEX IF NOT EXISTS idx_profiles_country ON profiles (country);
CREATE INDEX IF NOT EXISTS idx_profiles_designation ON profiles (designation);
