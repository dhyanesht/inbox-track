import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { supabase } from "@/integrations/supabase/client";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Plus, Mail, RefreshCw } from "lucide-react";
import { DashboardStats } from "@/components/dashboard/DashboardStats";
import { ApplicationsList } from "@/components/applications/ApplicationsList";
import { ApplicationDialog } from "@/components/applications/ApplicationDialog";
import { toast } from "sonner";
import { useGmailSync } from "@/hooks/useGmailSync";
import { Textarea } from "@/components/ui/textarea";

const Index = () => {
  const navigate = useNavigate();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [user, setUser] = useState<any>(null);
  const [gmailPopup, setGmailPopup] = useState<Window | null>(null);
  const [emailText, setEmailText] = useState("");
  const [emailLoading, setEmailLoading] = useState(false);
  const { syncEmails, isSyncing, isConnected } = useGmailSync(gmailPopup, setGmailPopup);

  // Check auth status
  const { data: session, isLoading: sessionLoading } = useQuery({
    queryKey: ["session"],
    queryFn: async () => {
      const { data: { session } } = await supabase.auth.getSession();
      setUser(session?.user ?? null);
      return session;
    },
  });

  // Fetch applications
  const { data: applications, isLoading, refetch } = useQuery({
    queryKey: ["applications", user?.id],
    queryFn: async () => {
      if (!user) return [];
      const { data, error } = await supabase
        .from("job_applications")
        .select("*")
        .order("application_date", { ascending: false });

      if (error) throw error;
      return data;
    },
    enabled: !!user,
  });

  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setEmailLoading(true);
    try {
      const { data: { user } } = await supabase.auth.getUser();
      if (!user) throw new Error("Not authenticated");

      console.log("[Job Paste] Parsing pasted content...");

      // Call parse-job-content edge function to extract structured data
      const { data: parsedData, error: parseError } = await supabase.functions.invoke(
        "parse-job-content",
        { body: { content: emailText } }
      );

      if (parseError) {
        console.error("[Job Paste] Parse error:", parseError);
        throw parseError;
      }

      if (parsedData?.error) {
        toast.error("Could not extract job information. Please check the format and try again.");
        return;
      }

      console.log("[Job Paste] Parsed data:", parsedData);

      const { company_name, position, location, application_date, email_type } = parsedData;

      if (!company_name || !position) {
        toast.error("Could not find company name or position in the pasted content.");
        return;
      }

      // Check if this is an update email (has email_type) or a new application
      const matchedApp = (applications || []).find((app: any) =>
        app.company_name.toLowerCase() === company_name.toLowerCase()
      );

      if (matchedApp && email_type && email_type !== "other") {
        // Update existing application with new status
        const { error: updateError } = await supabase.from("job_applications").update({
          status: email_type,
          last_updated: new Date().toISOString(),
          notes: matchedApp.notes 
            ? `${matchedApp.notes}\n\nUpdated via pasted email on ${new Date().toLocaleDateString()}`
            : `Updated via pasted email on ${new Date().toLocaleDateString()}`,
        }).eq("id", matchedApp.id);

        if (updateError) throw updateError;

        toast.success(`Application updated: ${company_name} → ${email_type}`);
      } else if (matchedApp) {
        // Application exists but no status update
        toast.info(`Application for ${company_name} already exists. No changes made.`);
      } else {
        // Create new application
        const { error: insertError } = await supabase.from("job_applications").insert({
          user_id: user.id,
          company_name,
          position,
          location: location || null,
          application_date: application_date || new Date().toISOString(),
          status: "applied",
          notes: `Created via pasted content on ${new Date().toLocaleDateString()}`,
        });

        if (insertError) throw insertError;

        toast.success(`New application created: ${company_name} - ${position}`);
      }

      refetch();
      setEmailText("");
    } catch (error: any) {
      console.error("[Job Paste] Error:", error);
      toast.error(error.message || "Failed to process content");
    } finally {
      setEmailLoading(false);
    }
  };

  if (sessionLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="text-center">
          <p className="text-lg text-foreground">Loading...</p>
        </div>
      </div>
    );
  }

  if (!session) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="text-center space-y-4 p-8">
          <h1 className="text-4xl font-bold text-foreground">Welcome to JobTrackr</h1>
          <p className="text-xl text-muted-foreground">
            Your AI-powered job application tracker
          </p>
          <Button onClick={() => navigate("/auth")} size="lg" className="mt-4">
            Get Started
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto p-6 space-y-8">
        {/* Header */}
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-4xl font-bold text-foreground">JobTrackr</h1>
            <p className="text-muted-foreground mt-1">Track your job applications effortlessly</p>
          </div>
          <div className="flex gap-3">
            {isConnected ? (
              <Button 
                variant="outline" 
                size="lg"
                onClick={syncEmails}
                disabled={isSyncing}
              >
                <RefreshCw className={`mr-2 h-5 w-5 ${isSyncing ? "animate-spin" : ""}`} />
                {isSyncing ? "Syncing..." : "Sync Emails"}
              </Button>
            ) : (
              <Button 
                variant="outline" 
                size="lg"
                onClick={async () => {
                  try {
                    const { data, error } = await supabase.functions.invoke("gmail-auth-init");
                    if (error) throw error;
                    const popup = window.open(data.authUrl, "_blank", "width=600,height=700");
                    setGmailPopup(popup);
                    toast.success("Opening Gmail authorization...");
                  } catch (error: any) {
                    toast.error(error.message || "Failed to connect Gmail");
                  }
                }}
              >
                <Mail className="mr-2 h-5 w-5" />
                Connect Gmail
              </Button>
            )}
            <Button onClick={() => setIsDialogOpen(true)} size="lg">
              <Plus className="mr-2 h-5 w-5" />
              Add Application
            </Button>
          </div>
        </div>

        {/* Stats Dashboard */}
        <DashboardStats applications={applications || []} />

        {/* Applications List */}
        <ApplicationsList
          applications={applications || []}
          isLoading={isLoading}
          onRefetch={refetch}
        />

        {/* Add Application Dialog */}
        <ApplicationDialog
          open={isDialogOpen}
          onOpenChange={setIsDialogOpen}
          onSuccess={() => {
            refetch();
            toast.success("Application added successfully!");
          }}
        />

        {/* Paste Job Content Section */}
        <div className="my-8 p-6 border rounded-xl bg-card">
          <h2 className="text-xl font-bold mb-2">Quick Add from Paste</h2>
          <p className="text-sm text-muted-foreground mb-4">
            Paste job board notifications, application confirmations, or status update emails to automatically create or update applications.
          </p>
          <form onSubmit={handleEmailSubmit} className="space-y-4">
            <Textarea
              value={emailText}
              onChange={e => setEmailText(e.target.value)}
              placeholder="Paste job posting, application confirmation, interview invite, or any job-related content here..."
              rows={6}
              required
            />
            <Button type="submit" disabled={emailLoading || !emailText}>
              {emailLoading ? "Processing..." : "Parse & Add Application"}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Index;

