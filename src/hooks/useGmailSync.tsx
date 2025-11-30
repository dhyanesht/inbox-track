import { useState, useEffect } from "react";
import { supabase } from "@/integrations/supabase/client";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";

export const useGmailSync = () => {
  const [isSyncing, setIsSyncing] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const queryClient = useQueryClient();

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

    // Listen for OAuth success
    const params = new URLSearchParams(window.location.search);
    if (params.get("gmail_connected") === "true") {
      toast.success("Gmail connected successfully!");
      setIsConnected(true);
      // Clean URL
      window.history.replaceState({}, "", window.location.pathname);
      // Trigger initial sync
      syncEmails();
    }
  }, []);

  const syncEmails = async () => {
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
  };

  return { syncEmails, isSyncing, isConnected };
};
