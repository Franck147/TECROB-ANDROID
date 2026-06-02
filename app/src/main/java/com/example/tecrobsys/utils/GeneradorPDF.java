package com.example.tecrobsys.utils;

import android.app.Activity;
import android.graphics.Picture;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.content.FileProvider;
import com.example.tecrobsys.modelos.Equipo;
import com.example.tecrobsys.modelos.ItemOrden;
import com.example.tecrobsys.modelos.Orden;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GeneradorPDF {

    public interface Callback {
        void onDone(Uri uri);
    }

    private static final int A4_W  = 794;
    private static final int A4_H  = 1123;
    // Renderizar a 2× resolución para texto nítido; se escala al dibujar en el PDF
    private static final int SCALE = 2;

    /** Genera el PDF de forma asíncrona. Debe llamarse desde el hilo principal. */
    public static void generarOrden(Activity activity, Orden orden, Callback callback) {
        WebView webView = new WebView(activity);
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        WebSettings cfg = webView.getSettings();
        cfg.setJavaScriptEnabled(false);
        cfg.setDefaultTextEncodingName("UTF-8");
        cfg.setUseWideViewPort(true);
        cfg.setLoadWithOverviewMode(true);

        int renderW = A4_W * SCALE;
        int renderH = A4_H * SCALE;
        int wSpec = View.MeasureSpec.makeMeasureSpec(renderW, View.MeasureSpec.EXACTLY);
        int hSpec = View.MeasureSpec.makeMeasureSpec(renderH, View.MeasureSpec.EXACTLY);
        webView.measure(wSpec, hSpec);
        webView.layout(0, 0, renderW, renderH);

        File dir = new File(activity.getExternalFilesDir(null), "pdfs");
        if (!dir.exists()) dir.mkdirs();
        String nombre = "orden_" + (orden.getNumeroOrden() != null
                ? orden.getNumeroOrden().replace("-", "_")
                : String.valueOf(orden.getId())) + ".pdf";
        File archivo = new File(dir, nombre);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    view.measure(wSpec, hSpec);
                    view.layout(0, 0, renderW, renderH);

                    PdfDocument doc = new PdfDocument();
                    PdfDocument.PageInfo info =
                            new PdfDocument.PageInfo.Builder(A4_W, A4_H, 1).create();
                    PdfDocument.Page page = doc.startPage(info);

                    // Escalar el canvas a 1/SCALE para que el render 2× quede en tamaño A4
                    page.getCanvas().scale(1.0f / SCALE, 1.0f / SCALE);
                    Picture picture = view.capturePicture();
                    picture.draw(page.getCanvas());
                    doc.finishPage(page);

                    try (FileOutputStream fos = new FileOutputStream(archivo)) {
                        doc.writeTo(fos);
                        doc.close();
                        Uri uri = FileProvider.getUriForFile(
                                activity,
                                activity.getPackageName() + ".fileprovider",
                                archivo);
                        webView.destroy();
                        callback.onDone(uri);
                    } catch (IOException e) {
                        doc.close();
                        webView.destroy();
                        callback.onDone(null);
                    }
                }, 600);
            }
        });

        webView.loadDataWithBaseURL(null,
                construirHtml(orden), "text/html", "UTF-8", null);
    }

    // ════════════════════════════════════════════════════════════════
    //  HTML
    // ════════════════════════════════════════════════════════════════

    private static String construirHtml(Orden orden) {
        String numOrden = orden.getNumeroOrden() != null
                ? "#" + orden.getNumeroOrden() : "#" + orden.getId();
        String fecha = UtilFecha.formatearFechaCorta(orden.getCreadoEn());

        return css()
                + "<body>"
                + encabezado(numOrden, fecha)
                + seccionCliente(orden)
                + seccionEquipo(orden)
                + seccionServicio(orden)
                + seccionFirma(orden)
                + terminos()
                + "</body></html>";
    }

    // ════════════════════════════════════════════════════════════════
    //  CSS — fondo blanco, texto negro, líneas grises sutiles
    // ════════════════════════════════════════════════════════════════

    private static String css() {
        return "<!DOCTYPE html><html><head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=794'>"
                + "<style>"

                + "* { box-sizing: border-box; margin: 0; padding: 0; }"
                + "body {"
                + "  font-family: Arial, Helvetica, sans-serif;"
                + "  font-size: 10px;"
                + "  color: #111;"
                + "  background: #fff;"
                + "  width: 794px;"
                + "  padding: 36px 44px 28px;"
                + "}"

                // ── Encabezado ──
                + ".hdr { width: 100%; border-collapse: collapse;"
                + "  padding-bottom: 14px; border-bottom: 1.5px solid #111; }"
                + ".hdr td { vertical-align: middle; padding: 0; }"
                + ".logo {"
                + "  display: inline-block; width: 38px; height: 38px;"
                + "  background: #111; border-radius: 5px;"
                + "  color: #fff; font-size: 19px; font-weight: bold;"
                + "  text-align: center; line-height: 38px; vertical-align: middle;"
                + "}"
                + ".emp { display: inline-block; vertical-align: middle; padding-left: 11px; }"
                + ".emp-nom { font-size: 12.5px; font-weight: bold; color: #111; display: block; line-height: 1.3; }"
                + ".emp-sub { font-size: 7.5px; color: #999; display: block; margin-top: 2px; }"
                + ".hdr-r { text-align: right; vertical-align: top !important; }"
                + ".ord-label { font-size: 7px; color: #bbb; letter-spacing: 1.5px; text-transform: uppercase; display: block; }"
                + ".ord-num { font-size: 18px; font-weight: bold; color: #111; display: block; line-height: 1.2; margin-top: 2px; }"
                + ".ord-date { font-size: 8.5px; color: #777; display: block; margin-top: 3px; }"

                // ── Secciones ──
                + ".sec { padding: 14px 0 12px; border-bottom: 1px solid #e8e8e8; }"
                + ".sec-title {"
                + "  font-size: 7.5px; font-weight: bold; color: #aaa;"
                + "  letter-spacing: 1.5px; text-transform: uppercase;"
                + "  margin-bottom: 10px;"
                + "}"

                // ── Grilla de campos ──
                + ".fgrid { width: 100%; border-collapse: collapse; }"
                + ".fgrid td { vertical-align: top; padding: 0 10px 6px 0; }"
                + ".fgrid td:last-child { padding-right: 0; }"
                + ".fl { font-size: 7.5px; color: #999; display: block; margin-bottom: 2px; }"
                + ".fv { font-size: 10px; color: #111; font-weight: bold; }"

                // ── Tabla de servicios ──
                + ".srv { width: 100%; border-collapse: collapse; margin-top: 2px; }"
                + ".srv thead th {"
                + "  font-size: 7.5px; color: #999; font-weight: normal; text-align: left;"
                + "  padding: 0 0 6px 0; border-bottom: 1px solid #ddd;"
                + "  letter-spacing: 0.5px; text-transform: uppercase;"
                + "}"
                + ".srv thead th.r { text-align: right; }"
                + ".srv tbody td {"
                + "  font-size: 10px; color: #111;"
                + "  padding: 7px 0;"
                + "  border-bottom: 1px solid #f2f2f2;"
                + "  vertical-align: middle;"
                + "}"
                + ".srv tbody td.c { text-align: center; color: #666; width: 50px; }"
                + ".srv tbody td.r { text-align: right; font-weight: bold; width: 85px; white-space: nowrap; }"
                + ".srv tbody td.empty { color: #ccc; font-style: italic; }"

                // ── Totales ──
                + ".totals { overflow: hidden; margin-top: 12px; }"
                + ".tot-blk { float: right; min-width: 220px; }"
                + ".tot { width: 100%; border-collapse: collapse; }"
                + ".tot td { padding: 3px 0; font-size: 10px; }"
                + ".tot .tl { text-align: right; color: #999; padding-right: 20px; }"
                + ".tot .tv { text-align: right; font-weight: bold; white-space: nowrap; min-width: 74px; }"
                + ".tot .sep td { border-top: 1px solid #ddd; padding-top: 7px; }"
                + ".tot .final .tl { font-size: 11px; font-weight: bold; color: #111; }"
                + ".tot .final .tv { font-size: 14px; font-weight: bold; color: #111; }"

                // ── Firma ──
                + ".firma { padding: 14px 0 10px; border-bottom: 1px solid #e8e8e8; }"
                + ".firma-row { width: 100%; border-collapse: collapse; margin-top: 10px; }"
                + ".firma-row td { padding: 0; vertical-align: bottom; font-size: 8.5px; }"
                + ".f-lbl { color: #888; white-space: nowrap; padding-right: 6px; display: block; margin-bottom: 4px; }"
                + ".f-lin { border-bottom: 1px solid #bbb; height: 22px; }"

                // ── Términos ──
                + ".terms { padding-top: 11px; font-size: 7px; color: #bbb; line-height: 1.6; }"
                + ".terms b { font-weight: bold; letter-spacing: 1px; text-transform: uppercase; }"

                + "</style></head>";
    }

    // ════════════════════════════════════════════════════════════════
    //  Secciones HTML
    // ════════════════════════════════════════════════════════════════

    private static String encabezado(String numOrden, String fecha) {
        return "<table class='hdr'><tr>"
                + "<td>"
                + "<span class='logo'>T</span>"
                + "<span class='emp'>"
                + "<span class='emp-nom'>MULTISERVICIOS TECROB SYS E.I.R.L.</span>"
                + "<span class='emp-sub'>Servicio T&eacute;cnico Especializado &bull; RPC: 000-000-000</span>"
                + "<span class='emp-sub'>tecrobsys@gmail.com</span>"
                + "</span>"
                + "</td>"
                + "<td class='hdr-r'>"
                + "<span class='ord-label'>Orden de Servicio</span>"
                + "<span class='ord-num'>" + h(numOrden) + "</span>"
                + "<span class='ord-date'>" + h(fecha) + "</span>"
                + "</td>"
                + "</tr></table>";
    }

    private static String seccionCliente(Orden orden) {
        String nombre = "&mdash;", dni = "&mdash;", tel = "&mdash;", tec = "&mdash;";
        if (orden.getCliente() != null) {
            Orden.ClienteResumen c = orden.getCliente();
            nombre = h(c.getNombreCompleto());
            if (c.getDni() != null && !c.getDni().isEmpty()) dni = h(c.getDni());
            if (c.getTelefono() != null && !c.getTelefono().isEmpty()) tel = h(c.getTelefono());
        }
        if (orden.getTecnico() != null) tec = h(orden.getTecnico().getNombreCompleto());

        return "<div class='sec'>"
                + "<div class='sec-title'>Datos del Cliente</div>"
                + "<table class='fgrid'><tr>"
                + fd("Nombre completo", nombre, "44%")
                + fd("DNI", dni, "16%")
                + fd("Tel&eacute;fono", tel, "20%")
                + fd("T&eacute;cnico asignado", tec, "20%")
                + "</tr></table>"
                + "</div>";
    }

    private static String seccionEquipo(Orden orden) {
        Equipo eq = orden.getEquipo();
        if (eq == null) {
            return "<div class='sec'>"
                    + "<div class='sec-title'>Datos del Equipo</div>"
                    + "<span style='color:#ccc;font-style:italic;font-size:10px;'>Sin equipo registrado</span>"
                    + "</div>";
        }
        String tipo        = h(eq.getTipoFormateado());
        String marcaModelo = h(eq.getNombreCompleto());
        String serie       = (eq.getNumeroSerie() != null && !eq.getNumeroSerie().isEmpty())
                ? h(eq.getNumeroSerie()) : "&mdash;";
        String accesorios  = (eq.getAccesorios() != null && !eq.getAccesorios().isEmpty())
                ? h(eq.getAccesorios()) : "&mdash;";
        String problema    = (eq.getDesperfecto() != null && !eq.getDesperfecto().isEmpty())
                ? h(eq.getDesperfecto()) : "&mdash;";

        return "<div class='sec'>"
                + "<div class='sec-title'>Datos del Equipo</div>"
                + "<table class='fgrid'>"
                + "<tr>"
                + fd("Tipo", tipo, "14%")
                + fd("Marca / Modelo", marcaModelo, "28%")
                + fd("N&uacute;mero de serie", serie, "22%")
                + fd("Accesorios entregados", accesorios, "36%")
                + "</tr>"
                + "<tr>"
                + "<td colspan='4' style='padding-top:4px;padding-bottom:0;'>"
                + "<span class='fl'>Problema / Desperfecto reportado</span>"
                + "<span class='fv'>" + problema + "</span>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "</div>";
    }

    private static String seccionServicio(Orden orden) {
        List<ItemOrden> items = orden.getItemsServicio();
        StringBuilder filas = new StringBuilder();

        if (items != null && !items.isEmpty()) {
            for (ItemOrden item : items) {
                if (item.getServicio() == null) continue;
                filas.append("<tr>")
                     .append("<td>").append(h(item.getServicio().getNombre())).append("</td>")
                     .append("<td class='c'>").append(item.getCantidad()).append("</td>")
                     .append("<td class='r'>").append(sf(item.getPrecioUnitario())).append("</td>")
                     .append("<td class='r'>").append(sf(item.getSubtotal())).append("</td>")
                     .append("</tr>");
            }
        } else {
            filas.append("<tr><td colspan='4' class='empty'>Sin servicios registrados</td></tr>");
        }

        return "<div class='sec'>"
                + "<div class='sec-title'>Detalle del Servicio</div>"
                + "<table class='srv'>"
                + "<thead><tr>"
                + "<th>Descripci&oacute;n</th>"
                + "<th class='r' style='width:50px;text-align:center;'>Cant.</th>"
                + "<th class='r' style='width:85px;'>P. Unitario</th>"
                + "<th class='r' style='width:85px;'>Subtotal</th>"
                + "</tr></thead>"
                + "<tbody>" + filas + "</tbody>"
                + "</table>"
                + tablaTotal(orden)
                + "</div>";
    }

    private static String tablaTotal(Orden orden) {
        return "<div class='totals'>"
                + "<div class='tot-blk'>"
                + "<table class='tot'>"
                + "<tr><td class='tl'>Subtotal</td>"
                +     "<td class='tv'>" + sf(orden.getSubtotal()) + "</td></tr>"
                + "<tr><td class='tl'>Descuento</td>"
                +     "<td class='tv'>&minus;&nbsp;" + sf(orden.getDescuento()) + "</td></tr>"
                + "<tr><td class='tl'>Adelanto</td>"
                +     "<td class='tv'>&minus;&nbsp;" + sf(orden.getAdelanto()) + "</td></tr>"
                + "<tr class='sep'><td></td><td></td></tr>"
                + "<tr class='final'>"
                +     "<td class='tl'>TOTAL A COBRAR</td>"
                +     "<td class='tv'>" + sf(orden.getSaldoPendiente()) + "</td>"
                + "</tr>"
                + "</table>"
                + "</div>"
                + "</div>";
    }

    private static String seccionFirma(Orden orden) {
        String tecNombre    = orden.getTecnico() != null
                ? h(orden.getTecnico().getNombreCompleto()) : "";
        String fechaIngreso = h(UtilFecha.formatearFechaCorta(orden.getCreadoEn()));
        String fechaProm    = (orden.getFechaPrometida() != null
                && !orden.getFechaPrometida().isEmpty())
                ? h(UtilFecha.formatearFechaCorta(orden.getFechaPrometida())) : "&mdash;";

        return "<div class='firma'>"
                + "<div class='sec-title'>Entrega del Equipo</div>"
                + "<table class='fgrid'><tr>"
                + fd("Fecha de ingreso", fechaIngreso, "25%")
                + fd("Fecha prometida de entrega", fechaProm, "30%")
                + "<td style='width:45%;'></td>"
                + "</tr></table>"
                + "<table class='firma-row' style='margin-top:16px;'><tr>"
                + "<td style='width:36%;'>"
                +   "<span class='f-lbl'>Firma del cliente</span>"
                +   "<div class='f-lin'></div>"
                + "</td>"
                + "<td style='width:3%;'></td>"
                + "<td style='width:19%;'>"
                +   "<span class='f-lbl'>DNI</span>"
                +   "<div class='f-lin'></div>"
                + "</td>"
                + "<td style='width:3%;'></td>"
                + "<td style='width:39%;'>"
                +   "<span class='f-lbl'>Fecha de entrega</span>"
                +   "<div class='f-lin'></div>"
                + "</td>"
                + "</tr></table>"
                + "<table class='firma-row' style='margin-top:12px;'><tr>"
                + "<td style='width:46%;'>"
                +   "<span class='f-lbl'>T&eacute;cnico responsable</span>"
                +   "<div class='f-lin'>" + tecNombre + "</div>"
                + "</td>"
                + "<td style='width:4%;'></td>"
                + "<td style='width:50%;'>"
                +   "<span class='f-lbl'>Firma del t&eacute;cnico</span>"
                +   "<div class='f-lin'></div>"
                + "</td>"
                + "</tr></table>"
                + "</div>";
    }

    private static String terminos() {
        return "<div class='terms'>"
                + "<b>T&Eacute;RMINOS Y CONDICIONES</b>&nbsp;&mdash;&nbsp;"
                + "Los equipos no retirados en 60 d&iacute;as desde la fecha prometida ser&aacute;n considerados en abandono. "
                + "TecrobSys no se responsabiliza por la p&eacute;rdida de informaci&oacute;n almacenada en el equipo. "
                + "Garant&iacute;a de 30 d&iacute;as sobre los trabajos realizados desde la fecha de entrega. "
                + "Conserve este comprobante para el retiro de su equipo."
                + "</div>";
    }

    // ════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════

    private static String fd(String label, String valor, String width) {
        return "<td style='width:" + width + ";'>"
                + "<span class='fl'>" + label + "</span>"
                + "<span class='fv'>" + valor + "</span>"
                + "</td>";
    }

    private static String sf(double monto) {
        return String.format(Locale.getDefault(), "S/&nbsp;%.2f", monto);
    }

    private static String h(String text) {
        if (text == null || text.isEmpty()) return "&mdash;";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
