ALTER TABLE core.doctor_profiles
    ALTER COLUMN anio_egreso TYPE INTEGER USING anio_egreso::INTEGER;
