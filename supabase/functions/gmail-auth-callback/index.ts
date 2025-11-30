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
    const url = new URL(req.url);
    const code = url.searchParams.get("code");
    const state = url.searchParams.get("state"); // userId

    if (!code || !state) {
      throw new Error("Missing code or state parameter");
    }

    const clientId = Deno.env.get("GMAIL_CLIENT_ID");
    const clientSecret = Deno.env.get("GMAIL_CLIENT_SECRET");
    const redirectUri = `${url.origin}/functions/v1/gmail-auth-callback`;

    console.log("Exchanging code for tokens...");

    // Exchange code for tokens
    const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        code,
        client_id: clientId!,
        client_secret: clientSecret!,
        redirect_uri: redirectUri,
        grant_type: "authorization_code",
      }),
    });

    if (!tokenResponse.ok) {
      const error = await tokenResponse.text();
      console.error("Token exchange failed:", error);
      throw new Error(`Token exchange failed: ${error}`);
    }

    const tokens = await tokenResponse.json();
    console.log("Tokens received successfully");

    // Store tokens in database
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    );

    const expiresAt = new Date(Date.now() + tokens.expires_in * 1000);

    const { error: dbError } = await supabase.from("gmail_tokens").upsert({
      user_id: state,
      access_token: tokens.access_token,
      refresh_token: tokens.refresh_token,
      expires_at: expiresAt.toISOString(),
    });

    if (dbError) {
      console.error("Database error:", dbError);
      throw dbError;
    }

    console.log("Tokens stored successfully");

    // Redirect back to app
    return new Response(null, {
      status: 302,
      headers: {
        ...corsHeaders,
        Location: `${url.origin}?gmail_connected=true`,
      },
    });
  } catch (error: any) {
    console.error("Error in gmail-auth-callback:", error);
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
