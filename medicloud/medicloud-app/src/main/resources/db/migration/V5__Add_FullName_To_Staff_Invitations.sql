-- Agrega el nombre completo del invitado a la tabla de invitaciones de personal.
-- Requerido por el endpoint POST /v1/staff/invite (API_Modulo_01_Auth.md v2.1).
ALTER TABLE core.staff_invitations
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(200);
