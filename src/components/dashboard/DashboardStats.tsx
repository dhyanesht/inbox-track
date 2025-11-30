import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Briefcase, Clock, CheckCircle, XCircle } from "lucide-react";

interface DashboardStatsProps {
  applications: any[];
}

export const DashboardStats = ({ applications }: DashboardStatsProps) => {
  const total = applications.length;
  const pending = applications.filter((app) => 
    ["applied", "screening"].includes(app.status)
  ).length;
  const interviews = applications.filter((app) => app.status === "interview").length;
  const rejected = applications.filter((app) => app.status === "rejected").length;

  const stats = [
    {
      title: "Total Applications",
      value: total,
      icon: Briefcase,
      color: "text-primary",
    },
    {
      title: "Pending Review",
      value: pending,
      icon: Clock,
      color: "text-accent-foreground",
    },
    {
      title: "Interviews",
      value: interviews,
      icon: CheckCircle,
      color: "text-primary",
    },
    {
      title: "Rejected",
      value: rejected,
      icon: XCircle,
      color: "text-destructive",
    },
  ];

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      {stats.map((stat) => {
        const Icon = stat.icon;
        return (
          <Card key={stat.title} className="transition-all hover:shadow-lg">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-card-foreground">
                {stat.title}
              </CardTitle>
              <Icon className={`h-5 w-5 ${stat.color}`} />
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold text-card-foreground">{stat.value}</div>
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
};