package scott.infra;

import scott.infra.jpa.entidad.ErrorValidacion;
import scott.infra.validacion.ValorInvalido;

import java.util.List;

public interface Resultados {

    interface Falla {
        String mensaje();

        Throwable error();
    }

    record FallaGeneral(String mensaje, Throwable error) implements Falla {
        public FallaGeneral(String mensaje) {
            this(mensaje, null);
        }
    }

    record FallaValidacion(String contexto, List<ValorInvalido> erroresValidacion, String mensaje) implements Falla {
        public FallaValidacion(String contexto, ErrorValidacion errorValidacion) {
            this(contexto,
                    errorValidacion.valoresInvalidos(),
                    "%s: %s".formatted(contexto, errorValidacion.getMessage()));
        }

        @Override
        public Throwable error() {
            // TODO Retornar error de validación original
            return null;
        }
    }

    record CondicionError(String contexto, Throwable error) implements Falla {

        @Override
        public String mensaje() {
            return "Error %s: %s".formatted(
                    contexto(),
                    error().getMessage() == null ? error().toString() : error().getMessage()
            );
        }
    }
}
