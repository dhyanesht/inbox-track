import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  try {
    const { content } = await req.json();
    
    console.log("[Parse Job] Parsing content...");
    
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
            content: `You extract job application information from text. Extract:
- company_name (required)
- position (required)
- location (optional, extract city/state/country or "Remote" or "United States (Remote)")
- application_date (optional, in ISO format YYYY-MM-DD, default to today if not found)
- email_type (optional: "thank_you", "interview", "offer", "rejection", "other" - only if this is an update email about existing application)

Return ONLY valid JSON with these fields. If company_name or position cannot be determined, return {"error": "insufficient_info"}.`
          },
          {
            role: "user",
            content: `Extract job application info from:\n\n${content}`
          }
        ],
      }),
    });

    const aiData = await aiResponse.json();
    const responseText = aiData.choices?.[0]?.message?.content?.trim();
    
    console.log("[Parse Job] AI response:", responseText);

    // Parse the JSON response
    let parsed;
    try {
      parsed = JSON.parse(responseText);
    } catch (e) {
      console.error("[Parse Job] Failed to parse AI response:", e);
      return new Response(
        JSON.stringify({ error: "Failed to parse AI response" }),
        { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (parsed.error) {
      return new Response(
        JSON.stringify({ error: parsed.error }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    console.log("[Parse Job] Parsed data:", parsed);
    
    return new Response(
      JSON.stringify(parsed),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error: any) {
    console.error("[Parse Job] Error:", error);
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
