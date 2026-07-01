export interface ClinicResponse {
  id: string;
  organizationId?: string;
  ownerUserId: string;
  name: string;
  legalName?: string;
  rfc?: string;
  taxRegimeCode?: string;
  addressStreet?: string;
  addressColonia?: string;
  addressMunicipality?: string;
  addressState?: string;
  addressZip?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
  timezone?: string;
  privacyNoticeUrl?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateClinicRequest {
  name: string;
  email: string;
  timezone: string;
  legalName?: string;
  rfc?: string;
  taxRegimeCode?: string;
  addressStreet?: string;
  addressColonia?: string;
  addressMunicipality?: string;
  addressState?: string;
  addressZip?: string;
  phone?: string;
}

export interface UpdateClinicRequest {
  name: string;
  legalName?: string;
  rfc?: string;
  taxRegimeCode?: string;
  addressStreet?: string;
  addressColonia?: string;
  addressMunicipality?: string;
  addressState?: string;
  addressZip?: string;
  phone?: string;
  email?: string;
  logoUrl?: string;
  timezone?: string;
  privacyNoticeUrl?: string;
}
