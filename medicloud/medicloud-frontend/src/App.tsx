import { useEffect, useState } from "react";
import { AppShell } from "./screens/AppShell";
import { AuthScreen } from "./screens/AuthScreen";
import { ClinicSetupScreen } from "./screens/ClinicSetupScreen";
import { authApi, sessionStore } from "./lib/api";
import type { UserProfile } from "./types/auth";
import type { Theme } from "./types/theme";

export default function App() {
  const [user, setUser] = useState<UserProfile | null>(() => sessionStore.getUser());
  const [needsClinicSetup, setNeedsClinicSetup] = useState(false);
  const [theme, setTheme] = useState<Theme>(() => (localStorage.getItem("medicloud.theme") as Theme) ?? "light");

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("medicloud.theme", theme);
  }, [theme]);

  useEffect(() => {
    if (!sessionStore.getAccessToken()) return;
    authApi
      .me()
      .then((profile) => {
        sessionStore.setUser(profile);
        setUser(profile);
      })
      .catch(() => {
        sessionStore.clear();
        setUser(null);
      });
  }, []);

  if (!user) {
    return (
      <AuthScreen
        onAuthenticated={setUser}
        onVerifiedAndAuthenticated={(profile) => {
          setUser(profile);
          setNeedsClinicSetup(true);
        }}
      />
    );
  }

  if (needsClinicSetup) {
    return (
      <ClinicSetupScreen
        user={user}
        onDone={(updatedUser) => {
          setUser(updatedUser);
          setNeedsClinicSetup(false);
        }}
      />
    );
  }

  return <AppShell user={user} setUser={setUser} theme={theme} setTheme={setTheme} />;
}
