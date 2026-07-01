import { regimenesFiscales } from "../constants/regimenFiscal";
import type { ClinicResponse } from "../types/clinic";
import { Field } from "./Field";

const regimenFiscalOptions = regimenesFiscales.map((regimen) => ({ value: regimen.code, label: regimen.label }));

export function ClinicFields({ clinic, requireEmail }: { clinic?: ClinicResponse; requireEmail?: boolean }) {
  return (
    <>
      <Field name="name" label="Nombre de la clínica" defaultValue={clinic?.name} required />
      <Field name="email" label="Correo de contacto" type="email" defaultValue={clinic?.email} required={requireEmail} />
      <Field name="phone" label="Teléfono" defaultValue={clinic?.phone} />
      <Field name="timezone" label="Zona horaria" defaultValue={clinic?.timezone ?? "America/Mexico_City"} />
      <Field name="legalName" label="Razón social" defaultValue={clinic?.legalName} />
      <Field name="rfc" label="RFC" defaultValue={clinic?.rfc} />
      <Field
        name="taxRegimeCode"
        label="Régimen fiscal"
        defaultValue={clinic?.taxRegimeCode}
        options={regimenFiscalOptions}
      />
      <Field name="addressStreet" label="Calle y número" defaultValue={clinic?.addressStreet} />
      <Field name="addressColonia" label="Colonia" defaultValue={clinic?.addressColonia} />
      <Field name="addressMunicipality" label="Municipio/Alcaldía" defaultValue={clinic?.addressMunicipality} />
      <Field name="addressState" label="Estado" defaultValue={clinic?.addressState} />
      <Field name="addressZip" label="Código postal" defaultValue={clinic?.addressZip} />
    </>
  );
}
