import { useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Card, CardContent } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Building2,
  MapPin,
  DollarSign,
  ExternalLink,
  Calendar,
  Edit,
  Trash2,
} from "lucide-react";
import { format } from "date-fns";
import { supabase } from "@/integrations/supabase/client";
import { toast } from "sonner";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";

interface ApplicationDetailDialogProps {
  application: any;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onRefetch: () => void;
}

export const ApplicationDetailDialog = ({
  application,
  open,
  onOpenChange,
  onRefetch,
}: ApplicationDetailDialogProps) => {
  const [isEditing, setIsEditing] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [status, setStatus] = useState(application.status);
  const [notes, setNotes] = useState(application.notes || "");
  const [loading, setLoading] = useState(false);

  const statusColors: Record<string, string> = {
    applied: "bg-accent text-accent-foreground",
    screening: "bg-secondary text-secondary-foreground",
    interview: "bg-primary text-primary-foreground",
    offer: "bg-primary text-primary-foreground",
    rejected: "bg-destructive text-destructive-foreground",
    withdrawn: "bg-muted text-muted-foreground",
  };

  const handleUpdate = async () => {
    setLoading(true);
    try {
      const { error } = await supabase
        .from("job_applications")
        .update({ status, notes })
        .eq("id", application.id);

      if (error) throw error;

      toast.success("Application updated successfully");
      setIsEditing(false);
      onRefetch();
    } catch (error: any) {
      toast.error(error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    setLoading(true);
    try {
      const { error } = await supabase
        .from("job_applications")
        .delete()
        .eq("id", application.id);

      if (error) throw error;

      toast.success("Application deleted successfully");
      onOpenChange(false);
      onRefetch();
    } catch (error: any) {
      toast.error(error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <div className="flex justify-between items-start">
              <div>
                <DialogTitle className="text-2xl">{application.position}</DialogTitle>
                <p className="text-muted-foreground mt-1">{application.company_name}</p>
              </div>
              <Badge className={statusColors[status]}>
                {status.charAt(0).toUpperCase() + status.slice(1)}
              </Badge>
            </div>
          </DialogHeader>

          <div className="space-y-6">
            <div className="grid grid-cols-2 gap-4">
              {application.location && (
                <div className="flex items-center gap-2 text-sm">
                  <MapPin className="h-4 w-4 text-muted-foreground" />
                  <span className="text-foreground">{application.location}</span>
                </div>
              )}
              {application.salary_range && (
                <div className="flex items-center gap-2 text-sm">
                  <DollarSign className="h-4 w-4 text-muted-foreground" />
                  <span className="text-foreground">{application.salary_range}</span>
                </div>
              )}
              <div className="flex items-center gap-2 text-sm">
                <Calendar className="h-4 w-4 text-muted-foreground" />
                <span className="text-foreground">
                  Applied {format(new Date(application.application_date), "MMM d, yyyy")}
                </span>
              </div>
              {application.job_url && (
                <a
                  href={application.job_url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2 text-sm text-primary hover:underline"
                >
                  <ExternalLink className="h-4 w-4" />
                  View Job Posting
                </a>
              )}
            </div>

            <Tabs defaultValue="details" className="w-full">
              <TabsList className="grid w-full grid-cols-2">
                <TabsTrigger value="details">Details</TabsTrigger>
                <TabsTrigger value="timeline">Timeline</TabsTrigger>
              </TabsList>

              <TabsContent value="details" className="space-y-4 mt-4">
                <Card>
                  <CardContent className="p-6 space-y-4">
                    <div className="space-y-2">
                      <Label>Status</Label>
                      <Select
                        value={status}
                        onValueChange={setStatus}
                        disabled={!isEditing}
                      >
                        <SelectTrigger>
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="applied">Applied</SelectItem>
                          <SelectItem value="screening">Screening</SelectItem>
                          <SelectItem value="interview">Interview</SelectItem>
                          <SelectItem value="offer">Offer</SelectItem>
                          <SelectItem value="rejected">Rejected</SelectItem>
                          <SelectItem value="withdrawn">Withdrawn</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>

                    <div className="space-y-2">
                      <Label>Notes</Label>
                      <Textarea
                        value={notes}
                        onChange={(e) => setNotes(e.target.value)}
                        rows={6}
                        disabled={!isEditing}
                        placeholder="Add notes about this application..."
                      />
                    </div>
                  </CardContent>
                </Card>
              </TabsContent>

              <TabsContent value="timeline" className="mt-4">
                <Card>
                  <CardContent className="p-6">
                    <p className="text-center text-muted-foreground py-8">
                      Timeline feature coming soon with email integration
                    </p>
                  </CardContent>
                </Card>
              </TabsContent>
            </Tabs>

            <div className="flex justify-between">
              <Button
                variant="destructive"
                onClick={() => setShowDeleteDialog(true)}
                disabled={loading}
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Delete
              </Button>
              <div className="flex gap-2">
                {isEditing ? (
                  <>
                    <Button
                      variant="outline"
                      onClick={() => {
                        setIsEditing(false);
                        setStatus(application.status);
                        setNotes(application.notes || "");
                      }}
                      disabled={loading}
                    >
                      Cancel
                    </Button>
                    <Button onClick={handleUpdate} disabled={loading}>
                      {loading ? "Saving..." : "Save Changes"}
                    </Button>
                  </>
                ) : (
                  <Button onClick={() => setIsEditing(true)}>
                    <Edit className="mr-2 h-4 w-4" />
                    Edit
                  </Button>
                )}
              </div>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Application</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete this application? This action cannot be
              undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} className="bg-destructive">
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
};