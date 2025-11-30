import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Search, Building2, MapPin } from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import { ApplicationDetailDialog } from "./ApplicationDetailDialog";

interface ApplicationsListProps {
  applications: any[];
  isLoading: boolean;
  onRefetch: () => void;
}

export const ApplicationsList = ({
  applications,
  isLoading,
  onRefetch,
}: ApplicationsListProps) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [selectedApplication, setSelectedApplication] = useState<any>(null);

  const filteredApplications = applications.filter((app) => {
    const matchesSearch =
      app.company_name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      app.position.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = statusFilter === "all" || app.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const statusColors: Record<string, string> = {
    applied: "bg-accent text-accent-foreground",
    screening: "bg-secondary text-secondary-foreground",
    interview: "bg-primary text-primary-foreground",
    offer: "bg-primary text-primary-foreground",
    rejected: "bg-destructive text-destructive-foreground",
    withdrawn: "bg-muted text-muted-foreground",
  };

  if (isLoading) {
    return (
      <Card>
        <CardContent className="p-8 text-center">
          <p className="text-muted-foreground">Loading applications...</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle className="text-2xl text-card-foreground">Your Applications</CardTitle>
          <div className="flex flex-col md:flex-row gap-4 mt-4">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search companies or positions..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-full md:w-[200px]">
                <SelectValue placeholder="Filter by status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Status</SelectItem>
                <SelectItem value="applied">Applied</SelectItem>
                <SelectItem value="screening">Screening</SelectItem>
                <SelectItem value="interview">Interview</SelectItem>
                <SelectItem value="offer">Offer</SelectItem>
                <SelectItem value="rejected">Rejected</SelectItem>
                <SelectItem value="withdrawn">Withdrawn</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardHeader>
        <CardContent>
          {filteredApplications.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-muted-foreground">No applications found</p>
              <p className="text-sm text-muted-foreground mt-2">
                {applications.length === 0
                  ? "Start tracking your job applications by adding one!"
                  : "Try adjusting your search or filters"}
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {filteredApplications.map((app) => (
                <Card
                  key={app.id}
                  className="cursor-pointer transition-all hover:shadow-md"
                  onClick={() => setSelectedApplication(app)}
                >
                  <CardContent className="p-6">
                    <div className="flex justify-between items-start">
                      <div className="flex-1">
                        <div className="flex items-start gap-3">
                          <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center">
                            <Building2 className="h-6 w-6 text-primary" />
                          </div>
                          <div className="flex-1">
                            <h3 className="text-xl font-semibold text-card-foreground">
                              {app.position}
                            </h3>
                            <p className="text-muted-foreground font-medium mt-1">
                              {app.company_name}
                            </p>
                            {app.location && (
                              <div className="flex items-center gap-1 mt-2 text-sm text-muted-foreground">
                                <MapPin className="h-4 w-4" />
                                {app.location}
                              </div>
                            )}
                            <p className="text-sm text-muted-foreground mt-2">
                              Applied{" "}
                              {formatDistanceToNow(new Date(app.application_date), {
                                addSuffix: true,
                              })}
                            </p>
                          </div>
                        </div>
                      </div>
                      <Badge className={statusColors[app.status] || ""}>
                        {app.status.charAt(0).toUpperCase() + app.status.slice(1)}
                      </Badge>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {selectedApplication && (
        <ApplicationDetailDialog
          application={selectedApplication}
          open={!!selectedApplication}
          onOpenChange={(open) => !open && setSelectedApplication(null)}
          onRefetch={onRefetch}
        />
      )}
    </>
  );
};