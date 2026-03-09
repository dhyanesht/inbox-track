-- Create job applications table
CREATE TABLE public.job_applications (
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
CREATE TABLE public.application_events (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  application_id UUID NOT NULL REFERENCES public.job_applications(id) ON DELETE CASCADE,
  event_type TEXT NOT NULL CHECK (event_type IN ('applied', 'email_received', 'interview_scheduled', 'follow_up', 'status_change', 'note_added')),
  title TEXT NOT NULL,
  description TEXT,
  event_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Enable Row Level Security
ALTER TABLE public.job_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.application_events ENABLE ROW LEVEL SECURITY;

-- Create policies for job_applications
CREATE POLICY "Users can view their own applications"
ON public.job_applications
FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can create their own applications"
ON public.job_applications
FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own applications"
ON public.job_applications
FOR UPDATE
USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own applications"
ON public.job_applications
FOR DELETE
USING (auth.uid() = user_id);

-- Create policies for application_events
CREATE POLICY "Users can view events for their applications"
ON public.application_events
FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM public.job_applications
    WHERE job_applications.id = application_events.application_id
    AND job_applications.user_id = auth.uid()
  )
);

CREATE POLICY "Users can create events for their applications"
ON public.application_events
FOR INSERT
WITH CHECK (
  EXISTS (
    SELECT 1 FROM public.job_applications
    WHERE job_applications.id = application_events.application_id
    AND job_applications.user_id = auth.uid()
  )
);

-- Create function to update last_updated timestamp
CREATE OR REPLACE FUNCTION public.update_last_updated_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.last_updated = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SET search_path = public;

-- Create trigger for automatic timestamp updates
CREATE TRIGGER update_job_applications_last_updated
BEFORE UPDATE ON public.job_applications
FOR EACH ROW
EXECUTE FUNCTION public.update_last_updated_column();

-- Create indexes for better performance
CREATE INDEX idx_job_applications_user_id ON public.job_applications(user_id);
CREATE INDEX idx_job_applications_status ON public.job_applications(status);
CREATE INDEX idx_application_events_application_id ON public.application_events(application_id);
CREATE INDEX idx_application_events_event_date ON public.application_events(event_date);