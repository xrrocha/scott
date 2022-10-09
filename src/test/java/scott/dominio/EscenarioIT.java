package scott.dominio;

import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import scott.PruebaIntegracion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

        final var empleados =
                List.ofAll(repositorioEmpleado.findAll())
                        .toMap(Empleado::getId, Function.identity());

        assertEquals(
                HashSet.of(idKing, idJones, idBlake, idAllen),
                empleados.keySet()
        );

        final var allenAntes = empleados.get(idAllen).get();
        assertEquals("Vendedor", allenAntes.getCargo());
        assertEquals(idBlake, allenAntes.getSupervisor().getId());
        assertEquals(new BigDecimal(8000), allenAntes.getSalario());
        assertEquals(new BigDecimal(1500), allenAntes.getComision());

        servicioEmpleado.reasignar(
                idAllen,
                idContabilidad,
                "Oficinista",
                idKing,
                new BigDecimal(5000),
                null
        );

        final var allenDespues = repositorioEmpleado.findById(idAllen).get();
        assertEquals("Oficinista", allenDespues.getCargo());
        assertEquals(idKing, allenDespues.getSupervisor().getId());
        assertEquals(new BigDecimal(5000), allenDespues.getSalario());
        assertNull(allenDespues.getComision());
    }

    private final RepositorioDepartamento repositorioDepartamento;
    private final RepositorioEmpleado repositorioEmpleado;
    private final ServicioDepartamento servicioDepartamento;
    private final ServicioEmpleado servicioEmpleado;

    @Autowired
    public EscenarioIT(RepositorioDepartamento repositorioDepartamento,
                       RepositorioEmpleado repositorioEmpleado,
                       ServicioDepartamento servicioDepartamento,
                       ServicioEmpleado servicioEmpleado) {
        this.repositorioDepartamento = repositorioDepartamento;
        this.repositorioEmpleado = repositorioEmpleado;
        this.servicioDepartamento = servicioDepartamento;
        this.servicioEmpleado = servicioEmpleado;
    }
}
