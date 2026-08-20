-- V2: Add OAuth support to users table

-- Add provider column (LOCAL, GOOGLE)
ALTER TABLE users ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

-- Add provider_id for OAuth providers (Google user ID)
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255);

-- Make password nullable for OAuth users who don't have a password
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
