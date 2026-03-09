import { useEffect } from "react";
import { useSearchParams } from "react-router-dom";

const GmailCallback = () => {
  const [searchParams] = useSearchParams();
  const success = searchParams.get("success") === "true";

  useEffect(() => {
    console.log("[Gmail Callback] Page loaded, success:", success);
    
    if (success && window.opener) {
      console.log("[Gmail Callback] Notifying parent window and closing popup");
      // Same origin now, so postMessage will work
      window.opener.postMessage({ type: "gmail_connected", success: true }, window.location.origin);
      
      // Close popup after short delay
      setTimeout(() => {
        window.close();
      }, 500);
    } else if (success) {
      console.log("[Gmail Callback] No opener, user opened in same tab");
      // If no opener, redirect to home
      setTimeout(() => {
        window.location.href = "/";
      }, 1000);
    }
  }, [success]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="text-center space-y-4 p-8">
        <div className="text-4xl">✓</div>
        <h1 className="text-2xl font-bold text-foreground">Gmail Connected!</h1>
        <p className="text-muted-foreground">
          {window.opener ? "This window will close automatically..." : "Redirecting..."}
        </p>
      </div>
    </div>
  );
};

export default GmailCallback;
