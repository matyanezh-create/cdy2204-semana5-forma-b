# Entradas Cloud Semana 5 Forma B

Sistema de registro de eventos y venta de entradas. La entrega reutiliza los microservicios de la semana 4 y centraliza sus 12 rutas en Amazon API Gateway. Cognito emite un access token OAuth 2.0. El Gateway valida el JWT y el scope. Spring Security vuelve a validar la firma, el emisor, la vigencia, el cliente, el tipo de token y el scope en ambos servicios. Compras reenvía el token a Eventos para consultar el precio vigente antes de guardar la transacción.

```text
Postman ← OAuth 2.0 + PKCE → Cognito
Postman → API Gateway → VPC Link → ALB interno → Eventos / Compras
                                              Compras → Eventos
                                              Compras → S3
```

## Ejecutar pruebas

Con Java 17 y Maven 3.9, ejecuta `./mvnw.cmd clean verify` en Windows o `sh ./mvnw clean verify` en Linux. Prueba los dos servicios, tanto las operaciones de negocio como los estados de autorización 401 y 403 con decodificador simulado.

## Desplegar

### Identidad

Despliega `infra/identity.json` por CloudFormation en la región del laboratorio. Usa un prefijo de dominio libre. En el user pool crea un usuario de demostración y completa el cambio de contraseña si Cognito lo solicita. Las salidas del stack incluyen `Issuer`, `ClientId`, `Scope` y `HostedDomain`. Si reutilizas el pool de la semana 4, crea o confirma el resource server `resumen`, el scope `acceso` y un cliente público con flujo Authorization Code, Cognito como proveedor y el callback de Postman.

```bash
aws cloudformation deploy --template-file infra/identity.json --stack-name entradas-s5-identidad --parameter-overrides DomainPrefix=REEMPLAZAR
```

### Microservicios

En ambos servicios configura `AUTH_ISSUER` con `Issuer` y `AUTH_CLIENT_ID` con `ClientId`. El scope predeterminado es `resumen/acceso`. En Compras añade `EVENTOS_BASE_URL` con la URL interna de Eventos, `RESUMEN_BUCKET` y `RESUMEN_REGION`. El bucket debe ser privado. Usa el perfil de instancia para S3 y adapta `infra/s3-permissions.example.json` con el nombre del bucket. Habilita la salida HTTPS de ambos servicios hacia el JWKS del emisor.

Para una VM del laboratorio con Docker, crea el archivo local `.env` con dichas variables y ejecuta `docker compose -f compose.local.yml up -d --build`. Compose resuelve Eventos como `http://eventos:8081` preservando la comunicación interna. El perfil local guarda los datos H2 en volúmenes. Para reutilizar PostgreSQL u Oracle, usa los perfiles y variables de conexión ya presentes en cada módulo. Mantén los grupos de seguridad de la VM restringidos al ALB en 8081 y 8082.

Usa dos listeners del ALB interno para los dos target groups. Los destinos son los puertos 8081 y 8082. Los chequeos de salud usan `/actuator/health`. Reutiliza el VPC Link de la semana 4 o créalo con conectividad al ALB. En la plantilla, los ARN son de los listeners y el valor VpcLinkId es el identificador del VPC Link.

### API Gateway

```bash
aws cloudformation deploy --template-file infra/gateway.json --stack-name entradas-s5-gateway \
  --parameter-overrides Issuer=EMISOR Audience=CLIENTE VpcLinkId=VPC_LINK \
  EventosListenerArn=ARN_LISTENER_EVENTOS ComprasListenerArn=ARN_LISTENER_COMPRAS
```

Configura `baseUrl` en Postman con la salida `BaseUrl`, incluida la etapa `/demo`. El Gateway exige JWT y `resumen/acceso` en sus 12 rutas. La plantilla configura límites de solicitudes y registros de acceso en CloudWatch. Las integraciones privadas conservan la ruta original al retirar el prefijo de la etapa.

## Configurar Postman

1. Importa `postman/Semana5.postman_collection.json`. En Authorization configura OAuth 2.0 con el flujo **Authorization Code (With PKCE)**, SHA-256 y **Authorize using browser**.
2. Callback `https://oauth.pstmn.io/v1/browser-callback`. Usa `HostedDomain/oauth2/authorize` y `HostedDomain/oauth2/token` como extremos. Ingresa el `ClientId`, deja Client Secret vacío, envía las credenciales del cliente en el cuerpo y solicita `openid resumen/acceso`.
3. Inicia sesión y guarda el **access token** en la variable privada `accessToken`. Para las pruebas de 403 solicita otro token del mismo cliente con solo `openid` y guárdalo en `tokenSinScope`. Conserva ambos tokens fuera del repositorio y evita mostrarlos en el video.
4. Para las pruebas directas usa un túnel SSH autorizado o una sesión de port forwarding hacia la VM. En el caso de una sola VM, reenvía los puertos remotos 8081 y 8082 a los mismos puertos locales. Los endpoints directos están definidos en la colección como `eventosDirecto` y `comprasDirecto`.
5. Ejecuta la colección en orden. La creación de evento y compra guarda automáticamente los identificadores para las llamadas siguientes.

## Inventario de rutas

| Método | Ruta | Éxito |
|---|---|---:|
| GET | /api/eventos | 200 |
| POST | /api/eventos | 201 |
| GET | /api/eventos/{id} | 200 |
| PUT | /api/eventos/{id} | 200 |
| DELETE | /api/eventos/{id} | 204 |
| POST | /api/compras | 201 |
| GET | /api/compras/{id} | 200 |
| GET | /api/compras/{id}/resumen/generar | 200 |
| POST | /api/compras/{id}/resumen | 201 |
| GET | /api/compras/{id}/resumen | 200 |
| PUT | /api/compras/{id}/resumen | 200 |
| DELETE | /api/compras/{id}/resumen | 204 |

La clave de cada resumen en S3 es `{id}/resumen.txt`. Al borrar el resumen la compra se conserva. La eliminación de un evento cambia su estado y lo deja fuera de las consultas activas.

Para la grabación y los enlaces del AVA, usa [GUIÓN TEAMS](GUION_TEAMS.md).

Referencias: [Autorizador JWT de API Gateway](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-jwt-authorizer.html) y [Resource Server JWT de Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
