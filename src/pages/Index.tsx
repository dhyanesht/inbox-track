import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { supabase } from "@/integrations/supabase/client";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Plus, Mail } from "lucide-react";
import { DashboardStats } from "@/components/dashboard/DashboardStats";
import { ApplicationsList } from "@/components/applications/ApplicationsList";
import { ApplicationDialog } from "@/components/applications/ApplicationDialog";
import { toast } from "sonner";

const Index = () => {
  const navigate = useNavigate();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [user, setUser] = useState<any>(null);

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
            <Button variant="outline" size="lg">
              <Mail className="mr-2 h-5 w-5" />
              Connect Gmail
            </Button>
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
      </div>
    </div>
  );
};

export default Index;