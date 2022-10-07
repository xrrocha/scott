# Scott: Un Ejemplo de DSL Funcional en Java

Este repositorio ilustra el diseño, implementación y uso de un lenguaje de dominio específico (DSL) en Java 17
empleando patrones funcionales.

El argumento de estudio es una aplicación SpringBoot JPA inspirada en el tradicional esquema _scott/tiger_ popularizado
por Oracle desde los años 80.

El [DSL](src/main/java/scott/infra/jpa/ServicioDSL.java) implementado en este repositorio captura patrones repetitivos 
en el uso de repositorios JPA desde componentes Spring con estereotipo servicio (`@Service`).

Empleando este DSL la persistencia de una nueva instancia de `Departamento` en la base de datos luciría como:

```java
public Either<Falla, Id> crearDepartamento(String codigo, String nombre, String localidad) {
    return nuevaInstanciaEntidad(
            contexto("Crear departamento %s".formatted(nombre), repositorioDepartamento),
            detectarDuplicado(repositorioDepartamento::buscarPorCodigo, codigo),
            crearInstancia(() ->
                    Departamento.builder()
                            .codigo(codigo)
                            .nombre(nombre)
                            .localidad(localidad)
                            .build()
            ));
}
```

Un servicio Spring típico, en cambio, implementaría esta misma funcionalidad de forma imperativa como:

```java
public String crearDepartamento(String codigo, String nombre, String localidad) {
    // Valida que el código de departamento no sea duplicado
    final Optional<Departamento> optDepartamento;
    try {
        optDepartamento = repositorioDepartamento.buscarPorCodigo(codigo);
    } catch (Exception e) {
        throw new RuntimeException("Error recuperando departamento por código", e);
    }
    optDepartamento.ifPresent(d -> {
        String mensaje = "Ya existe un departamento con codigo %s: %s!".formatted(codigo, d.getNombre());
        throw new IllegalArgumentException(mensaje);
    });

    // Construye y valida instancia de departamento
    final Departamento departamento;
    try {
        departamento = Departamento.builder()
                .codigo(codigo)
                .nombre(nombre)
                .localidad(localidad)
                .build();
    } catch (Exception e) {
        throw new RuntimeException("Error de validación creando departamento", e);
    }

    // Persiste nuevo departamento
    final Departamento departamentoGuardado;
    try {
        departamentoGuardado = repositorioDepartamento.guardar(departamento);
    } catch (Exception e) {
        throw new RuntimeException("Error persistiendo nuevo departamento en base de datos", e);
    }

    // Retorna id generado para nuevo departamento
    return departamentoGuardado.getId();
}
```


