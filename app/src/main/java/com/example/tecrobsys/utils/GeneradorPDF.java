package com.example.tecrobsys.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.example.tecrobsys.modelos.ItemOrden;
import com.example.tecrobsys.modelos.Orden;
import java.io.File;
import java.util.List;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * GeneradorPDF — Genera reportes PDF profesionales de órdenes de servicio.
 *
 * Usa la API nativa de Android (android.graphics.pdf.PdfDocument).
 * No requiere dependencias externas.
 *
 * Formato: A4 a 72dpi → 595 × 842 px
 *
 * IMPORTANTE: ejecutar en hilo secundario para no bloquear la UI.
 * El método generarOrden() es sincrónico (escribe el archivo en disco).
 */
public class GeneradorPDF {

    // ── Dimensiones de página (A4 a 72dpi) ───────────────────────────
    private static final int PAGE_WIDTH  = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN      = 40;
    private static final int LINE_HEIGHT = 22;

    // ── Colores corporativos ──────────────────────────────────────────
    private static final int COLOR_ROJO      = Color.parseColor("#E85D5D");
    private static final int COLOR_ROJO_DARK = Color.parseColor("#C43C3C");
    private static final int COLOR_GRIS      = Color.parseColor("#555555");
    private static final int COLOR_GRIS_CLARO= Color.parseColor("#EEEEEE");
    private static final int COLOR_NEGRO     = Color.parseColor("#1A1A1A");
    private static final int COLOR_BLANCO    = Color.WHITE;

    /**
     * Genera el PDF de una orden y retorna su URI para compartir.
     *
     * @param context Context de la aplicación (para acceder a filesDir y FileProvider)
     * @param orden   Orden completa con cliente, equipo y totales
     * @return Uri del PDF listo para compartir, o null si ocurrió un error
     */
    public static Uri generarOrden(Context context, Orden orden) {
        PdfDocument document = new PdfDocument();

        try {
            // Crear página A4
            PdfDocument.PageInfo pageInfo =
                    new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Dibujar contenido
            int y = dibujarHeader(canvas, orden);
            y = dibujarSeparador(canvas, y + 8);
            y = dibujarSeccionCliente(canvas, orden, y + 16);
            y = dibujarSeparador(canvas, y + 8);
            y = dibujarSeccionEquipo(canvas, orden, y + 16);
            y = dibujarSeparador(canvas, y + 8);
            List<ItemOrden> items = orden.getItemsServicio();
            if (items != null && !items.isEmpty()) {
                y = dibujarSeccionServicios(canvas, items, y + 16);
                y = dibujarSeparador(canvas, y + 8);
            }
            y = dibujarTotales(canvas, orden, y + 16);
            dibujarPiePagina(canvas);

            document.finishPage(page);

            // Guardar el archivo
            File dir = new File(context.getExternalFilesDir(null), "pdfs");
            if (!dir.exists()) dir.mkdirs();

            String nombreArchivo = "orden_"
                    + (orden.getNumeroOrden() != null
                    ? orden.getNumeroOrden().replace("-", "_") : orden.getId())
                    + ".pdf";
            File archivo = new File(dir, nombreArchivo);

            FileOutputStream fos = new FileOutputStream(archivo);
            document.writeTo(fos);
            fos.close();

            // Retornar URI compartible via FileProvider
            return FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    archivo);

        } catch (IOException e) {
            return null;
        } finally {
            document.close();
        }
    }

    // ════════════════════════════════════════════════════════
    //  Secciones del PDF
    // ════════════════════════════════════════════════════════

    /**
     * Header: círculo rojo con "T", nombre de empresa, número de orden.
     * @return nueva posición Y después del header
     */
    private static int dibujarHeader(Canvas canvas, Orden orden) {
        int y = MARGIN;

        // Círculo rojo con logo "T"
        Paint circulo = new Paint(Paint.ANTI_ALIAS_FLAG);
        circulo.setColor(COLOR_ROJO);
        circulo.setStyle(Paint.Style.FILL);
        canvas.drawCircle(MARGIN + 24, y + 24, 24, circulo);

        Paint logoTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
        logoTexto.setColor(COLOR_BLANCO);
        logoTexto.setTextSize(28);
        logoTexto.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        logoTexto.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("T", MARGIN + 24, y + 33, logoTexto);

        // Nombre de empresa
        Paint nombreEmpresa = new Paint(Paint.ANTI_ALIAS_FLAG);
        nombreEmpresa.setColor(COLOR_NEGRO);
        nombreEmpresa.setTextSize(16);
        nombreEmpresa.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("TALLER TÉCNICO", MARGIN + 58, y + 18, nombreEmpresa);

        Paint subEmpresa = new Paint(Paint.ANTI_ALIAS_FLAG);
        subEmpresa.setColor(COLOR_GRIS);
        subEmpresa.setTextSize(10);
        canvas.drawText("TecrobSys — Sistema de Gestión", MARGIN + 58, y + 33, subEmpresa);

        // Número de orden (alineado a la derecha)
        Paint numOrden = new Paint(Paint.ANTI_ALIAS_FLAG);
        numOrden.setColor(COLOR_ROJO);
        numOrden.setTextSize(20);
        numOrden.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        numOrden.setTextAlign(Paint.Align.RIGHT);
        String numText = orden.getNumeroOrden() != null
                ? "#" + orden.getNumeroOrden() : "#" + orden.getId();
        canvas.drawText(numText, PAGE_WIDTH - MARGIN, y + 22, numOrden);

        // Estado de la orden (debajo del número)
        Paint estadoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        estadoPaint.setColor(COLOR_GRIS);
        estadoPaint.setTextSize(10);
        estadoPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Estado: " + UtilEstado.obtenerTexto(orden.getEstado()),
                PAGE_WIDTH - MARGIN, y + 38, estadoPaint);

        // Fecha de generación del PDF
        Paint fecha = new Paint(Paint.ANTI_ALIAS_FLAG);
        fecha.setColor(COLOR_GRIS);
        fecha.setTextSize(9);
        fecha.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Generado: " + UtilFecha.obtenerFechaHoy(),
                PAGE_WIDTH - MARGIN, y + 52, fecha);

        return y + 55;
    }

    /** Línea separadora horizontal roja */
    private static int dibujarSeparador(Canvas canvas, int y) {
        Paint linea = new Paint();
        linea.setColor(COLOR_ROJO);
        linea.setStrokeWidth(1.5f);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linea);
        return y;
    }

    /** Encabezado de sección (ej: "CLIENTE") */
    private static int dibujarEncabezadoSeccion(Canvas canvas, String titulo, int y) {
        // Fondo del encabezado
        Paint fondo = new Paint(Paint.ANTI_ALIAS_FLAG);
        fondo.setColor(COLOR_GRIS_CLARO);
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18, fondo);

        Paint texto = new Paint(Paint.ANTI_ALIAS_FLAG);
        texto.setColor(COLOR_GRIS);
        texto.setTextSize(10);
        texto.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(titulo, MARGIN + 6, y + 13, texto);

        return y + 24;
    }

    /** Fila de dato: "Etiqueta    Valor" */
    private static int dibujarFila(Canvas canvas, String etiqueta, String valor, int y) {
        Paint etiquetaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        etiquetaPaint.setColor(COLOR_GRIS);
        etiquetaPaint.setTextSize(10);
        canvas.drawText(etiqueta, MARGIN + 4, y, etiquetaPaint);

        Paint valorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valorPaint.setColor(COLOR_NEGRO);
        valorPaint.setTextSize(10);
        canvas.drawText(valor != null ? valor : "—", MARGIN + 130, y, valorPaint);

        return y + LINE_HEIGHT;
    }

    /** Sección de datos del cliente */
    private static int dibujarSeccionCliente(Canvas canvas, Orden orden, int y) {
        y = dibujarEncabezadoSeccion(canvas, "CLIENTE", y);

        if (orden.getCliente() != null) {
            Orden.ClienteResumen cli = orden.getCliente();
            y = dibujarFila(canvas, "Nombre:", cli.getNombreCompleto(), y);
            y = dibujarFila(canvas, "Teléfono:", cli.getTelefono(), y);
        } else {
            y = dibujarFila(canvas, "Nombre:", "—", y);
        }
        return y;
    }

    /** Sección de datos del equipo */
    private static int dibujarSeccionEquipo(Canvas canvas, Orden orden, int y) {
        y = dibujarEncabezadoSeccion(canvas, "EQUIPO", y);

        if (orden.getEquipo() != null) {
            com.example.tecrobsys.modelos.Equipo eq = orden.getEquipo();
            y = dibujarFila(canvas, "Tipo:", eq.getTipoFormateado(), y);
            y = dibujarFila(canvas, "Marca / Modelo:", eq.getNombreCompleto(), y);
            if (eq.getNumeroSerie() != null && !eq.getNumeroSerie().isEmpty())
                y = dibujarFila(canvas, "Nro. de serie:", eq.getNumeroSerie(), y);
            y = dibujarFila(canvas, "Problema:", eq.getDesperfecto(), y);
            if (eq.getDescripcionGeneral() != null && !eq.getDescripcionGeneral().isEmpty())
                y = dibujarFila(canvas, "Estado general:", eq.getDescripcionGeneral(), y);
            if (eq.getAccesorios() != null && !eq.getAccesorios().isEmpty())
                y = dibujarFila(canvas, "Accesorios:", eq.getAccesorios(), y);
        } else {
            y = dibujarFila(canvas, "Equipo:", "Sin datos", y);
        }
        return y;
    }

    /** Sección de servicios y repuestos aplicados */
    private static int dibujarSeccionServicios(Canvas canvas, List<ItemOrden> items, int y) {
        y = dibujarEncabezadoSeccion(canvas, "SERVICIOS Y REPUESTOS", y);

        for (ItemOrden item : items) {
            if (item.getServicio() == null) continue;
            String nombre = item.getServicio().getNombre();
            if (item.getCantidad() > 1) nombre += " ×" + item.getCantidad();
            String precio = String.format("S/ %.2f", item.getSubtotal());

            // Nombre alineado a la izquierda, precio a la derecha
            Paint nomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            nomPaint.setColor(COLOR_NEGRO);
            nomPaint.setTextSize(10);
            canvas.drawText(nombre, MARGIN + 4, y, nomPaint);

            Paint precioPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            precioPaint.setColor(COLOR_GRIS);
            precioPaint.setTextSize(10);
            precioPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(precio, PAGE_WIDTH - MARGIN - 4, y, precioPaint);

            y += LINE_HEIGHT;
        }
        return y;
    }

    /** Sección de totales con el TOTAL resaltado */
    private static int dibujarTotales(Canvas canvas, Orden orden, int y) {
        y = dibujarEncabezadoSeccion(canvas, "RESUMEN DE COBRO", y);

        y = dibujarFila(canvas, "Subtotal:",  String.format("S/ %.2f", orden.getSubtotal()), y);
        y = dibujarFila(canvas, "Descuento:", String.format("- S/ %.2f", orden.getDescuento()), y);
        y = dibujarFila(canvas, "Adelanto:",  String.format("- S/ %.2f", orden.getAdelanto()), y);

        // Separador antes del total
        Paint sep = new Paint();
        sep.setColor(Color.LTGRAY);
        sep.setStrokeWidth(0.8f);
        canvas.drawLine(MARGIN + 4, y - 4, PAGE_WIDTH - MARGIN - 4, y - 4, sep);

        // TOTAL A COBRAR — resaltado en rojo y negrita
        Paint etiquetaTotal = new Paint(Paint.ANTI_ALIAS_FLAG);
        etiquetaTotal.setColor(COLOR_ROJO_DARK);
        etiquetaTotal.setTextSize(13);
        etiquetaTotal.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("TOTAL A COBRAR:", MARGIN + 4, y + 4, etiquetaTotal);

        Paint valorTotal = new Paint(Paint.ANTI_ALIAS_FLAG);
        valorTotal.setColor(COLOR_ROJO);
        valorTotal.setTextSize(15);
        valorTotal.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(
                String.format("S/ %.2f", orden.getSaldoPendiente()),
                MARGIN + 130, y + 4, valorTotal);

        return y + LINE_HEIGHT + 8;
    }

    /** Pie de página con fecha y mensaje */
    private static void dibujarPiePagina(Canvas canvas) {
        int y = PAGE_HEIGHT - 30;

        // Línea separadora
        Paint linea = new Paint();
        linea.setColor(Color.LTGRAY);
        linea.setStrokeWidth(0.8f);
        canvas.drawLine(MARGIN, y - 10, PAGE_WIDTH - MARGIN, y - 10, linea);

        // Texto de pie
        Paint pie = new Paint(Paint.ANTI_ALIAS_FLAG);
        pie.setColor(COLOR_GRIS);
        pie.setTextSize(9);
        pie.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Gracias por confiar en nosotros • TecrobSys",
                PAGE_WIDTH / 2f, y + 4, pie);

        Paint marca = new Paint(Paint.ANTI_ALIAS_FLAG);
        marca.setColor(Color.LTGRAY);
        marca.setTextSize(8);
        marca.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Documento generado automáticamente",
                PAGE_WIDTH / 2f, y + 16, marca);
    }
}
