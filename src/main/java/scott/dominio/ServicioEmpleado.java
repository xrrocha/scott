package scott.dominio;

import io.vavr.control.Either;
import org.springframework.stereotype.Service;
import scott.infra.Resultados.Falla;

import java.math.BigDecimal;
import java.time.LocalDate;

import static scott.infra.jpa.RepositorioDSL.*;

public interface ServicioEmpleado {
    Either<Falla, String> crearEmpleado(String codigo,
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

    // TODO Reimplementar métodos de servicio de empleado con Either
    @Service
    class Impl implements ServicioEmpleado {
        @Override
        public Either<Falla, String> crearEmpleado(String codigo,
                                                   String nombre,
                                                   Genero genero,
                                                   String cargo,
                                                   String idSupervisor,
                                                   LocalDate fechaContratacion,
                                                   BigDecimal salario,
                                                   BigDecimal comision,
                                                   String idDepartamento) {
            return responderCon(() ->
                    persistirInstancia(
                            repositorioEmpleado,
                            detectarDuplicado(repositorioEmpleado::buscarPorCodigo, codigo),
                            () -> Empleado.builder()
                                    .codigo(codigo)
                                    .nombre(nombre)
                                    .genero(genero)
                                    .cargo(cargo)
                                    .supervisor(leerOpcional(repositorioEmpleado, idSupervisor))
                                    .fechaContratacion(fechaContratacion)
                                    .salario(salario)
                                    .comision(comision)
                                    .departamento(leer(repositorioDepartamento, idDepartamento))
                                    .build()
                    ));
        }

        @Override
        public Either<Falla, Void> reasignar(String idEmpleado,
                                             String idDepartamento,
                                             String cargo,
                                             String idSupervisor,
                                             BigDecimal salario,
                                             BigDecimal comision) {
            return ejecutar(() ->
                    actualizar(idEmpleado, repositorioEmpleado,
                            empleado -> empleado.reasignar(
                                    leer(repositorioDepartamento, idDepartamento),
                                    cargo,
                                    leerOpcional(repositorioEmpleado, idSupervisor),
                                    salario,
                                    comision)));
        }

        private final RepositorioEmpleado repositorioEmpleado;
        private final RepositorioDepartamento repositorioDepartamento;

        public Impl(RepositorioEmpleado repositorioEmpleado, RepositorioDepartamento repositorioDepartamento) {
            this.repositorioEmpleado = repositorioEmpleado;
            this.repositorioDepartamento = repositorioDepartamento;
        }
    }
}
