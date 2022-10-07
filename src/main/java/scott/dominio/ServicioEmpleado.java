package scott.dominio;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import scott.infra.Resultados.Falla;
import scott.infra.Resultados.Id;
import scott.infra.jpa.ServicioDSL;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ServicioEmpleado {
    Either<Falla, Id> crearEmpleado(String codigo,
                                    String nombre,
                                    Genero genero,
                                    String cargo,
                                    String idSupervisor,
                                    LocalDate fechaContratacion,
                                    BigDecimal salario,
                                    BigDecimal comision,
                                    String idDepartamento);

    Either<Falla, Void> reasignar(String idEmpleado,
                                  String idDepartamento,
                                  String cargo,
                                  String idSupervisor,
                                  BigDecimal salario,
                                  BigDecimal comision);

    @Service
    class Impl extends ServicioDSL implements ServicioEmpleado {
        @Override
        public Either<Falla, Id> crearEmpleado(String codigo,
                                               String nombre,
                                               Genero genero,
                                               String cargo,
                                               String idSupervisor,
                                               LocalDate fechaContratacion,
                                               BigDecimal salario,
                                               BigDecimal comision,
                                               String idDepartamento) {
            return conIdOptAccion(
                    contexto("Crear empleado %s".formatted(nombre)),
                    conId(repositorioDepartamento, idDepartamento),
                    conId(repositorioEmpleado, idSupervisor),
                    (departamento, supervisor) -> nuevaInstanciaEntidad(
                            contexto("Crear empleado %s en %s".formatted(nombre, departamento.getNombre()),
                                    repositorioEmpleado),
                            detectarDuplicado(repositorioEmpleado::buscarPorCodigo, codigo),
                            crearInstancia(() ->
                                    Empleado.builder()
                                            .codigo(codigo)
                                            .nombre(nombre)
                                            .genero(genero)
                                            .cargo(cargo)
                                            .supervisor(supervisor.orElse(null))
                                            .fechaContratacion(fechaContratacion)
                                            .salario(salario)
                                            .comision(comision)
                                            .departamento(departamento)
                                            .build()
                            )));
        }

        @Override
        public Either<Falla, Void> reasignar(String idEmpleado,
                                             String idDepartamento,
                                             String cargo,
                                             String idSupervisor,
                                             BigDecimal salario,
                                             BigDecimal comision) {
            return conIdsAccion(
                    contexto("Reasignar empleado %s".formatted(idEmpleado)),
                    conId(repositorioDepartamento, idDepartamento),
                    conId(repositorioEmpleado, idSupervisor),
                    (departamento, supervisor) -> actualizar(
                            contexto("Reasignar empleado %s".formatted(idEmpleado), repositorioEmpleado),
                            recuperarPor(repositorioEmpleado::buscarPorId, idEmpleado),
                            actualizarCon(empleado ->
                                    empleado.reasignar(
                                            departamento,
                                            cargo,
                                            supervisor,
                                            salario,
                                            comision))));
        }

        private final RepositorioEmpleado repositorioEmpleado;
        private final RepositorioDepartamento repositorioDepartamento;

        public Impl(RepositorioEmpleado repositorioEmpleado, RepositorioDepartamento repositorioDepartamento) {
            this.repositorioEmpleado = repositorioEmpleado;
            this.repositorioDepartamento = repositorioDepartamento;
        }
    }
}
