package scott.dominio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import scott.PruebaIntegracion;

import java.math.BigDecimal;
import java.time.LocalDate;

import static scott.dominio.Genero.FEMENINO;
import static scott.dominio.Genero.MASCULINO;

@Transactional
@SpringBootTest
public class EscenarioIT extends PruebaIntegracion {

    @Test
    public void pueblaDatos() {

        final var idContabilidad =
                servicioDepartamento.crearDepartamento(
                                "10",
                                "Contabilidad",
                                "Quito")
                        .get();
        final var idInvestigacion =
                servicioDepartamento.crearDepartamento(
                                "20",
                                "Investigación",
                                "Sunrise")
                        .get();
        final var idVentas =
                servicioDepartamento.crearDepartamento(
                                "30",
                                "Ventas",
                                "Bogota")
                        .get();

        final var idKing = servicioEmpleado.crearEmpleado(
                        "7839",
                        "King",
                        FEMENINO,
                        "Presidente",
                        null,
                        LocalDate.of(2011, 11, 17),
                        new BigDecimal(15000),
                        null,
                        idContabilidad)
                .get();

        final var idJones = servicioEmpleado.crearEmpleado(
                        "7566",
                        "Jones",
                        MASCULINO,
                        "Gerente",
                        idKing,
                        LocalDate.of(2011, 4, 2),
                        new BigDecimal(14875),
                        null,
                        idInvestigacion)
                .get();

        final var idBlake = servicioEmpleado.crearEmpleado(
                        "7698",
                        "Blake",
                        MASCULINO,
                        "Gerente",
                        idKing,
                        LocalDate.of(2011, 1, 1),
                        new BigDecimal(14250),
                        null,
                        idVentas)
                .get();

        final var idAllen = servicioEmpleado.crearEmpleado(
                        "7499",
                        "Allen",
                        MASCULINO,
                        "Vendedor",
                        idBlake,
                        LocalDate.of(2011, 2, 20),
                        new BigDecimal(8000),
                        new BigDecimal(1500),
                        idVentas)
                .get();
    }

    private final ServicioDepartamento servicioDepartamento;
    private final ServicioEmpleado servicioEmpleado;

    @Autowired
    public EscenarioIT(ServicioDepartamento servicioDepartamento, ServicioEmpleado servicioEmpleado) {
        this.servicioDepartamento = servicioDepartamento;
        this.servicioEmpleado = servicioEmpleado;
    }
}
