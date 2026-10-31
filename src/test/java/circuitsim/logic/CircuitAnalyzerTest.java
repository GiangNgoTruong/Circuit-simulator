package circuitsim.logic;

import circuitsim.model.*;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/** Known MNA solutions isolate analysis from the independently tested solver. */
class CircuitAnalyzerTest {
    private static final double EPS = 1e-9;
    private static final int GROUND = 0;
    private static final int NODE = 7;
    private static final double AC = 1000.0 / (2 * Math.PI); // omega = 1000 rad/s

    private static CircuitSolution.MnaResult result(Complex... unknowns) {
        return new CircuitSolution.MnaResult(unknowns, Map.of());
    }

    private static CircuitAnalyzer analyzer(Map<Double, CircuitSolution.MnaResult> results) {
        return new CircuitAnalyzer(new CircuitSolution(Map.of(NODE, 0), results, GROUND));
    }

    private static CircuitAnalyzer at(double frequency, Complex voltage) {
        return analyzer(Map.of(frequency, result(voltage)));
    }

    private static Edge load(Component component) {
        return new Edge(NODE, GROUND, component);
    }

    private static void assertComplex(double real, double imaginary, Complex actual) {
        assertNotNull(actual);
        assertTrue(actual.isFinite(), "Expected a finite result");
        assertEquals(real, actual.real(), EPS, "Real part");
        assertEquals(imaginary, actual.imag(), EPS, "Imaginary part");
    }

    @Test
    void resolvesArbitraryNodeIndicesAndNonzeroGroundId() {
        CircuitAnalyzer analyzer = new CircuitAnalyzer(new CircuitSolution(
                Map.of(20, 1, 70, 0), Map.of(50.0, result(new Complex(3, -4), new Complex(9, 2))), 99));
        assertComplex(9, 2, analyzer.getNodeVoltage(20, 50));
        assertComplex(3, -4, analyzer.getNodeVoltage(70, 50));
        assertComplex(0, 0, analyzer.getNodeVoltage(99, 50));
        assertComplex(6, 6, analyzer.getVoltage(new Edge(20, 70, new Resistor("R", 2)), 50));
    }

    @Test
    void rejectsUnknownNode() {
        CircuitAnalyzer analyzer = at(0, new Complex(10));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> analyzer.getNodeVoltage(999, 0));
        assertEquals("Unknown node: 999", error.getMessage());
    }

    @Test
    void rejectsUnsolvedFrequencyIncludingForGround() {
        CircuitAnalyzer analyzer = at(0, new Complex(10));
        Edge resistor = load(new Resistor("R", 5));
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> analyzer.getNodeVoltage(NODE, 50)),
                () -> assertThrows(IllegalArgumentException.class, () -> analyzer.getNodeVoltage(GROUND, 50)),
                () -> assertThrows(IllegalArgumentException.class, () -> analyzer.getVoltage(resistor, 50)),
                () -> assertThrows(IllegalArgumentException.class, () -> analyzer.getCurrent(resistor, 50)));
    }

    @Test
    void reportsExactlyTheSolvedFrequencies() {
        CircuitAnalyzer analyzer = analyzer(Map.of(0.0, result(new Complex(6)),
                50.0, result(new Complex(0, 8)), 150.0, result(new Complex(6, 8))));
        assertEquals(Set.of(0.0, 50.0, 150.0), analyzer.getFrequencies());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void edgeDirectionControlsVoltageAndResistorCurrentSigns(boolean reverse) {
        CircuitAnalyzer analyzer = at(0, new Complex(10));
        Edge edge = reverse ? new Edge(GROUND, NODE, new Resistor("R", 5)) : load(new Resistor("R", 5));
        assertComplex(reverse ? -10 : 10, 0, analyzer.getVoltage(edge, 0));
        assertComplex(reverse ? -2 : 2, 0, analyzer.getCurrent(edge, 0));
        assertEquals(20, analyzer.getAveragePower(edge), EPS, "Reversing both references preserves absorbed power");
    }

    @Test
    void computesComplexResistorCurrentWithoutLosingPhase() {
        CircuitAnalyzer analyzer = at(50, new Complex(6, 8));
        Edge resistor = load(new Resistor("R", 2));
        assertComplex(3, 4, analyzer.getCurrent(resistor, 50));
        assertEquals(25, analyzer.getAveragePower(resistor), EPS,
                "Power uses conjugate current and peak-to-RMS factor 1/2");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void capacitorCurrentLeadsVoltageByNinetyDegrees(boolean reverse) {
        CircuitAnalyzer analyzer = at(AC, new Complex(10));
        Capacitor capacitor = new Capacitor("C", 100e-6);
        Edge edge = reverse ? new Edge(GROUND, NODE, capacitor) : load(capacitor);
        // I=j*omega*C*V = j A.
        assertComplex(0, reverse ? -1 : 1, analyzer.getCurrent(edge, AC));
        assertEquals(1 / Math.sqrt(2), analyzer.getCurrentRMS(edge), EPS);
        assertEquals(0, analyzer.getAveragePower(edge), EPS);
    }

    @Test
    void capacitorUsesFullComplexVoltage() {
        CircuitAnalyzer analyzer = at(AC, new Complex(6, 8));
        assertComplex(-0.8, 0.6, analyzer.getCurrent(load(new Capacitor("C", 100e-6)), AC));
    }

    @Test
    void dcCapacitorHasZeroCurrentDespiteNonzeroVoltage() {
        CircuitAnalyzer analyzer = at(0, new Complex(12));
        Edge capacitor = load(new Capacitor("C", 1e-6));
        assertComplex(12, 0, analyzer.getVoltage(capacitor, 0));
        assertComplex(0, 0, analyzer.getCurrent(capacitor, 0));
        assertEquals(0, analyzer.getCurrentRMS(capacitor), EPS);
        assertEquals(0, analyzer.getAveragePower(capacitor), EPS);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void acInductorCurrentLagsVoltageByNinetyDegrees(boolean reverse) {
        CircuitAnalyzer analyzer = at(AC, new Complex(10));
        Inductor inductor = new Inductor("L", 0.01);
        Edge edge = reverse ? new Edge(GROUND, NODE, inductor) : load(inductor);
        // I=V/(j*omega*L)=-j A. No MNA current index is needed for AC inductors.
        assertComplex(0, reverse ? 1 : -1, analyzer.getCurrent(edge, AC));
        assertEquals(1 / Math.sqrt(2), analyzer.getCurrentRMS(edge), EPS);
        assertEquals(0, analyzer.getAveragePower(edge), EPS);
    }

    @Test
    void inductorUsesFullComplexVoltage() {
        CircuitAnalyzer analyzer = at(AC, new Complex(6, 8));
        assertComplex(0.8, -0.6, analyzer.getCurrent(load(new Inductor("L", 0.01)), AC));
    }

    @Test
    void dcInductorReadsMnaCurrentInsteadOfDividingByZeroImpedance() {
        Edge inductor = load(new Inductor("L", 0.01));
        CircuitAnalyzer analyzer = analyzer(Map.of(0.0, new CircuitSolution.MnaResult(
                new Complex[]{new Complex(0), new Complex(999), new Complex(-3)}, Map.of(inductor, 2))));
        assertComplex(0, 0, analyzer.getVoltage(inductor, 0));
        assertComplex(-3, 0, analyzer.getCurrent(inductor, 0));
        assertEquals(3, analyzer.getCurrentRMS(inductor), EPS);
        assertEquals(0, analyzer.getAveragePower(inductor), EPS);
    }

    @Test
    void voltageSourceReadsItsOwnMnaCurrentIndex() {
        Edge first = load(new VoltageSource("V1")), second = load(new VoltageSource("V2"));
        CircuitAnalyzer analyzer = analyzer(Map.of(50.0, new CircuitSolution.MnaResult(
                new Complex[]{new Complex(10), new Complex(2, -3), new Complex(-4, 5)},
                Map.of(first, 2, second, 1))));
        assertComplex(-4, 5, analyzer.getCurrent(first, 50));
        assertComplex(2, -3, analyzer.getCurrent(second, 50));
    }

    @Test
    void mnaCurrentIndicesCanChangeBetweenDcAndAc() {
        Edge source = load(new VoltageSource("V"));
        Edge inductor = load(new Inductor("L", 0.01));
        CircuitAnalyzer analyzer = analyzer(Map.of(
                0.0, new CircuitSolution.MnaResult(new Complex[]{new Complex(0), new Complex(3), new Complex(-3)},
                        Map.of(inductor, 1, source, 2)),
                AC, new CircuitSolution.MnaResult(new Complex[]{new Complex(10), new Complex(0, 1)}, Map.of(source, 1))));
        assertComplex(-3, 0, analyzer.getCurrent(source, 0));
        assertComplex(3, 0, analyzer.getCurrent(inductor, 0));
        assertComplex(0, 1, analyzer.getCurrent(source, AC));
        assertComplex(0, -1, analyzer.getCurrent(inductor, AC));
    }

    @Test
    void rejectsMissingMnaCurrentForSourceAndDcInductor() {
        CircuitAnalyzer analyzer = at(0, new Complex(0));
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> analyzer.getCurrent(load(new VoltageSource("V")), 0)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> analyzer.getCurrent(load(new Inductor("L", 0.01)), 0)));
    }

    @Test
    void rejectsUnknownNodeOnEitherEndOfAnEdge() {
        CircuitAnalyzer analyzer = at(0, new Complex(10));
        Edge unknownFrom = new Edge(999, GROUND, new Resistor("R1", 5));
        Edge unknownTo = new Edge(NODE, 999, new Resistor("R2", 5));
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> analyzer.getVoltage(unknownFrom, 0)),
                () -> assertThrows(IllegalArgumentException.class, () -> analyzer.getVoltage(unknownTo, 0)),
                () -> assertThrows(IllegalArgumentException.class, () -> analyzer.getCurrent(unknownTo, 0)));
    }

    @Test
    void rejectsUnsupportedComponentCurrent() {
        CircuitAnalyzer analyzer = at(0, new Complex(10));
        Edge unsupported = load(new Component("unknown") {});
        assertThrows(IllegalArgumentException.class, () -> analyzer.getCurrent(unsupported, 0));
    }

    @Test
    void dcRmsUsesAbsoluteValueWithoutAcScaling() {
        CircuitAnalyzer analyzer = at(0, new Complex(-12));
        Edge resistor = load(new Resistor("R", 4));
        assertEquals(12, analyzer.getVoltageRMS(resistor), EPS);
        assertEquals(3, analyzer.getCurrentRMS(resistor), EPS);
        assertEquals(36, analyzer.getAveragePower(resistor), EPS);
    }

    @Test
    void acRmsConvertsPeakMagnitudeToRms() {
        CircuitAnalyzer analyzer = at(50, new Complex(6, 8));
        Edge resistor = load(new Resistor("R", 2));
        assertEquals(10 / Math.sqrt(2), analyzer.getVoltageRMS(resistor), EPS);
        assertEquals(5 / Math.sqrt(2), analyzer.getCurrentRMS(resistor), EPS);
        assertEquals(25, analyzer.getAveragePower(resistor), EPS);
    }

    @Test
    void mixedDcAndAcFrequenciesCombineBySumOfSquaresNotPhasorSum() {
        CircuitAnalyzer analyzer = analyzer(Map.of(0.0, result(new Complex(6)),
                50.0, result(new Complex(0, 8)), 150.0, result(new Complex(6, 8))));
        Edge resistor = load(new Resistor("R", 2));
        // Vrms² = 6² + 8²/2 + 10²/2 = 118; P = Vrms²/R = 59 W.
        assertEquals(Math.sqrt(118), analyzer.getVoltageRMS(resistor), EPS);
        assertEquals(Math.sqrt(118) / 2, analyzer.getCurrentRMS(resistor), EPS);
        assertEquals(59, analyzer.getAveragePower(resistor), EPS);
        assertEquals(Math.pow(analyzer.getCurrentRMS(resistor), 2) * 2,
                analyzer.getAveragePower(resistor), EPS);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 50.0})
    void sourceSuppliesExactlyThePowerAbsorbedByItsResistor(double frequency) {
        Edge source = load(new VoltageSource("V")), resistor = load(new Resistor("R", 5));
        CircuitAnalyzer analyzer = analyzer(Map.of(frequency, new CircuitSolution.MnaResult(
                new Complex[]{new Complex(10), new Complex(-2)}, Map.of(source, 1))));
        double expected = frequency == 0 ? 20 : 10;
        assertEquals(expected, analyzer.getAveragePower(resistor), EPS);
        assertEquals(-expected, analyzer.getAveragePower(source), EPS);
        assertEquals(0, analyzer.getAveragePower(source) + analyzer.getAveragePower(resistor), EPS);
    }

    @Test
    void phaseShiftedReactiveBranchesExchangeNoAveragePowerAcrossFrequencies() {
        CircuitAnalyzer analyzer = analyzer(Map.of(50.0, result(new Complex(6, 8)),
                150.0, result(new Complex(-4, 3))));
        assertEquals(0, analyzer.getAveragePower(load(new Capacitor("C", 1e-6))), EPS);
        assertEquals(0, analyzer.getAveragePower(load(new Inductor("L", 0.01))), EPS);
    }

    @Test
    void zeroSolutionHasZeroRmsAndPower() {
        CircuitAnalyzer analyzer = analyzer(Map.of(0.0, result(Complex.ZERO), 50.0, result(Complex.ZERO)));
        Edge resistor = load(new Resistor("R", 5));
        assertEquals(0, analyzer.getVoltageRMS(resistor), EPS);
        assertEquals(0, analyzer.getCurrentRMS(resistor), EPS);
        assertEquals(0, analyzer.getAveragePower(resistor), EPS);
    }

    @Test
    void emptyFrequencySetHasZeroRmsAndPower() {
        CircuitAnalyzer analyzer = analyzer(Map.of());
        Edge resistor = load(new Resistor("R", 5));
        assertTrue(analyzer.getFrequencies().isEmpty());
        assertEquals(0, analyzer.getVoltageRMS(resistor), EPS);
        assertEquals(0, analyzer.getCurrentRMS(resistor), EPS);
        assertEquals(0, analyzer.getAveragePower(resistor), EPS);
    }
}
