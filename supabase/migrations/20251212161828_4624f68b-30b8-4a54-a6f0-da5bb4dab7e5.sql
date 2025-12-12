-- Add unique constraint on user_id to prevent duplicate tokens
ALTER TABLE public.gmail_tokens 
ADD CONSTRAINT gmail_tokens_user_id_unique UNIQUE (user_id);