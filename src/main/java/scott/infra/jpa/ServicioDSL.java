package scott.infra.jpa;

import io.vavr.Function3;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scott.infra.Resultados.*;
import scott.infra.jpa.entidad.Entidad;
import scott.infra.jpa.entidad.ErrorValidacion;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class ServicioDSL {
    private static final Logger logger = LoggerFactory.getLogger(ServicioDSL.class);

    // nuevaInstanciaEntidad: parámetros nombrados
    protected record Contexto<E extends Entidad>(String nombre, Repositorio<E> repositorio) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    protected <E extends Entidad> Contexto<E> contexto(String nombre, Repositorio<E> repositorio) {
        return new Contexto<>(nombre, repositorio);
    }

    protected <E extends Entidad> Supplier<E> crearInstancia(Supplier<E> crear) {
        return crear;
    }

    protected <E extends Entidad> Function<E, E> guardarInstancia(Function<E, E> guardar) {
        return guardar;
    }

    // nuevaInstanciaEntidad #1: sin clave primaria natural
    protected <E extends Entidad> Either<Falla, Id> nuevaInstanciaEntidad(
            Contexto<E> contexto,
            Supplier<E> creadorInstancia
    ) {
        return nuevaInstanciaEntidad(contexto, creadorInstancia, contexto.repositorio::guardarYGrabar);
    }

    protected <E extends Entidad> Either<Falla, Id> nuevaInstanciaEntidad(
            Contexto<E> contexto,
            Supplier<E> creadorInstancia,
            Function<E, E> persistir
    ) {
        try {
            final E entidad;
            try {
                entidad = creadorInstancia.get();
            } catch (ErrorValidacion errorValidacion) {
                return fallaValidacion(contexto.toString(), errorValidacion);
            }
            final var entidadGuardada = persistir.apply(entidad);
            logger.debug("{}: entidad creada con id '{}'", contexto, entidadGuardada.getId());
            return Either.right(new Id(entidadGuardada.getId()));
        } catch (Exception e) {
            return condicionError("%s: Error de creación".formatted(contexto), e);
        }
    }

    // nuevaInstanciaEntidad #2: con clave primaria natural
    protected <E extends Entidad, T> Function<String, Either<Falla, Void>> detectarDuplicado(
            Function<T, Optional<E>> leer, T t
    ) {
        return (contexto) -> leer.apply(t)
                .map(e -> this.<Void>fallaGeneral("%s. Clave duplicada: %s".formatted(contexto, t)))
                .orElseGet(() -> Either.right(null));
    }

    protected <E extends Entidad, T1, T2> Function<String, Either<Falla, Void>> detectarDuplicado(
            BiFunction<T1, T2, Optional<E>> leer, T1 t1, T2 t2
    ) {
        return (contexto) -> leer.apply(t1, t2)
                .map(e -> this.<Void>fallaGeneral("%s. Clave duplicada: %s/%s".formatted(contexto, t1, t2)))
                .orElseGet(() -> Either.right(null));
    }

    protected <E extends Entidad> Either<Falla, Id> nuevaInstanciaEntidad(
            Contexto<E> contexto,
            Function<String, Either<Falla, Void>> detectarDuplicado,
            Supplier<E> creadorInstancia
    ) {
        return nuevaInstanciaEntidad(contexto, detectarDuplicado, creadorInstancia,
                contexto.repositorio::guardarYGrabar);
    }

    protected <E extends Entidad> Either<Falla, Id> nuevaInstanciaEntidad(
            Contexto<E> contexto,
            Function<String, Either<Falla, Void>> detectarDuplicado,
            Supplier<E> creadorInstancia,
            Function<E, E> persistir
    ) {
        return detectarDuplicado.apply(contexto.toString())
                .flatMap(ignored -> nuevaInstanciaEntidad(contexto, creadorInstancia, persistir));
    }

    // actualizarYPropagar: parámetros nombrados
    protected <E extends Entidad, T> Supplier<Optional<E>> recuperarPor(
            Function<T, Optional<E>> recuperacion, T t
    ) {
        return () -> recuperacion.apply(t);
    }

    protected <E extends Entidad, T1, T2> Supplier<Optional<E>> recuperarPor(
            BiFunction<T1, T2, Optional<E>> recuperacion, T1 t1, T2 t2
    ) {
        return () -> recuperacion.apply(t1, t2);
    }

    protected <E extends Entidad> Consumer<E> actualizarCon(Consumer<E> actualizacion) {
        return actualizacion;
    }

    protected <E extends Entidad> Consumer<E> propagarCon(Consumer<E> propagacion) {
        return propagacion;
    }

    // actualizarYPropagar: implementaciones
    protected <E extends Entidad> Either<Falla, Void> actualizar(
            Contexto<E> contexto,
            Supplier<Optional<E>> recuperar,
            Consumer<E> actualizar
    ) {
        return actualizarYPropagar(contexto, recuperar, actualizar, null);
    }

    protected <E extends Entidad> Either<Falla, Void> actualizarYPropagar(
            Contexto<E> contexto,
            Supplier<Optional<E>> recuperar,
            Consumer<E> actualizar,
            Consumer<E> propagar
    ) {
        try {
            return recuperar.get()
                    .map(entidad -> {

                        try {
                            actualizar.accept(entidad);
                        } catch (IllegalStateException | IllegalArgumentException e) {
                            return this.<Void>fallaGeneral(
                                    "%s. Error de actualización: %s".formatted(contexto, e.getMessage()));
                        }

                        contexto.repositorio.guardarYGrabar(entidad);

                        if (propagar != null) {
                            propagar.accept(entidad);
                        }

                        return Either.<Falla, Void>right(null);
                    })
                    .orElseGet(() -> fallaGeneral("%s: no encontrado".formatted(contexto)));
        } catch (Exception e) {
            return condicionError(
                    "%s. Error inesperado durante actualización: %s".formatted(contexto, e.getMessage()), e);
        }
    }

    // consultar
    protected <E extends Entidad, R> Function<E, R> consultarCon(Function<E, R> accion) {
        return accion;
    }

    protected <E extends Entidad, R> Either<Falla, R> consultar(
            Contexto<E> contexto,
            Supplier<Optional<E>> recuperar,
            Function<E, R> accion
    ) {
        try {
            return recuperar.get()
                    .map(entidad -> {
                        try {
                            return Either.<Falla, R>right(accion.apply(entidad));
                        } catch (IllegalStateException | IllegalArgumentException e) {
                            return this.<R>fallaGeneral(
                                    "%s. Error de actualización: %s".formatted(contexto, e.getMessage()));
                        }
                    })
                    .orElseGet(() -> fallaGeneral("%s: no encontrado".formatted(contexto)));
        } catch (Exception e) {
            return condicionError(
                    "%s. Error inesperado durante actualización: %s".formatted(contexto, e.getMessage()), e);
        }
    }

    // conId
    protected <E extends Entidad, R> Either<Falla, R> conId(Contexto<E> contexto, String id, Function<E, R> accion) {
        try {
            return contexto.repositorio.buscarPorId(id)
                    .map(entidad -> Either.<Falla, R>right(accion.apply(entidad)))
                    .orElseGet(() -> fallaGeneral("%s. Id no encontrado: %s".formatted(contexto, id)));
        } catch (Exception e) {
            return condicionError("%s. Error al recuperar id: %s".formatted(contexto, id), e);
        }
    }

    // conIds
    protected String contexto(String nombre) {
        return nombre;
    }

    protected <E extends Entidad> Supplier<Optional<E>> conId(Repositorio<E> repositorio, String id) {
        return () -> {
            if (id == null) return Optional.empty();
            else return repositorio.buscarPorId(id);
        };
    }

    // conIds: implementación
    protected <E1 extends Entidad, E2 extends Entidad, R> Either<Falla, R> conIds(
            String contexto,
            Supplier<Optional<E1>> conId1,
            Supplier<Optional<E2>> conId2,
            BiFunction<E1, E2, R> accion
    ) {
        try {
            return conId1.get()
                    .flatMap(entidad1 -> conId2.get().map(entidad2 -> new Tuple2<>(entidad1, entidad2)))
                    .map(tupla -> Either.<Falla, R>right(accion.apply(tupla._1, tupla._2)))
                    .orElseGet(() -> fallaGeneral("%s. Ids no encontrados".formatted(contexto)));
        } catch (Exception e) {
            return condicionError("%s. Error al recuperar por ids: %s".formatted(contexto, e.getMessage()), e);
        }
    }

    protected <E1 extends Entidad, E2 extends Entidad, R> Either<Falla, R> conIdOptAccion(
            String contexto,
            Supplier<Optional<E1>> conId1,
            Supplier<Optional<E2>> conId2,
            BiFunction<E1, Optional<E2>, Either<Falla, R>> accion
    ) {
        try {
            return conId1.get()
                    .map(e1 -> accion.apply(e1, conId2.get()))
                    .orElseGet(() -> fallaGeneral("%s. Id no encontrado".formatted(contexto)));
        } catch (Exception e) {
            return condicionError("%s. Error al recuperar por ids: %s".formatted(contexto, e.getMessage()), e);
        }
    }

    protected <E1 extends Entidad, E2 extends Entidad, R> Either<Falla, R> conIdsAccion(
            String contexto,
            Supplier<Optional<E1>> conId1,
            Supplier<Optional<E2>> conId2,
            BiFunction<E1, E2, Either<Falla, R>> accion
    ) {
        try {
            return conId1.get()
                    .flatMap(entidad1 -> conId2.get().map(entidad2 -> new Tuple2<>(entidad1, entidad2)))
                    .map(tupla -> accion.apply(tupla._1, tupla._2))
                    .orElseGet(() -> fallaGeneral("%s. Ids no encontrados".formatted(contexto)));
        } catch (Exception e) {
            return condicionError("%s. Error al recuperar por ids: %s".formatted(contexto, e.getMessage()), e);
        }
    }

    protected <E1 extends Entidad, E2 extends Entidad, E3 extends Entidad, R> Either<Falla, R> conIds(
            String contexto,
            Supplier<Optional<E1>> conId1,
            Supplier<Optional<E2>> conId2,
            Supplier<Optional<E3>> conId3,
            Function3<E1, E2, E3, R> accion
    ) {
        try {
            return conId1.get()
                    .flatMap(entidad1 -> conId2.get().map(entidad2 -> new Tuple2<>(entidad1, entidad2)))
                    .flatMap(tupla2 -> conId3.get().map(tupla2::append))
                    .map(tupla3 -> Either.<Falla, R>right(accion.apply(tupla3._1, tupla3._2, tupla3._3)))
                    .orElseGet(() -> fallaGeneral("%s. Ids no encontrados".formatted(contexto)));
        } catch (Exception e) {
            return condicionError("%s. Error recuperando por ids: %s".formatted(contexto, e.getMessage()), e);
        }
    }

    // Métodos generales de soporte
    protected <T> Either<Falla, T> fallaGeneral(String mensajeError) {
        logger.warn(mensajeError);
        return Either.left(new FallaGeneral(mensajeError));
    }

    protected <T> Either<Falla, T> fallaValidacion(String contexto, ErrorValidacion errorValidacion) {
        final var mensajeError = "%s. Falla de validación: %s".formatted(contexto, errorValidacion.getMessage());
        logger.warn(mensajeError, errorValidacion);
        return Either.left(new FallaValidacion(mensajeError, errorValidacion));
    }

    protected <T> Either<Falla, T> condicionError(String contexto, Exception e) {
        final var mensajeError = "%s: %s".formatted(contexto, e.getMessage());
        logger.error(mensajeError, e);
        return Either.left(new CondicionError(mensajeError, e));
    }
}

