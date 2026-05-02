-- ═══════════════════════════════════════════════════════════════════════════
--  TecrobSys — Schema completo de Supabase
--  Versión: 2.1  |  2026-05-01
--
--  Tablas:  empresa · tecnico · cliente · servicio_catalogo
--           orden · equipo · orden_servicio · historial_estado · pago
--
--  INSTRUCCIONES:
--    1. Ejecuta este script completo en Supabase → SQL Editor → Run.
--    2. Ve a Authentication → Users → Add user (email + contraseña).
--    3. Copia el UUID del usuario recién creado.
--    4. Ejecuta el INSERT de la sección "PASO FINAL" con ese UUID.
-- ═══════════════════════════════════════════════════════════════════════════


-- ───────────────────────────────────────────────────────────────────────────
-- 0. LIMPIEZA  (DROP en orden inverso de dependencias)
-- ───────────────────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS pago              CASCADE;
DROP TABLE IF EXISTS historial_estado  CASCADE;
DROP TABLE IF EXISTS orden_servicio    CASCADE;
DROP TABLE IF EXISTS equipo            CASCADE;
DROP TABLE IF EXISTS orden             CASCADE;
DROP TABLE IF EXISTS servicio_catalogo CASCADE;
DROP TABLE IF EXISTS cliente           CASCADE;
DROP TABLE IF EXISTS tecnico           CASCADE;
DROP TABLE IF EXISTS empresa           CASCADE;

DROP SEQUENCE IF EXISTS orden_numero_seq;

DROP FUNCTION IF EXISTS obtener_empresa_id_usuario();
DROP FUNCTION IF EXISTS generar_numero_orden();
DROP FUNCTION IF EXISTS actualizar_updated_at();
DROP FUNCTION IF EXISTS registrar_cambio_estado();
DROP FUNCTION IF EXISTS actualizar_subtotal_orden();
DROP FUNCTION IF EXISTS actualizar_totales_desde_pago();


-- ───────────────────────────────────────────────────────────────────────────
-- 1. TABLAS BASE
--    NOTA: obtener_empresa_id_usuario() se crea DESPUÉS de la tabla tecnico
--    porque LANGUAGE sql valida las referencias en el momento de compilar.
-- ───────────────────────────────────────────────────────────────────────────

-- 1.1 empresa — un registro por taller (multi-tenant)
CREATE TABLE empresa (
  id         SERIAL PRIMARY KEY,
  nombre     TEXT NOT NULL,
  ruc        TEXT,
  telefono   TEXT,
  email      TEXT,
  direccion  TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.2 tecnico — usuarios del sistema, vinculados a Supabase Auth
CREATE TABLE tecnico (
  id           SERIAL PRIMARY KEY,
  empresa_id   INTEGER NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
  auth_user_id UUID    UNIQUE,
  nombre       TEXT    NOT NULL,
  apellido     TEXT,
  email        TEXT    NOT NULL,
  rol          TEXT    NOT NULL DEFAULT 'tecnico'
                       CHECK (rol IN ('administrador', 'tecnico')),
  activo       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.3 cliente — clientes del taller
CREATE TABLE cliente (
  id         SERIAL PRIMARY KEY,
  empresa_id INTEGER NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
  nombre     TEXT    NOT NULL,
  apellido   TEXT,
  telefono   TEXT    NOT NULL,
  email      TEXT,
  dni        TEXT,
  direccion  TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.4 servicio_catalogo — servicios y repuestos disponibles
CREATE TABLE servicio_catalogo (
  id          SERIAL PRIMARY KEY,
  empresa_id  INTEGER NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
  nombre      TEXT    NOT NULL,
  descripcion TEXT,
  precio_base NUMERIC(10,2) NOT NULL DEFAULT 0,
  categoria   TEXT NOT NULL DEFAULT 'otro'
              CHECK (categoria IN (
                'mantenimiento','reparacion','software',
                'repuesto','diagnostico','otro')),
  activo      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ───────────────────────────────────────────────────────────────────────────
-- 2. FUNCIÓN AUXILIAR DE RLS
--    Se crea AQUÍ, después de la tabla tecnico.
--    Devuelve el empresa_id del técnico autenticado.
-- ───────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION obtener_empresa_id_usuario()
RETURNS INTEGER
LANGUAGE sql
STABLE
SECURITY DEFINER
AS $$
  SELECT empresa_id
  FROM   tecnico
  WHERE  auth_user_id = auth.uid()
  LIMIT  1;
$$;


-- ───────────────────────────────────────────────────────────────────────────
-- 3. SECUENCIA DE NÚMERO DE ORDEN  →  ORD-0001, ORD-0002 …
-- ───────────────────────────────────────────────────────────────────────────
CREATE SEQUENCE orden_numero_seq START 1;


-- ───────────────────────────────────────────────────────────────────────────
-- 4. TABLA ORDEN  (entidad central del sistema)
-- ───────────────────────────────────────────────────────────────────────────
CREATE TABLE orden (
  id                SERIAL PRIMARY KEY,
  numero_orden      TEXT    UNIQUE,
  empresa_id        INTEGER NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
  cliente_id        INTEGER NOT NULL REFERENCES cliente(id) ON DELETE RESTRICT,
  tecnico_id        INTEGER NOT NULL REFERENCES tecnico(id) ON DELETE RESTRICT,

  estado            TEXT NOT NULL DEFAULT 'pendiente'
                    CHECK (estado IN (
                      'pendiente','diagnostico','en_progreso',
                      'listo','entregado','cancelado','sin_reparacion')),
  prioridad         TEXT NOT NULL DEFAULT 'normal'
                    CHECK (prioridad IN ('baja','normal','alta','urgente')),

  subtotal          NUMERIC(10,2) NOT NULL DEFAULT 0,
  descuento         NUMERIC(10,2) NOT NULL DEFAULT 0,
  total             NUMERIC(10,2) NOT NULL DEFAULT 0,
  adelanto          NUMERIC(10,2) NOT NULL DEFAULT 0,
  saldo_pendiente   NUMERIC(10,2) NOT NULL DEFAULT 0,

  contrasena_equipo TEXT,
  fecha_prometida   DATE,
  observaciones     TEXT,
  pdf_url           TEXT,

  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ───────────────────────────────────────────────────────────────────────────
-- 5. TABLAS DEPENDIENTES DE ORDEN
-- ───────────────────────────────────────────────────────────────────────────

-- 5.1 equipo — relación 1:1 con orden
CREATE TABLE equipo (
  id                  SERIAL PRIMARY KEY,
  orden_id            INTEGER NOT NULL UNIQUE REFERENCES orden(id) ON DELETE CASCADE,
  tipo                TEXT NOT NULL DEFAULT 'otro'
                      CHECK (tipo IN (
                        'laptop','computadora','impresora','fotocopiadora',
                        'tablet','celular','parlante','otro')),
  marca               TEXT,
  modelo              TEXT,
  numero_serie        TEXT,
  desperfecto         TEXT,
  descripcion_general TEXT,
  accesorios          TEXT,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5.2 orden_servicio — servicios/repuestos aplicados a una orden
CREATE TABLE orden_servicio (
  id              SERIAL PRIMARY KEY,
  orden_id        INTEGER NOT NULL REFERENCES orden(id)             ON DELETE CASCADE,
  servicio_id     INTEGER NOT NULL REFERENCES servicio_catalogo(id) ON DELETE RESTRICT,
  precio_unitario NUMERIC(10,2) NOT NULL DEFAULT 0,
  cantidad        INTEGER NOT NULL DEFAULT 1 CHECK (cantidad > 0),
  subtotal        NUMERIC(10,2) GENERATED ALWAYS AS (precio_unitario * cantidad) STORED
);

-- 5.3 historial_estado — log automático de cambios de estado (RF-10)
CREATE TABLE historial_estado (
  id           BIGSERIAL PRIMARY KEY,
  orden_id     INTEGER NOT NULL REFERENCES orden(id)    ON DELETE CASCADE,
  estado_prev  TEXT,
  estado_nuevo TEXT    NOT NULL,
  tecnico_id   INTEGER REFERENCES tecnico(id)           ON DELETE SET NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5.4 pago — pagos parciales o totales (RF-17)
CREATE TABLE pago (
  id         BIGSERIAL PRIMARY KEY,
  orden_id   INTEGER NOT NULL REFERENCES orden(id) ON DELETE CASCADE,
  monto      NUMERIC(10,2) NOT NULL CHECK (monto > 0),
  metodo     TEXT NOT NULL
             CHECK (metodo IN ('efectivo','yape','plin','transferencia','tarjeta')),
  nota       TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ───────────────────────────────────────────────────────────────────────────
-- 6. FUNCIONES Y TRIGGERS
-- ───────────────────────────────────────────────────────────────────────────

-- 6.1 Genera numero_orden = "ORD-0001" al insertar
CREATE OR REPLACE FUNCTION generar_numero_orden()
RETURNS TRIGGER AS $$
BEGIN
  NEW.numero_orden := 'ORD-' || LPAD(nextval('orden_numero_seq')::TEXT, 4, '0');
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_numero_orden
  BEFORE INSERT ON orden
  FOR EACH ROW
  EXECUTE FUNCTION generar_numero_orden();


-- 6.2 Mantiene updated_at actualizado
CREATE OR REPLACE FUNCTION actualizar_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_updated_at
  BEFORE UPDATE ON orden
  FOR EACH ROW
  EXECUTE FUNCTION actualizar_updated_at();


-- 6.3 Registra cada cambio de estado en historial_estado (RF-10)
CREATE OR REPLACE FUNCTION registrar_cambio_estado()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.estado IS DISTINCT FROM OLD.estado THEN
    INSERT INTO historial_estado (orden_id, estado_prev, estado_nuevo, tecnico_id)
    VALUES (NEW.id, OLD.estado, NEW.estado, NEW.tecnico_id);
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trigger_historial_estado
  AFTER UPDATE OF estado ON orden
  FOR EACH ROW
  EXECUTE FUNCTION registrar_cambio_estado();


-- 6.4 Recalcula subtotal/total/saldo al agregar o quitar servicios (RF-09)
CREATE OR REPLACE FUNCTION actualizar_subtotal_orden()
RETURNS TRIGGER AS $$
DECLARE
  oid            INTEGER;
  nuevo_subtotal NUMERIC(10,2);
BEGIN
  oid := CASE TG_OP WHEN 'DELETE' THEN OLD.orden_id ELSE NEW.orden_id END;

  SELECT COALESCE(SUM(subtotal), 0)
  INTO   nuevo_subtotal
  FROM   orden_servicio
  WHERE  orden_id = oid;

  UPDATE orden
  SET
    subtotal        = nuevo_subtotal,
    total           = nuevo_subtotal - descuento,
    saldo_pendiente = (nuevo_subtotal - descuento) - adelanto,
    updated_at      = NOW()
  WHERE id = oid;

  RETURN CASE TG_OP WHEN 'DELETE' THEN OLD ELSE NEW END;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trigger_subtotal_servicios
  AFTER INSERT OR UPDATE OR DELETE ON orden_servicio
  FOR EACH ROW
  EXECUTE FUNCTION actualizar_subtotal_orden();


-- 6.5 Recalcula adelanto/saldo al registrar un pago (RF-17 / RF-09)
CREATE OR REPLACE FUNCTION actualizar_totales_desde_pago()
RETURNS TRIGGER AS $$
DECLARE
  oid            INTEGER;
  nuevo_adelanto NUMERIC(10,2);
BEGIN
  oid := CASE TG_OP WHEN 'DELETE' THEN OLD.orden_id ELSE NEW.orden_id END;

  SELECT COALESCE(SUM(monto), 0)
  INTO   nuevo_adelanto
  FROM   pago
  WHERE  orden_id = oid;

  UPDATE orden
  SET
    adelanto        = nuevo_adelanto,
    saldo_pendiente = total - nuevo_adelanto,
    updated_at      = NOW()
  WHERE id = oid;

  RETURN CASE TG_OP WHEN 'DELETE' THEN OLD ELSE NEW END;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trigger_totales_pago
  AFTER INSERT OR UPDATE OR DELETE ON pago
  FOR EACH ROW
  EXECUTE FUNCTION actualizar_totales_desde_pago();


-- ───────────────────────────────────────────────────────────────────────────
-- 7. ROW LEVEL SECURITY
-- ───────────────────────────────────────────────────────────────────────────
ALTER TABLE empresa           ENABLE ROW LEVEL SECURITY;
ALTER TABLE tecnico           ENABLE ROW LEVEL SECURITY;
ALTER TABLE cliente           ENABLE ROW LEVEL SECURITY;
ALTER TABLE servicio_catalogo ENABLE ROW LEVEL SECURITY;
ALTER TABLE orden             ENABLE ROW LEVEL SECURITY;
ALTER TABLE equipo            ENABLE ROW LEVEL SECURITY;
ALTER TABLE orden_servicio    ENABLE ROW LEVEL SECURITY;
ALTER TABLE historial_estado  ENABLE ROW LEVEL SECURITY;
ALTER TABLE pago              ENABLE ROW LEVEL SECURITY;

-- empresa
CREATE POLICY "empresa_select"
  ON empresa FOR SELECT
  USING (id = obtener_empresa_id_usuario());

-- tecnico
CREATE POLICY "tecnico_select"
  ON tecnico FOR SELECT
  USING (empresa_id = obtener_empresa_id_usuario());

CREATE POLICY "tecnico_update_propio"
  ON tecnico FOR UPDATE
  USING (auth_user_id = auth.uid());

-- cliente
CREATE POLICY "cliente_select"
  ON cliente FOR SELECT
  USING (empresa_id = obtener_empresa_id_usuario());

CREATE POLICY "cliente_insert"
  ON cliente FOR INSERT
  WITH CHECK (empresa_id = obtener_empresa_id_usuario());

CREATE POLICY "cliente_update"
  ON cliente FOR UPDATE
  USING (empresa_id = obtener_empresa_id_usuario());

CREATE POLICY "cliente_delete"
  ON cliente FOR DELETE
  USING (empresa_id = obtener_empresa_id_usuario());

-- servicio_catalogo
CREATE POLICY "catalogo_select"
  ON servicio_catalogo FOR SELECT
  USING (empresa_id = obtener_empresa_id_usuario());

CREATE POLICY "catalogo_insert"
  ON servicio_catalogo FOR INSERT
  WITH CHECK (empresa_id = obtener_empresa_id_usuario());

CREATE POLICY "catalogo_update"
  ON servicio_catalogo FOR UPDATE
  USING (empresa_id = obtener_empresa_id_usuario());

CREATE POLICY "catalogo_delete"
  ON servicio_catalogo FOR DELETE
  USING (empresa_id = obtener_empresa_id_usuario());

-- orden
CREATE POLICY "orden_select"
  ON orden FOR SELECT
  USING (empresa_id = obtener_empresa_id_usuario());

CREATE POLICY "orden_insert"
  ON orden FOR INSERT
  WITH CHECK (empresa_id = obtener_empresa_id_usuario());

CREATE POLICY "orden_update"
  ON orden FOR UPDATE
  USING (empresa_id = obtener_empresa_id_usuario());

CREATE POLICY "orden_delete"
  ON orden FOR DELETE
  USING (empresa_id = obtener_empresa_id_usuario());

-- equipo
CREATE POLICY "equipo_select"
  ON equipo FOR SELECT
  USING (EXISTS (
    SELECT 1 FROM orden o
    WHERE o.id = equipo.orden_id
      AND o.empresa_id = obtener_empresa_id_usuario()
  ));

CREATE POLICY "equipo_insert"
  ON equipo FOR INSERT
  WITH CHECK (EXISTS (
    SELECT 1 FROM orden o
    WHERE o.id = equipo.orden_id
      AND o.empresa_id = obtener_empresa_id_usuario()
  ));

CREATE POLICY "equipo_update"
  ON equipo FOR UPDATE
  USING (EXISTS (
    SELECT 1 FROM orden o
    WHERE o.id = equipo.orden_id
      AND o.empresa_id = obtener_empresa_id_usuario()
  ));

-- orden_servicio
CREATE POLICY "orden_servicio_select"
  ON orden_servicio FOR SELECT
  USING (EXISTS (
    SELECT 1 FROM orden o
    WHERE o.id = orden_servicio.orden_id
      AND o.empresa_id = obtener_empresa_id_usuario()
  ));

CREATE POLICY "orden_servicio_insert"
  ON orden_servicio FOR INSERT
  WITH CHECK (EXISTS (
    SELECT 1 FROM orden o
    WHERE o.id = orden_servicio.orden_id
      AND o.empresa_id = obtener_empresa_id_usuario()
  ));

CREATE POLICY "orden_servicio_update"
  ON orden_servicio FOR UPDATE
  USING (EXISTS (
    SELECT 1 FROM orden o
    WHERE o.id = orden_servicio.orden_id
      AND o.empresa_id = obtener_empresa_id_usuario()
  ));

CREATE POLICY "orden_servicio_delete"
  ON orden_servicio FOR DELETE
  USING (EXISTS (
    SELECT 1 FROM orden o
    WHERE o.id = orden_servicio.orden_id
      AND o.empresa_id = obtener_empresa_id_usuario()
  ));

-- historial_estado (solo lectura; el trigger escribe con SECURITY DEFINER)
CREATE POLICY "historial_select"
  ON historial_estado FOR SELECT
  USING (EXISTS (
    SELECT 1 FROM orden o
    WHERE o.id = historial_estado.orden_id
      AND o.empresa_id = obtener_empresa_id_usuario()
  ));

-- pago
CREATE POLICY "pago_select"
  ON pago FOR SELECT
  USING (EXISTS (
    SELECT 1 FROM orden o
    WHERE o.id = pago.orden_id
      AND o.empresa_id = obtener_empresa_id_usuario()
  ));

CREATE POLICY "pago_insert"
  ON pago FOR INSERT
  WITH CHECK (EXISTS (
    SELECT 1 FROM orden o
    WHERE o.id = pago.orden_id
      AND o.empresa_id = obtener_empresa_id_usuario()
  ));


-- ───────────────────────────────────────────────────────────────────────────
-- 8. ÍNDICES
-- ───────────────────────────────────────────────────────────────────────────
CREATE INDEX idx_orden_empresa      ON orden(empresa_id);
CREATE INDEX idx_orden_estado       ON orden(estado);
CREATE INDEX idx_orden_cliente      ON orden(cliente_id);
CREATE INDEX idx_orden_created      ON orden(created_at DESC);
CREATE INDEX idx_equipo_orden       ON equipo(orden_id);
CREATE INDEX idx_historial_orden    ON historial_estado(orden_id);
CREATE INDEX idx_pago_orden         ON pago(orden_id);
CREATE INDEX idx_cliente_empresa    ON cliente(empresa_id);
CREATE INDEX idx_catalogo_empresa   ON servicio_catalogo(empresa_id);
CREATE INDEX idx_orden_servicio_ord ON orden_servicio(orden_id);
CREATE INDEX idx_tecnico_auth_uid   ON tecnico(auth_user_id);


-- ───────────────────────────────────────────────────────────────────────────
-- 9. DATOS INICIALES
-- ───────────────────────────────────────────────────────────────────────────

INSERT INTO empresa (id, nombre, ruc, telefono, email, direccion)
VALUES (
  1,
  'Multiservicios Tecrob Sys E.I.R.L.',
  '20601234567',
  '987654321',
  'contacto@tecrobsys.pe',
  'Av. Principal 123, Lima'
);

INSERT INTO servicio_catalogo (empresa_id, nombre, descripcion, precio_base, categoria, activo) VALUES
  (1, 'Mantenimiento preventivo laptop',  'Limpieza interna, pasta térmica y revisión general', 60.00,  'mantenimiento', TRUE),
  (1, 'Mantenimiento preventivo PC',      'Limpieza interna y revisión de componentes',         50.00,  'mantenimiento', TRUE),
  (1, 'Limpieza externa equipo',          'Limpieza de carcasa y puertos',                      25.00,  'mantenimiento', TRUE),
  (1, 'Diagnóstico de hardware',          'Revisión completa de componentes',                   35.00,  'diagnostico',   TRUE),
  (1, 'Diagnóstico de software',          'Análisis del sistema operativo y programas',         25.00,  'diagnostico',   TRUE),
  (1, 'Instalación de Windows 10/11',     'Instalación limpia con controladores',               80.00,  'software',      TRUE),
  (1, 'Instalación de Office 365',        'Activación incluida',                                50.00,  'software',      TRUE),
  (1, 'Eliminación de virus/malware',     'Limpieza completa del sistema',                      45.00,  'software',      TRUE),
  (1, 'Recuperación de datos',            'Recuperación desde disco dañado',                   120.00,  'software',      TRUE),
  (1, 'Cambio de pantalla laptop',        'Incluye mano de obra',                              150.00,  'reparacion',    TRUE),
  (1, 'Cambio de teclado laptop',         'Incluye mano de obra',                               80.00,  'reparacion',    TRUE),
  (1, 'Cambio de pasta térmica',          'Incluye limpieza del disipador',                     30.00,  'reparacion',    TRUE),
  (1, 'Reparación de fuente de poder',    'Diagnóstico y reemplazo de componentes',             90.00,  'reparacion',    TRUE),
  (1, 'SSD 240GB',                        'Disco de estado sólido 2.5"',                       120.00,  'repuesto',      TRUE);

INSERT INTO cliente (empresa_id, nombre, apellido, telefono, email, dni)
VALUES (1, 'Carlos', 'Mendoza', '987123456', 'carlos.mendoza@email.com', '12345678');


-- ───────────────────────────────────────────────────────────────────────────
-- 10. TÉCNICO ADMINISTRADOR
--     Se inserta SIN auth_user_id (NULL) para que el script corra sin errores.
--     Después vincula el UUID en el PASO FINAL de abajo.
-- ───────────────────────────────────────────────────────────────────────────
INSERT INTO tecnico (empresa_id, nombre, apellido, email, rol, activo)
VALUES (
  1,
  'Adler',
  'Cisneros',
  'adlercisneros147@gmail.com',
  'administrador',
  TRUE
);


-- ───────────────────────────────────────────────────────────────────────────
-- PASO FINAL — vincular el UUID de Supabase Auth (ejecutar por separado)
--
--  1. Ve a Supabase → Authentication → Users → Add user
--     Email: adlercisneros147@gmail.com  |  Contraseña: la que usas en la app
--  2. Copia el UUID de la columna "UID" del usuario recién creado.
--  3. Ejecuta SOLO este UPDATE en una nueva consulta del SQL Editor:
--
--     UPDATE tecnico
--     SET    auth_user_id = 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'
--     WHERE  email = 'adlercisneros147@gmail.com';
-- ───────────────────────────────────────────────────────────────────────────


-- ───────────────────────────────────────────────────────────────────────────
-- VERIFICACIÓN  (descomenta y ejecuta después del script)
-- ───────────────────────────────────────────────────────────────────────────
/*
SELECT tabla, conteo FROM (
  SELECT 'empresa'          AS tabla, COUNT(*)::INT AS conteo FROM empresa
  UNION ALL SELECT 'tecnico',          COUNT(*)::INT FROM tecnico
  UNION ALL SELECT 'cliente',          COUNT(*)::INT FROM cliente
  UNION ALL SELECT 'servicio_catalogo',COUNT(*)::INT FROM servicio_catalogo
  UNION ALL SELECT 'orden',            COUNT(*)::INT FROM orden
  UNION ALL SELECT 'equipo',           COUNT(*)::INT FROM equipo
  UNION ALL SELECT 'pago',             COUNT(*)::INT FROM pago
  UNION ALL SELECT 'historial_estado', COUNT(*)::INT FROM historial_estado
) t;
*/
