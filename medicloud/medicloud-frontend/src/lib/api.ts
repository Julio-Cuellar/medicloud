import type {
  ApiErrorBody,
  LoginRequest,
  LoginResponse,
  MessageResponse,
  RefreshResponse,
  RegisterRequest,
  UserProfile
} from "../types/auth";
import type { ClinicResponse, CreateClinicRequest, UpdateClinicRequest } from "../types/clinic";
import type { PatientResponse, RegisterPatientRequest, UpdatePatientRequest } from "../types/patient";
import type {
  AttachmentMeta,
  CreateHistoryTemplateRequest,
  HistoryTemplateResponse,
  MedicalHistoryResponse,
  SaveMedicalHistoryRequest,
  UpdateHistoryTemplateRequest
} from "../types/clinicalRecords";

const API_BASE_URL = import.meta.env.VITE_API_URL ?? "/api";
const ACCESS_TOKEN_KEY = "medicloud.access_token";
const REFRESH_TOKEN_KEY = "medicloud.refresh_token";
const USER_KEY = "medicloud.user";

export class ApiClientError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiClientError";
    this.status = status;
  }
}

export const sessionStore = {
  getAccessToken: () => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefreshToken: () => localStorage.getItem(REFRESH_TOKEN_KEY),
  getUser: (): UserProfile | null => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as UserProfile) : null;
  },
  setTokens: (payload: { token: string; refreshToken: string }) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, payload.token);
    localStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken);
  },
  setUser: (user: UserProfile) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
};

async function request<T>(path: string, options: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(options.headers);
  if (!headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const token = sessionStore.getAccessToken();
  if (token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    if (response.status === 401 && retry && sessionStore.getRefreshToken() && path !== "/v1/auth/refresh") {
      try {
        const refreshed = await authApi.refresh();
        sessionStore.setTokens(refreshed);
        return request<T>(path, options, false);
      } catch {
        sessionStore.clear();
      }
    }
    const apiError = payload as ApiErrorBody | null;
    throw new ApiClientError(apiError?.message ?? "No se pudo completar la solicitud.", response.status);
  }

  return payload as T;
}

export const authApi = {
  register: (body: RegisterRequest) =>
    request<UserProfile>("/v1/users/register", { method: "POST", body: JSON.stringify(body) }),
  verifyEmail: (token: string) =>
    request<MessageResponse>("/v1/users/verify-email", { method: "POST", body: JSON.stringify({ token }) }),
  login: (body: LoginRequest) =>
    request<LoginResponse>("/v1/auth/login", { method: "POST", body: JSON.stringify(body) }),
  refresh: () =>
    request<RefreshResponse>(
      "/v1/auth/refresh",
      { method: "POST", body: JSON.stringify({ refreshToken: sessionStore.getRefreshToken() }) },
      false
    ),
  logout: () => request<void>("/v1/auth/logout", { method: "POST" }, false),
  me: () => request<UserProfile>("/v1/auth/me")
};

export const clinicsApi = {
  list: () => request<ClinicResponse[]>("/v1/clinics"),
  get: (clinicId: string) => request<ClinicResponse>(`/v1/clinics/${clinicId}`),
  create: (body: CreateClinicRequest) =>
    request<ClinicResponse>("/v1/clinics", { method: "POST", body: JSON.stringify(body) }),
  update: (clinicId: string, body: UpdateClinicRequest) =>
    request<ClinicResponse>(`/v1/clinics/${clinicId}`, { method: "PUT", body: JSON.stringify(body) })
};

export const patientsApi = {
  listByClinic: (clinicId: string) =>
    request<PatientResponse[]>(`/v1/patients?clinicId=${encodeURIComponent(clinicId)}`),
  get: (patientId: string) => request<PatientResponse>(`/v1/patients/${patientId}`),
  register: (body: RegisterPatientRequest) =>
    request<PatientResponse>("/v1/patients", { method: "POST", body: JSON.stringify(body) }),
  update: (patientId: string, body: UpdatePatientRequest) =>
    request<PatientResponse>(`/v1/patients/${patientId}`, { method: "PUT", body: JSON.stringify(body) }),
  remove: (patientId: string) => request<void>(`/v1/patients/${patientId}`, { method: "DELETE" })
};

export const historyTemplatesApi = {
  list: (clinicId: string) => request<HistoryTemplateResponse[]>(`/v1/clinics/${clinicId}/history-templates`),
  create: (clinicId: string, body: CreateHistoryTemplateRequest) =>
    request<HistoryTemplateResponse>(`/v1/clinics/${clinicId}/history-templates`, {
      method: "POST",
      body: JSON.stringify(body)
    }),
  update: (clinicId: string, templateId: string, body: UpdateHistoryTemplateRequest) =>
    request<HistoryTemplateResponse>(`/v1/clinics/${clinicId}/history-templates/${templateId}`, {
      method: "PUT",
      body: JSON.stringify(body)
    }),
  remove: (clinicId: string, templateId: string) =>
    request<void>(`/v1/clinics/${clinicId}/history-templates/${templateId}`, { method: "DELETE" })
};

export const medicalHistoryApi = {
  listByPatient: (patientId: string, clinicId: string) =>
    request<MedicalHistoryResponse[]>(`/v1/patients/${patientId}/medical-history?clinicId=${encodeURIComponent(clinicId)}`),
  getByTemplate: (patientId: string, templateId: string, clinicId: string) =>
    request<MedicalHistoryResponse>(
      `/v1/patients/${patientId}/medical-history/by-template/${templateId}?clinicId=${encodeURIComponent(clinicId)}`
    ),
  save: (patientId: string, body: SaveMedicalHistoryRequest) =>
    request<MedicalHistoryResponse>(`/v1/patients/${patientId}/medical-history`, {
      method: "PUT",
      body: JSON.stringify(body)
    })
};

export const attachmentsApi = {
  list: (patientId: string, elementId: string) =>
    request<AttachmentMeta[]>(`/v1/patients/${patientId}/attachments?elementId=${encodeURIComponent(elementId)}`),
  upload: async (patientId: string, clinicId: string, elementId: string, file: File): Promise<AttachmentMeta> => {
    const formData = new FormData();
    formData.append("file", file);
    const headers = new Headers();
    const token = sessionStore.getAccessToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
    const response = await fetch(
      `${API_BASE_URL}/v1/patients/${patientId}/attachments?clinicId=${encodeURIComponent(clinicId)}&elementId=${encodeURIComponent(elementId)}`,
      { method: "POST", headers, body: formData }
    );
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      const apiError = payload as ApiErrorBody | null;
      throw new ApiClientError(apiError?.message ?? "No se pudo subir el archivo.", response.status);
    }
    return payload as AttachmentMeta;
  },
  remove: (patientId: string, attachmentId: string) =>
    request<void>(`/v1/patients/${patientId}/attachments/${attachmentId}`, { method: "DELETE" }),
  downloadBlob: async (patientId: string, attachmentId: string): Promise<Blob> => {
    const headers = new Headers();
    const token = sessionStore.getAccessToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
    const response = await fetch(`${API_BASE_URL}/v1/patients/${patientId}/attachments/${attachmentId}/content`, { headers });
    if (!response.ok) throw new ApiClientError("No se pudo descargar el archivo.", response.status);
    return response.blob();
  }
};

export function getFriendlyError(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.message.includes("DataIntegrityViolationException") && error.message.toLowerCase().includes("phone")) {
      return "El teléfono es obligatorio.";
    }
    return error.message;
  }
  return "Ocurrió un error inesperado.";
}
