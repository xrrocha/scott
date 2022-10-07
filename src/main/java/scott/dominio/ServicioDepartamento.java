package scott.dominio;

import io.vavr.control.Either;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import scott.infra.Resultados.Falla;
import scott.infra.Resultados.Id;
import scott.infra.jpa.ServicioDSL;

public interface ServicioDepartamento {
    Either<Falla, Id> crearDepartamento(String codigo,
                                        String nombre,
                                        String localidad);

    Either<Falla, Void> relocalizar(String idDepartamento, String nuevaLocalidad);

    @Service
    class Impl extends ServicioDSL implements ServicioDepartamento {
        @Override
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

        @Override
        public Either<Falla, Void> relocalizar(String idDepartamento, String nuevaLocalidad) {
            return actualizarYPropagar(
                    contexto("Relocalizar departamento %s a %s".formatted(idDepartamento, nuevaLocalidad),
                            repositorioDepartamento),
                    recuperarPor(repositorioDepartamento::buscarPorId, idDepartamento),
                    actualizarCon(departamento -> departamento.relocalizar(nuevaLocalidad)),
                    propagarCon(departamento ->
                            applicationEventPublisher.publishEvent(new RelocalizacionDepartamento(departamento))
                    ));
        }

        private final RepositorioDepartamento repositorioDepartamento;
        private final ApplicationEventPublisher applicationEventPublisher;

        @Autowired
        public Impl(RepositorioDepartamento repositorioDepartamento,
                    ApplicationEventPublisher applicationEventPublisher) {
            this.repositorioDepartamento = repositorioDepartamento;
            this.applicationEventPublisher = applicationEventPublisher;
        }
    }

    // TODO: Pasar vieja localizacion
    record RelocalizacionDepartamento(Departamento departamento) {
    }
}

