import { FormEvent, useState } from "react";
import { IconShieldLock } from "@tabler/icons-react";
import { Field } from "../components/Field";
import { authApi, getFriendlyError, sessionStore } from "../lib/api";
import type { UserProfile } from "../types/auth";

type AuthView = "login" | "register" | "verify";

function authTitle(view: AuthView) {
  return {
    login: "Inicio de sesión",
    register: "Registro de usuario",
    verify: "Verificación de correo"
  }[view];
}

function authSubtitle(view: AuthView) {
  return {
    login: "Accede con una cuenta verificada y activa.",
    register: "Crea el usuario dueño de la clínica.",
    verify: "Confirma el token recibido por correo."
  }[view];
}

function authButton(view: AuthView) {
  return {
    login: "Iniciar sesión",
    register: "Registrar",
    verify: "Verificar correo"
  }[view];
}

export function AuthScreen({
  onAuthenticated,
  onVerifiedAndAuthenticated
}: {
  onAuthenticated: (user: UserProfile) => void;
  onVerifiedAndAuthenticated: (user: UserProfile) => void;
}) {
  const [view, setView] = useState<AuthView>("login");
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [pendingCredentials, setPendingCredentials] = useState<{ email: string; password: string } | null>(null);

  const goTo = (next: AuthView) => {
    setView(next);
    setError("");
    setNotice("");
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    setNotice("");
    const form = new FormData(event.currentTarget);
    const value = (name: string) => String(form.get(name) ?? "").trim();

    try {
      if (view === "login") {
        const data = await authApi.login({ email: value("email"), password: value("password") });
        sessionStore.setTokens(data);
        const profile = await authApi.me();
        sessionStore.setUser(profile);
        onAuthenticated(profile);
      }
      if (view === "register") {
        const email = value("email");
        const password = value("password");
        await authApi.register({
          fullName: value("fullName"),
          email,
          password,
          clinicName: value("clinicName")
        });
        setPendingCredentials({ email, password });
        setNotice("Registro exitoso. Revisa la consola del backend para obtener el token de verificación.");
        setView("verify");
      }
      if (view === "verify") {
        await authApi.verifyEmail(value("token"));
        if (pendingCredentials) {
          const data = await authApi.login(pendingCredentials);
          sessionStore.setTokens(data);
          const profile = await authApi.me();
          sessionStore.setUser(profile);
          onVerifiedAndAuthenticated(profile);
        } else {
          setNotice("Correo verificado correctamente. Ya puedes iniciar sesión.");
          setView("login");
        }
      }
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-layout">
      <section className="auth-panel" aria-labelledby="auth-title">
        <div className="brand-row">
          <span className="brand-dot" />
          <span className="brand-word">MediCloud</span>
        </div>
        <div className="auth-heading">
          <h1 id="auth-title">{authTitle(view)}</h1>
          <p>{authSubtitle(view)}</p>
        </div>

        <form className="auth-form" onSubmit={submit}>
          {view === "register" && <Field name="fullName" label="Nombre completo" autoComplete="name" required />}
          {(view === "login" || view === "register") && (
            <Field name="email" label="Correo electrónico" type="email" autoComplete="email" required />
          )}
          {view === "register" && <Field name="clinicName" label="Nombre de la clínica" required />}
          {(view === "login" || view === "register") && (
            <Field
              name="password"
              label="Contraseña"
              type="password"
              autoComplete={view === "login" ? "current-password" : "new-password"}
              required
            />
          )}
          {view === "verify" && <Field name="token" label="Token de verificación" required />}

          {error && <p className="alert error">{error}</p>}
          {notice && <p className="alert success">{notice}</p>}
          <button className="btn primary" disabled={loading} type="submit">
            <IconShieldLock size={20} aria-hidden="true" />
            {loading ? "Procesando" : authButton(view)}
          </button>
        </form>

        {view === "login" && (
          <p className="auth-switch">
            ¿No tienes cuenta?{" "}
            <button type="button" className="link-btn" onClick={() => goTo("register")}>
              Regístrate
            </button>
          </p>
        )}
        {view === "register" && (
          <p className="auth-switch">
            ¿Ya tienes cuenta?{" "}
            <button type="button" className="link-btn" onClick={() => goTo("login")}>
              Inicia sesión
            </button>
          </p>
        )}
      </section>
    </main>
  );
}
