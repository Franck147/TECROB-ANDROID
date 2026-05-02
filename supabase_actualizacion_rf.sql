-- ═══════════════════════════════════════════════════════════════════
--  TecrobSys — Script de actualización de BD (RF-10, RF-16, RF-17)
--  Ejecutar en Supabase SQL Editor
-- ═══════════════════════════════════════════════════════════════════

-- ──────────────────────────────────────────────────────────────────
-- RF-16: Accesorios del equipo
-- ──────────────────────────────────────────────────────────────────
ALTER TABLE equipo
  ADD COLUMN IF NOT EXISTS accesorios TEXT;

-- ──────────────────────────────────────────────────────────────────
-- RF-10 / RNF-10: Historial de cambios de estado
-- ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS historial_estado (
  id          BIGSERIAL PRIMARY KEY,
  orden_id    INTEGER NOT NULL REFERENCES orden(id) ON DELETE CASCADE,
  estado_prev TEXT,
  estado_nuevo TEXT NOT NULL,
  tecnico_id  INTEGER REFERENCES tecnico(id),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Función que registra cada cambio de estado en la tabla historial
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

-- Trigger sobre la tabla orden (dispara después de UPDATE)
DROP TRIGGER IF EXISTS trigger_historial_estado ON orden;
CREATE TRIGGER trigger_historial_estado
  AFTER UPDATE OF estado ON orden
  FOR EACH ROW
  EXECUTE FUNCTION registrar_cambio_estado();

-- ──────────────────────────────────────────────────────────────────
-- RF-17: Registro de pagos
-- ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pago (
  id        BIGSERIAL PRIMARY KEY,
  orden_id  INTEGER NOT NULL REFERENCES orden(id) ON DELETE CASCADE,
  monto     NUMERIC(10,2) NOT NULL CHECK (monto > 0),
  metodo    TEXT NOT NULL CHECK (metodo IN (
              'efectivo', 'yape', 'plin', 'transferencia', 'tarjeta')),
  nota      TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Función que actualiza orden.adelanto y orden.saldo_pendiente
-- cada vez que se inserta, actualiza o elimina un pago
CREATE OR REPLACE FUNCTION actualizar_totales_orden()
RETURNS TRIGGER AS $$
DECLARE
  oid INTEGER;
BEGIN
  oid := CASE TG_OP WHEN 'DELETE' THEN OLD.orden_id ELSE NEW.orden_id END;

  UPDATE orden
  SET
    adelanto        = (SELECT COALESCE(SUM(monto), 0) FROM pago WHERE orden_id = oid),
    saldo_pendiente = subtotal - descuento
                      - (SELECT COALESCE(SUM(monto), 0) FROM pago WHERE orden_id = oid),
    updated_at      = NOW()
  WHERE id = oid;

  RETURN CASE TG_OP WHEN 'DELETE' THEN OLD ELSE NEW END;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_totales_pago ON pago;
CREATE TRIGGER trigger_totales_pago
  AFTER INSERT OR UPDATE OR DELETE ON pago
  FOR EACH ROW
  EXECUTE FUNCTION actualizar_totales_orden();

-- ──────────────────────────────────────────────────────────────────
-- RLS: políticas para las nuevas tablas
-- ──────────────────────────────────────────────────────────────────

-- historial_estado: solo usuarios de la misma empresa pueden leer
ALTER TABLE historial_estado ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "historial_select_empresa" ON historial_estado;
CREATE POLICY "historial_select_empresa" ON historial_estado
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM orden o
      WHERE o.id = historial_estado.orden_id
        AND o.empresa_id = obtener_empresa_id_usuario()
    )
  );

-- pago: técnicos de la empresa pueden insertar y leer
ALTER TABLE pago ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "pago_select_empresa" ON pago;
CREATE POLICY "pago_select_empresa" ON pago
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM orden o
      WHERE o.id = pago.orden_id
        AND o.empresa_id = obtener_empresa_id_usuario()
    )
  );

DROP POLICY IF EXISTS "pago_insert_empresa" ON pago;
CREATE POLICY "pago_insert_empresa" ON pago
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM orden o
      WHERE o.id = pago.orden_id
        AND o.empresa_id = obtener_empresa_id_usuario()
    )
  );

-- ──────────────────────────────────────────────────────────────────
-- RF-09: Índice de rendimiento para suma de pagos por orden
-- ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_pago_orden_id ON pago(orden_id);
CREATE INDEX IF NOT EXISTS idx_historial_orden_id ON historial_estado(orden_id);
