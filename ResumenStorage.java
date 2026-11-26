package cl.duoc.compras;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class ResumenStorage {
    private final String bucket;
    private final S3Client s3;

    public ResumenStorage(@Value("${resumen.bucket:}") String bucket,
                          @Value("${resumen.region:us-east-1}") String region,
                          @Value("${resumen.endpoint:}") String endpoint) {
        this.bucket = bucket;
        var builder = S3Client.builder().region(Region.of(region));
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }
        this.s3 = builder.build();
    }

    public void crear(String clave, byte[] contenido) {
        ejecutar(() -> s3.putObject(p -> p.bucket(bucket).key(clave)
                .ifNoneMatch("*").contentType("text/plain; charset=UTF-8"), RequestBody.fromBytes(contenido)));
    }

    public byte[] leer(String clave) {
        return ejecutar(() -> s3.getObjectAsBytes(p -> p.bucket(bucket).key(clave))).asByteArray();
    }

    public void reemplazar(String clave, byte[] contenido) {
        String etag = etag(clave);
        ejecutar(() -> s3.putObject(p -> p.bucket(bucket).key(clave)
                .ifMatch(etag).contentType("text/plain; charset=UTF-8"), RequestBody.fromBytes(contenido)));
    }

    public void borrar(String clave) {
        String etag = etag(clave);
        ejecutar(() -> s3.deleteObject(p -> p.bucket(bucket).key(clave).ifMatch(etag)));
    }

    private String etag(String clave) {
        return ejecutar(() -> s3.headObject(p -> p.bucket(bucket).key(clave))).eTag();
    }

    private <T> T ejecutar(Supplier<T> operacion) {
        if (bucket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Falta configurar el bucket de resúmenes");
        }
        try {
            return operacion.get();
        } catch (S3Exception e) {
            if (e.statusCode() == 404 && (e.awsErrorDetails() == null || !"NoSuchBucket".equals(e.awsErrorDetails().errorCode()))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resumen no encontrado en S3", e);
            }
            if (e.statusCode() == 409 || e.statusCode() == 412) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El resumen ya existe o cambió durante la operación", e);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "S3 rechazó la operación", e);
        } catch (SdkClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo conectar con S3", e);
        }
    }

    @PreDestroy
    public void cerrar() {
        s3.close();
    }
}

