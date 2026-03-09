-- Create job applications table
CREATE TABLE job_track.job_applications (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID NOT NULL,
  company_name TEXT NOT NULL,
  position TEXT NOT NULL,
  job_url TEXT,
  status TEXT NOT NULL DEFAULT 'applied' CHECK (status IN ('applied', 'screening', 'interview', 'offer', 'rejected', 'withdrawn')),
  location TEXT,
  salary_range TEXT,
  application_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  notes TEXT,
  email_thread_id TEXT,
  priority TEXT DEFAULT 'medium' CHECK (priority IN ('low', 'medium', 'high')),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Create application events table for timeline
CREATE TABLE job_track.application_events (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  application_id UUID NOT NULL REFERENCES job_track.job_applications(id) ON DELETE CASCADE,
  event_type TEXT NOT NULL CHECK (event_type IN ('applied', 'email_received', 'interview_scheduled', 'follow_up', 'status_change', 'note_added')),
  title TEXT NOT NULL,
  description TEXT,
  event_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);