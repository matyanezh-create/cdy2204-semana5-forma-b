package cl.duoc.compras;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ResumenApiTest {
    @Autowired MockMvc mvc;
    @Autowired CompraRepository compras;
    @MockitoBean ResumenStorage storage;
    @MockitoBean EventoClient eventos;

    @Test
    void cicloDelArchivoParaUnaCompraPersistida() throws Exception {
        var evento = new EventoClient.EventoDetalle(12L, "Festival", OffsetDateTime.now().plusDays(2),
                new BigDecimal("15000"), true);
        var compra = compras.save(new Compra(evento, 2));
        String ruta = "/api/compras/" + compra.id + "/resumen";
        String clave = compra.id + "/resumen.txt";
        var objeto = new AtomicReference<byte[]>();

        doAnswer(call -> {
            if (!objeto.compareAndSet(null, call.getArgument(1))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicado");
            }
            return null;
        }).when(storage).crear(eq(clave), any(byte[].class));
        when(storage.leer(clave)).thenAnswer(call -> {
            if (objeto.get() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo ausente");
            }
            return objeto.get();
        });
        doAnswer(call -> {
            objeto.set(call.getArgument(1));
            return null;
        }).when(storage).reemplazar(eq(clave), any(byte[].class));
        doAnswer(call -> {
            objeto.set(null);
            return null;
        }).when(storage).borrar(clave);

        mvc.perform(get(ruta + "/generar"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("Festival")));
        verifyNoInteractions(storage);
        mvc.perform(post(ruta)).andExpect(status().isCreated()).andExpect(jsonPath("$.clave").value(clave));
        mvc.perform(post(ruta)).andExpect(status().isConflict());
        mvc.perform(get(ruta)).andExpect(status().isOk()).andExpect(content().string(containsString("30000")));
        mvc.perform(put(ruta).contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacion\":\"Acceso norte\"}")).andExpect(status().isOk());
        mvc.perform(get(ruta)).andExpect(status().isOk())
                .andExpect(content().encoding(StandardCharsets.UTF_8))
                .andExpect(content().string(containsString("Observación: Acceso norte")));
        mvc.perform(delete(ruta)).andExpect(status().isNoContent());
        mvc.perform(get(ruta)).andExpect(status().isNotFound());
        mvc.perform(get("/api/compras/" + compra.id)).andExpect(status().isOk());
    }
}


