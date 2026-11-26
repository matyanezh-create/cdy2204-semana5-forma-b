package cl.duoc.compras;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compras/{id}/resumen")
public class ResumenController {
    private final CompraService compras;
    private final ResumenStorage storage;

    public ResumenController(CompraService compras, ResumenStorage storage) {
        this.compras = compras;
        this.storage = storage;
    }

    @GetMapping("/generar")
    public ResponseEntity<byte[]> generar(@PathVariable Long id) {
        return archivo(id, contenido(compras.buscar(id), ""));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> subir(@PathVariable Long id) {
        storage.crear(clave(id), contenido(compras.buscar(id), ""));
        return Map.of("clave", clave(id));
    }

    @GetMapping
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        compras.buscar(id);
        return archivo(id, storage.leer(clave(id)));
    }

    @PutMapping
    public Map<String, String> modificar(@PathVariable Long id, @Valid @RequestBody Correccion correccion) {
        byte[] actualizado = contenido(compras.buscar(id), correccion.observacion().strip());
        storage.reemplazar(clave(id), actualizado);
        return Map.of("clave", clave(id));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@PathVariable Long id) {
        compras.buscar(id);
        storage.borrar(clave(id));
    }

    private String clave(Long id) {
        return id + "/resumen.txt";
    }

    private byte[] contenido(Compra compra, String observacion) {
        return """
                RESUMEN DE COMPRA N.º %d
                Evento: %s
                Cantidad: %d
                Precio unitario: %s CLP
                Total: %s CLP
                Fecha: %s
                Observación: %s
                """.formatted(compra.id, compra.evento, compra.cantidad, compra.precioUnitario,
                compra.precioTotal, compra.fechaCompra, observacion).getBytes(StandardCharsets.UTF_8);
    }

    private ResponseEntity<byte[]> archivo(Long id, byte[] contenido) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("resumen-" + id + ".txt").build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(contenido);
    }

    public record Correccion(@NotBlank @Size(max = 500) String observacion) {}
}
