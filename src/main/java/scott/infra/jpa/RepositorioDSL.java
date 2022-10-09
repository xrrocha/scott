package scott.infra.jpa;

import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedRunnable;
import io.vavr.control.Either;
import org.springframework.data.jpa.repository.JpaRepository;
import scott.infra.Resultados.CondicionError;
import scott.infra.Resultados.Falla;
import scott.infra.Resultados.FallaGeneral;
import scott.infra.jpa.entidad.Entidad;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RepositorioDSL {

    public static Either<Falla, Void> ejecutar(CheckedRunnable action) {
        try {
            action.run();
            return Either.right(null);
        } catch (ExcepcionDSL e) {
            return Either.left(new FallaGeneral(e.getMessage()));
        } catch (Throwable e) {
            return Either.left(new CondicionError("Error inesperado en método de servicio", e));
        }
    }

    public static <R> Either<Falla, R> responderCon(CheckedFunction0<R> action) {
        try {
            return Either.right(action.apply());
        } catch (ExcepcionDSL e) {
            return Either.left(new FallaGeneral(e.getMessage()));
        } catch (Throwable e) {
            return Either.left(new CondicionError("Error inesperado en método de servicio", e));
        }
    }

    public static <E, I, R> E leer(JpaRepository<E, I> repositorio, I id) {
        return Optional.ofNullable(id)
                .flatMap(repositorio::findById)
                .orElseThrow(() -> new ExcepcionDSL("Id inexistente: %s".formatted(id)));
    }

    public static <E, I, R> E leerOpcional(JpaRepository<E, I> repositorio, I id) {
        if (id == null) return null;
        else return repositorio.findById(id)
                .orElseThrow(() -> new ExcepcionDSL("Id inexistente: %s".formatted(id)));
    }

    public static <E, C>
    Consumer<E> detectarDuplicado(Function<C, Optional<E>> extractor, C valorClave) {
        return e -> extractor.apply(valorClave).ifPresent(t -> {
            throw new ExcepcionDSL("Ya existe una instancia con la misma clave: %s".formatted(valorClave));
        });
    }

    public static <T> Either<Falla, T> eitherCatch(String contexto, CheckedFunction0<T> supplier) {
        try {
            return Either.right(supplier.apply());
        } catch (Exception e) {
            return Either.left(new FallaGeneral("Error " + contexto, e));
        } catch (Throwable t) {
            return Either.left(new CondicionError(contexto, t));
        }
    }

    public static <T> Either<Falla, T> eitherCatch(String contexto, T value, CheckedConsumer<T> consumer) {
        try {
            if (consumer != null) {
                consumer.accept(value);
            }
            return Either.right(value);
        } catch (Exception e) {
            return Either.left(new FallaGeneral("Error " + contexto, e));
        } catch (Throwable t) {
            return Either.left(new CondicionError(contexto, t));
        }
    }

    public static <E, I> Either<Falla, I> persistirInstancia(
            JpaRepository<E, I> repositorio,
            CheckedFunction1<E, I> clavePrimaria,
            CheckedConsumer<E> validacion,
            CheckedFunction0<E> crearInstancia
    ) {
        return eitherCatch("creando instancia de entidad en memoria", crearInstancia)
                .flatMap(entidad ->
                        eitherCatch("validando instancia de entidad en memoria", entidad, validacion))
                .flatMap(entidad ->
                        eitherCatch("persistiendo nueva instancia", () -> repositorio.save(entidad)))
                .flatMap(entidad ->
                        eitherCatch("recuperando clave primaria", () -> clavePrimaria.apply(entidad)));
    }

    public static <E, I> I persistirInstancia(
            JpaRepository<E, I> repositorio,
            Function<E, I> clavePrimaria,
            Consumer<E> validacion,
            Supplier<E> crearInstancia
    ) {
        final E entidad;
        try {
            entidad = crearInstancia.get();
        } catch (Exception e) {
            throw new ExcepcionDSL("Error creando instancia de entidad en memoria", e);
        }

        if (validacion != null) {
            try {
                validacion.accept(entidad);
            } catch (Exception e) {
                throw new ExcepcionDSL("Error de validación de entidad", e);
            }
        }

        final E entidadGuardada;
        try {
            entidadGuardada = repositorio.save(entidad);
        } catch (Exception e) {
            throw new ExcepcionDSL("Error persistiendo nueva instancia", e);
        }

        return clavePrimaria.apply(entidadGuardada);
    }

    // TODO Implementar actualizar mediante Either
    public static <E, I, R> void actualizar(
            I id,
            JpaRepository<E, I> repositorio,
            Consumer<E> actualizar
    ) {
        try {
            repositorio.findById(id)
                    .map(entidad -> {
                        try {
                            actualizar.accept(entidad);
                        } catch (Exception e) {
                            throw new ExcepcionDSL("%s. Error de actualización: %s".formatted(id, e.getMessage()));
                        }

                        repositorio.saveAndFlush(entidad);
                        return 0;
                    })
                    .orElseThrow(() -> new ExcepcionDSL("%s: no encontrado".formatted(id)));
        } catch (Exception e) {
            throw new RuntimeException(
                    "%s. Error inesperado durante actualización: %s".formatted(id, e.getMessage()), e);
        }
    }

    // TODO Implementar actualizarRetornando mediante Either
    public static <E, I, R> R actualizarRetornando(
            I id,
            JpaRepository<E, I> repositorio,
            Function<E, R> actualizar
    ) {
        try {
            return repositorio.findById(id)
                    .map(entidad -> {

                        final R resultado;
                        try {
                            resultado = actualizar.apply(entidad);
                        } catch (Exception e) {
                            throw new ExcepcionDSL("%s. Error de actualización: %s".formatted(id, e.getMessage()));
                        }

                        repositorio.saveAndFlush(entidad);

                        return resultado;
                    })
                    .orElseThrow(() -> new ExcepcionDSL("%s: no encontrado".formatted(id)));
        } catch (Exception e) {
            throw new RuntimeException(
                    "%s. Error inesperado durante actualización: %s".formatted(id, e.getMessage()), e);
        }
    }

    public static <E, I> I persistirInstancia(
            JpaRepository<E, I> repositorio,
            Function<E, I> clavePrimaria,
            Supplier<E> crearInstancia
    ) {
        return persistirInstancia(repositorio, clavePrimaria, null, crearInstancia);
    }

    public static <E extends Entidad, R extends JpaRepository<E, String>>
    String persistirInstancia(
            R repositorio,
            Consumer<E> validacion,
            Supplier<E> crearInstancia
    ) {
        return persistirInstancia(repositorio, Entidad::getId, validacion, crearInstancia);
    }

    public static <E extends Entidad, R extends JpaRepository<E, String>>
    String persistirInstancia(R repositorio, Supplier<E> crearInstancia) {
        return persistirInstancia(repositorio, Entidad::getId, null, crearInstancia);
    }

    static class ExcepcionDSL extends RuntimeException {
        public ExcepcionDSL(String mensaje) {
            super(mensaje);
        }

        public ExcepcionDSL(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}

