import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      { global: { headers: { Authorization: req.headers.get("Authorization")! } } }
    );

    // Get user
    const { data: { user }, error: userError } = await supabase.auth.getUser();
    if (userError || !user) throw new Error("Not authenticated");

    console.log("Fetching Gmail tokens for user:", user.id);

    // Get Gmail tokens - use service role to bypass RLS since tokens were stored via callback
    const supabaseAdmin = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    );

    const { data: tokenData, error: tokenError } = await supabaseAdmin
      .from("gmail_tokens")
      .select("*")
      .eq("user_id", user.id)
      .single();

    if (tokenError) {
      console.error("Token fetch error:", tokenError);
      throw new Error("Gmail not connected");
    }

    if (!tokenData) {
      throw new Error("Gmail not connected");
    }

    // Refresh token if needed
    let accessToken = tokenData.access_token;
    if (new Date(tokenData.expires_at) < new Date()) {
      console.log("Refreshing expired token...");
      const refreshResponse = await fetch("https://oauth2.googleapis.com/token", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
          refresh_token: tokenData.refresh_token,
          client_id: Deno.env.get("GMAIL_CLIENT_ID")!,
          client_secret: Deno.env.get("GMAIL_CLIENT_SECRET")!,
          grant_type: "refresh_token",
        }),
      });

      if (!refreshResponse.ok) {
        throw new Error("Failed to refresh token");
      }

      const newTokens = await refreshResponse.json();
      accessToken = newTokens.access_token;

      // Update tokens in database
      const supabaseAdmin = createClient(
        Deno.env.get("SUPABASE_URL")!,
        Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
      );

      await supabaseAdmin.from("gmail_tokens").update({
        access_token: newTokens.access_token,
        expires_at: new Date(Date.now() + newTokens.expires_in * 1000).toISOString(),
      }).eq("user_id", user.id);
    }

    console.log("Fetching emails from Gmail...");

    // Fetch recent emails (last 7 days)
    const sevenDaysAgo = Math.floor((Date.now() - 7 * 24 * 60 * 60 * 1000) / 1000);
    const query = `after:${sevenDaysAgo}`;

    const messagesResponse = await fetch(
      `https://gmail.googleapis.com/gmail/v1/users/me/messages?q=${encodeURIComponent(query)}&maxResults=50`,
      { headers: { Authorization: `Bearer ${accessToken}` } }
    );

    if (!messagesResponse.ok) {
      throw new Error("Failed to fetch emails");
    }

    const messagesData = await messagesResponse.json();
    const messages = messagesData.messages || [];

    console.log(`Found ${messages.length} messages`);

    // Get job applications to match against
    const { data: applications } = await supabase
      .from("job_applications")
      .select("*")
      .eq("user_id", user.id);

    const categorizedEmails = [];

    // Process each email
    for (const message of messages.slice(0, 20)) { // Process first 20 for performance
      const emailResponse = await fetch(
        `https://gmail.googleapis.com/gmail/v1/users/me/messages/${message.id}`,
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );

      const emailData = await emailResponse.json();
      const headers = emailData.payload.headers;
      const subject = headers.find((h: any) => h.name === "Subject")?.value || "";
      const from = headers.find((h: any) => h.name === "From")?.value || "";
      const snippet = emailData.snippet;

      // Categorize email using AI
      const aiResponse = await fetch("https://ai.gateway.lovable.dev/v1/chat/completions", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${Deno.env.get("LOVABLE_API_KEY")}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          model: "google/gemini-2.5-flash",
          messages: [
            {
              role: "system",
              content: "You categorize job application emails. Return only ONE of: thank_you, interview, offer, rejection, other"
            },
            {
              role: "user",
              content: `Subject: ${subject}\nFrom: ${from}\nSnippet: ${snippet}\n\nCategorize this email.`
            }
          ],
        }),
      });

      const aiData = await aiResponse.json();
      const category = aiData.choices[0].message.content.trim().toLowerCase();

      console.log(`Email categorized as: ${category}`);

      // Match to application by company name
      const matchedApp = applications?.find((app: any) => 
        from.toLowerCase().includes(app.company_name.toLowerCase()) ||
        subject.toLowerCase().includes(app.company_name.toLowerCase())
      );

      if (matchedApp && category !== "other") {
        categorizedEmails.push({
          application_id: matchedApp.id,
          category,
          subject,
          from,
          snippet,
        });
      }
    }

    return new Response(
      JSON.stringify({ 
        success: true, 
        emailsProcessed: messages.length,
        emailsCategorized: categorizedEmails.length,
        emails: categorizedEmails 
      }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error: any) {
    console.error("Error in sync-emails:", error);
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
