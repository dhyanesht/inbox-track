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
    const { subject, from, body } = await req.json();
    const aiResponse = await fetch("https://ai.gateway.lovable.dev/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${Deno.env.get("LOVABLE_API_KEY")}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: "google/gemini-2.5-flash",
        messages: [
          { role: "system", content: "You categorize job application emails. Return only ONE of: thank_you, interview, offer, rejection, other" },
          { role: "user", content: `Subject: ${subject}\nFrom: ${from}\nBody: ${body}\n\nCategorize this email.` }
        ],
      }),
    });
    const aiData = await aiResponse.json();
    const category = aiData.choices?.[0]?.message?.content?.trim().toLowerCase();
    return new Response(JSON.stringify({ category }), { headers: corsHeaders });
  } catch (error: any) {
    return new Response(JSON.stringify({ error: error.message }), { status: 500, headers: corsHeaders });
  }
});

