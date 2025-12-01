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
    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const redirectUri = `${supabaseUrl}/functions/v1/gmail-auth-callback`;

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

    // Return HTML that closes popup and notifies parent window
    const html = `
      <!DOCTYPE html>
      <html>
        <head>
          <title>Gmail Connected</title>
          <style>
            body {
              font-family: system-ui, -apple-system, sans-serif;
              display: flex;
              align-items: center;
              justify-content: center;
              height: 100vh;
              margin: 0;
              background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
              color: white;
            }
            .message {
              text-align: center;
              padding: 2rem;
              background: rgba(255, 255, 255, 0.1);
              border-radius: 1rem;
              backdrop-filter: blur(10px);
            }
            h1 { margin: 0 0 1rem 0; }
            p { margin: 0; opacity: 0.9; }
          </style>
        </head>
        <body>
          <div class="message">
            <h1>✓ Gmail Connected!</h1>
            <p>This window will close automatically...</p>
          </div>
          <script>
            // Notify parent window and close
            if (window.opener) {
              window.opener.postMessage({ type: 'gmail_connected', success: true }, '*');
              setTimeout(() => window.close(), 1000);
            } else {
              document.body.innerHTML = '<div class="message"><h1>✓ Connected!</h1><p>You can close this window now.</p></div>';
            }
          </script>
        </body>
      </html>
    `;

    return new Response(html, {
      headers: { ...corsHeaders, 'Content-Type': 'text/html' },
    });
  } catch (error: any) {
    console.error("Error in gmail-auth-callback:", error);
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
