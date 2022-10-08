# Scott: Un Ejemplo de DSL Funcional en Java

Este repositorio ilustra el diseño, implementación y uso de un lenguaje de dominio específico (DSL) en Java 17
empleando patrones funcionales.

El argumento de estudio es una aplicación SpringBoot JPA inspirada en el tradicional esquema _scott/tiger_ popularizado
por Oracle desde sus inicios.

El DSL implementado en este repositorio captura patrones repetitivos en el uso de repositorios JPA desde componentes 
Spring con estereotipo servicio (`@Service`).

Un servicio Spring típico implementaría imperativamente la persistencia de una nueva instancia de `Departamento` en 
la base de datos como:

```java
// Retorna el id generado para una nueva instancia de departamento persistida exitosamente
// o causa una excepción en cada posible escenario de falla
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
        throw new RuntimeException("Error persistiendo nuevo departamento", e);
    }

    // Retorna id generado para nuevo departamento
    return departamentoGuardado.getId();
}
```

Empleando el DSL implementado en este repositorio, la misma funcionalidad luciría como:

```java
// Retorna el id generado para una nueva instancia de departamento persistida exitosamente
// o un objeto de falla que contiene información de qué problema ocurrió al intentar persistir
public Either<Falla, Id> crearDepartamento(String codigo, String nombre, String localidad) {
    return nuevaInstanciaEntidad(
            contexto("Crear departamento " + nombre, repositorioDepartamento),
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


