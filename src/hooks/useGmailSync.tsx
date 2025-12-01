import { useState, useEffect, useCallback } from "react";
import { supabase } from "@/integrations/supabase/client";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";

export const useGmailSync = () => {
  const [isSyncing, setIsSyncing] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const queryClient = useQueryClient();

  const syncEmails = useCallback(async () => {
    try {
      setIsSyncing(true);
      const { data, error } = await supabase.functions.invoke("sync-emails");
      
      if (error) throw error;

      toast.success(`Synced ${data.emailsCategorized} emails successfully!`);
      
      // Refresh applications to show updated data
      queryClient.invalidateQueries({ queryKey: ["applications"] });
      
      return data;
    } catch (error: any) {
      toast.error(error.message || "Failed to sync emails");
      throw error;
    } finally {
      setIsSyncing(false);
    }
  }, [queryClient]);

  useEffect(() => {
    // Check if Gmail is connected
    const checkConnection = async () => {
      const { data: { user } } = await supabase.auth.getUser();
      if (!user) return;

      const { data } = await supabase
        .from("gmail_tokens")
        .select("id")
        .eq("user_id", user.id)
        .single();

      setIsConnected(!!data);
    };

    checkConnection();

    // Listen for postMessage from OAuth popup
    const handleMessage = (event: MessageEvent) => {
      console.log('[Gmail Sync] Received message:', event.data);
      
      if (event.data.type === 'gmail_connected' && event.data.success) {
        console.log('[Gmail Sync] Gmail connected message received, triggering sync');
        toast.success("Gmail connected successfully!");
        setIsConnected(true);
        // Trigger initial sync
        syncEmails();
      }
    };

    console.log('[Gmail Sync] Setting up message listener');
    window.addEventListener('message', handleMessage);

    return () => {
      console.log('[Gmail Sync] Removing message listener');
      window.removeEventListener('message', handleMessage);
    };
  }, [syncEmails]);

  return { syncEmails, isSyncing, isConnected };
};
