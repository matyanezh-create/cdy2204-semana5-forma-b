# Demostración de Semana 5 Forma B

En una reunión de Teams comparte la ventana de AWS y Postman. Antes de grabar, confirma que el stage está desplegado, los targets están sanos y el usuario de prueba inicia sesión. Usa los resultados obtenidos en vivo y evita mostrar tokens y contraseñas.

| Bloque | Acción en vivo | Evidencia |
|---|---|---|
| 1. API Gateway | Abrir la HTTP API, el stage demo y las integraciones privadas. Mostrar las 12 rutas y su autorización. | Nombres de rutas, autorizador JWT, límites y registros de acceso. |
| 2. IDaaS | Abrir Cognito y mostrar el user pool, cliente, callback, flujo code y scope. Obtener el access token por Authorization Code con PKCE en Postman. | El login redirige al callback y Postman recibe el token. |
| 3. Integración | Ejecutar las solicitudes al Gateway sin token, con token inválido y con token válido sin scope. | 401, 401 y 403 respectivamente. |
| 4. Eventos y ventas | Crear, listar, consultar y actualizar el evento. Comprar dos entradas y consultar el total. | Evento creado y actualizado. Precio unitario de 15.000 CLP y total de 30.000 CLP. |
| 5. Resúmenes | Generar el TXT, guardarlo en S3, probar un duplicado, descargar, corregir, volver a descargar, borrar y volver a consultar. Consultar la compra tras el borrado. | TXT descargable, 409 por duplicado, observación corregida, 404 del resumen borrado y 200 de la compra. |
| 6. Spring Security | Usar las solicitudes directas por túnel para Eventos y Compras. Demostrar rechazo sin token y éxito con scope en Eventos. Demostrar 403 directo en Compras sin el scope. | La seguridad de los servicios funciona antes de pasar por la lógica del controlador, incluso en llamadas directas. |
| 7. Cierre | Desactivar el evento y repetir el intento de compra. Mostrar un registro del Gateway en CloudWatch. | 204 al desactivar, 404 al intentar comprar y registro con ruta y estado. |

Frase de cierre sugerida: «El Gateway valida el acceso al sistema y cada microservicio vuelve a comprobar el JWT. Compras conserva el precio aplicado al momento de la venta y guarda el resumen en S3.»

## Enlaces para entregar en el AVA

- Integrantes → completar.
- Repositorio Git → pegar la URL del repositorio con permisos de lectura para el docente.
- Video de Teams → pegar la URL de la grabación con permisos de reproducción para el docente.
- Archivo comprimido → adjuntar el ZIP del proyecto.
