-- Create emails table
CREATE TABLE job_track.emails (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  message_id TEXT,
  subject TEXT,
  body TEXT,
  classificaiton TEXT, -- kept spelling as in the entity
  application_id UUID UNIQUE
    REFERENCES job_track.job_applications(id)
    ON DELETE SET NULL
);
