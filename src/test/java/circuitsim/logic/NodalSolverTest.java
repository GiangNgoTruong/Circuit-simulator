package circuitsim.logic;

import circuitsim.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class NodalSolverTest {
    private static final double EPS = 1e-9;
    private static final double AC_FREQUENCY = 1000.0 / (2 * Math.PI); // omega = 1000 rad/s
    private final NodalSolver solver = new NodalSolver();

    // VoltageSource currently exposes read-only, zero-valued parameters. A fixed
    // test source supplies its public getters without reflection or model API changes.
    private static VoltageSource source(String id, double voltage, double frequency, double phase) {
        return new VoltageSource(id) {
            @Override public double getVoltage() { return voltage; }
            @Override public double getFrequency() { return frequency; }
            @Override public double getPhase() { return phase; }
        };
    }

    private static VoltageSource dc(double voltage) {
        return source("V", voltage, 0, 0);
    }

    private static CircuitGraph groundedGraph() {
        CircuitGraph graph = new CircuitGraph();
        graph.setGround(graph.addNode());
        return graph;
    }

    private static void assertComplex(double real, double imag, Complex actual) {
        assertNotNull(actual);
        assertTrue(actual.isFinite(), "Result must be finite");
        assertEquals(real, actual.real(), EPS, "Real part");
        assertEquals(imag, actual.imag(), EPS, "Imaginary part");
    }

    private static CircuitSolution atFrequency(CircuitSolution solution, double frequency) {
        return new CircuitSolution(solution.getNodeIndex(),
                Map.of(frequency, solution.getResults().get(frequency)), solution.getGround());
    }

    // Resolve unknowns through the indices returned by the solver, never inferred order.
    private static Complex voltage(CircuitGraph graph, CircuitSolution solution, int node) {
        assertEquals(graph.getGround(), solution.getGround());
        if (node == solution.getGround()) return Complex.ZERO;
        Integer index = solution.getNodeIndex().get(node);
        assertNotNull(index, "Node must have a voltage index");
        assertEquals(1, solution.getResults().size(), "Select the frequency before reading voltage");
        return solution.getResults().values().iterator().next().getSolution()[index];
    }

    private static Complex current(CircuitGraph graph, CircuitSolution solution, double frequency, Component branch) {
        var result = solution.getResults().get(frequency);
        assertNotNull(result);
        for (Edge edge : graph.getEdges()) {
            if (edge.getComponent() == branch) {
                Integer index = result.getSourceIndex().get(edge);
                assertNotNull(index, "Branch must have an MNA current index");
                return result.getSolution()[index];
            }
        }
        throw new AssertionError("Branch does not belong to graph");
    }

    @Test
    void rejectsMissingGround() {
        CircuitGraph graph = new CircuitGraph();
        int a = graph.addNode(), b = graph.addNode();
        graph.addEdge(a, b, dc(5));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> solver.solve(graph));
        assertEquals("Ground is not set", error.getMessage());
    }

    @Test
    void rejectsGroundRemovedFromGraph() {
        CircuitGraph graph = groundedGraph();
        graph.addNode();
        graph.removeNode(graph.getGround());
        assertThrows(IllegalArgumentException.class, () -> solver.solve(graph));
    }

    @Test
    void noSourcesReturnsNoFrequencyResults() {
        CircuitGraph graph = groundedGraph();
        int node = graph.addNode();
        graph.addEdge(node, graph.getGround(), new Resistor("R", 10));
        CircuitSolution solution = solver.solve(graph);
        assertTrue(solution.getResults().isEmpty());
        assertEquals(graph.getGround(), solution.getGround());
        assertEquals(Set.of(node), solution.getNodeIndex().keySet());
    }

    @Test
    void idealSourceWithoutLoadHasZeroCurrent() {
        CircuitGraph graph = groundedGraph();
        int node = graph.addNode();
        VoltageSource source = dc(12);
        graph.addEdge(node, graph.getGround(), source);
        CircuitSolution results = solver.solve(graph);
        assertEquals(Set.of(0.0), results.getResults().keySet());
        assertEquals(2, results.getResults().get(0.0).getSolution().length);
        assertComplex(12, 0, voltage(graph, results, node));
        assertComplex(0, 0, current(graph, results, 0, source));
    }

    @Test
    void dcResistorLoadObeysOhmsLawAndSourceCurrentSign() {
        CircuitGraph graph = groundedGraph();
        int node = graph.addNode();
        VoltageSource source = dc(10);
        graph.addEdge(node, graph.getGround(), source);
        graph.addEdge(node, graph.getGround(), new Resistor("R", 5));
        CircuitSolution result = solver.solve(graph);
        assertComplex(10, 0, voltage(graph, result, node));
        // Current is referenced from source.from to source.to; delivering power is negative.
        assertComplex(-2, 0, current(graph, result, 0, source));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void resistorDividerIsIndependentOfPassiveEdgeDirection(boolean reverse) {
        CircuitGraph graph = groundedGraph();
        int supply = graph.addNode(), output = graph.addNode(), ground = graph.getGround();
        VoltageSource source = dc(12);
        graph.addEdge(supply, ground, source);
        graph.addEdge(reverse ? output : supply, reverse ? supply : output, new Resistor("R1", 100));
        graph.addEdge(reverse ? ground : output, reverse ? output : ground, new Resistor("R2", 200));
        CircuitSolution result = solver.solve(graph);
        assertComplex(12, 0, voltage(graph, result, supply));
        assertComplex(8, 0, voltage(graph, result, output));
        assertComplex(-0.04, 0, current(graph, result, 0, source));
    }

    @Test
    void parallelResistorsSumBranchCurrents() {
        CircuitGraph graph = groundedGraph();
        int node = graph.addNode();
        VoltageSource source = dc(12);
        graph.addEdge(node, graph.getGround(), source);
        graph.addEdge(node, graph.getGround(), new Resistor("R1", 6));
        graph.addEdge(node, graph.getGround(), new Resistor("R2", 3));
        CircuitSolution result = solver.solve(graph);
        assertComplex(12, 0, voltage(graph, result, node));
        assertComplex(-6, 0, current(graph, result, 0, source));
    }

    @Test
    void reversingSourcePolarityReversesNodeVoltage() {
        CircuitGraph graph = groundedGraph();
        int node = graph.addNode();
        VoltageSource source = dc(10);
        graph.addEdge(graph.getGround(), node, source);
        graph.addEdge(node, graph.getGround(), new Resistor("R", 5));
        CircuitSolution result = solver.solve(graph);
        assertComplex(-10, 0, voltage(graph, result, node));
        assertComplex(-2, 0, current(graph, result, 0, source));
    }

    @Test
    void sourceBetweenTwoNonGroundNodesFormsSupernode() {
        CircuitGraph graph = groundedGraph();
        int a = graph.addNode(), b = graph.addNode();
        VoltageSource source = dc(9);
        graph.addEdge(a, b, source);
        graph.addEdge(a, graph.getGround(), new Resistor("R1", 3));
        graph.addEdge(b, graph.getGround(), new Resistor("R2", 3));
        CircuitSolution result = solver.solve(graph);
        assertComplex(4.5, 0, voltage(graph, result, a));
        assertComplex(-4.5, 0, voltage(graph, result, b));
        assertComplex(-1.5, 0, current(graph, result, 0, source));
    }

    @Test
    void supportsSparseNodeIdsAndNonFirstGround() {
        CircuitGraph graph = new CircuitGraph();
        int removed = graph.addNode(), node = graph.addNode(), ground = graph.addNode();
        graph.removeNode(removed);
        graph.setGround(ground);
        VoltageSource source = dc(6);
        graph.addEdge(node, ground, new Resistor("R", 3));
        graph.addEdge(node, ground, source);
        CircuitSolution result = solver.solve(graph);
        assertEquals(2, result.getResults().values().iterator().next().getSolution().length);
        assertComplex(6, 0, voltage(graph, result, node));
        assertComplex(-2, 0, current(graph, result, 0, source));
    }

    @Test
    void acSourcePhaseIsInRadians() {
        CircuitGraph graph = groundedGraph();
        int node = graph.addNode();
        VoltageSource source = source("V", 10, 50, Math.PI / 2);
        graph.addEdge(node, graph.getGround(), source);
        graph.addEdge(node, graph.getGround(), new Resistor("R", 5));
        CircuitSolution result = solver.solve(graph);
        assertComplex(0, 10, voltage(graph, result, node));
        assertComplex(0, -2, current(graph, result, 50, source));
    }

    @Test
    void rcLowPassHasExpectedComplexVoltageAtCornerFrequency() {
        CircuitGraph graph = groundedGraph();
        int supply = graph.addNode(), output = graph.addNode();
        VoltageSource source = source("V", 10, AC_FREQUENCY, 0);
        graph.addEdge(supply, graph.getGround(), source);
        graph.addEdge(supply, output, new Resistor("R", 1000));
        graph.addEdge(output, graph.getGround(), new Capacitor("C", 1e-6));
        CircuitSolution result = solver.solve(graph);
        // omega*R*C=1: Vout=10/(1+j)=5-j5.
        assertComplex(5, -5, voltage(graph, result, output));
        assertComplex(-0.005, -0.005, current(graph, result, AC_FREQUENCY, source));
    }

    @Test
    void rlDividerHasExpectedInductivePhase() {
        CircuitGraph graph = groundedGraph();
        int supply = graph.addNode(), output = graph.addNode();
        VoltageSource source = source("V", 10, AC_FREQUENCY, 0);
        graph.addEdge(supply, graph.getGround(), source);
        graph.addEdge(supply, output, new Resistor("R", 10));
        graph.addEdge(output, graph.getGround(), new Inductor("L", 0.01));
        CircuitSolution result = solver.solve(graph);
        // ZL=j10: Vout=10*j10/(10+j10)=5+j5.
        assertEquals(3, result.getResults().values().iterator().next().getSolution().length, "AC inductors must not add a current unknown");
        assertComplex(5, 5, voltage(graph, result, output));
        assertComplex(-0.5, 0.5, current(graph, result, AC_FREQUENCY, source));
    }

    @Test
    void parallelRlcSumsComplexAdmittances() {
        CircuitGraph graph = groundedGraph();
        int node = graph.addNode(), ground = graph.getGround();
        VoltageSource source = source("V", 10, AC_FREQUENCY, 0);
        graph.addEdge(node, ground, source);
        graph.addEdge(node, ground, new Resistor("R", 10));
        graph.addEdge(node, ground, new Capacitor("C", 200e-6));
        graph.addEdge(ground, node, new Inductor("L", 0.01));
        CircuitSolution result = solver.solve(graph);
        // Y=0.1+j0.2-j0.1=0.1+j0.1 S.
        assertComplex(10, 0, voltage(graph, result, node));
        assertComplex(-1, -1, current(graph, result, AC_FREQUENCY, source));
    }

    @Test
    void dcCapacitorIsAnOpenCircuit() {
        CircuitGraph graph = groundedGraph();
        int supply = graph.addNode(), output = graph.addNode();
        VoltageSource source = dc(10);
        graph.addEdge(supply, graph.getGround(), source);
        graph.addEdge(supply, output, new Resistor("R", 10));
        graph.addEdge(output, graph.getGround(), new Capacitor("C", 1e-6));
        CircuitSolution result = solver.solve(graph);
        assertComplex(10, 0, voltage(graph, result, output));
        assertComplex(0, 0, current(graph, result, 0, source));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void dcInductorIsAShortCircuitWithReferencedCurrent(boolean reverse) {
        CircuitGraph graph = groundedGraph();
        int supply = graph.addNode(), output = graph.addNode(), ground = graph.getGround();
        VoltageSource source = dc(10);
        Inductor inductor = new Inductor("L", 0.01);
        graph.addEdge(supply, ground, source);
        graph.addEdge(supply, output, new Resistor("R", 10));
        graph.addEdge(reverse ? ground : output, reverse ? output : ground, inductor);
        CircuitSolution result = solver.solve(graph);
        assertEquals(4, result.getResults().values().iterator().next().getSolution().length, "DC inductor adds an MNA current unknown");
        assertComplex(0, 0, voltage(graph, result, output));
        assertComplex(-1, 0, current(graph, result, 0, source));
        assertComplex(reverse ? -1 : 1, 0, current(graph, result, 0, inductor));
    }

    @Test
    void differentFrequenciesAreSolvedSeparatelyWithInactiveSourcesShorted() {
        CircuitGraph graph = groundedGraph();
        int a = graph.addNode(), b = graph.addNode(), output = graph.addNode(), ground = graph.getGround();
        VoltageSource dc = dc(10), ac = source("AC", 20, 50, 0);
        graph.addEdge(a, ground, dc);
        graph.addEdge(b, ground, ac);
        graph.addEdge(a, output, new Resistor("R1", 10));
        graph.addEdge(b, output, new Resistor("R2", 10));
        graph.addEdge(output, ground, new Resistor("R3", 10));
        CircuitSolution results = solver.solve(graph);
        assertEquals(Set.of(0.0, 50.0), results.getResults().keySet());
        CircuitSolution atDc = atFrequency(results, 0.0), atAc = atFrequency(results, 50.0);
        assertComplex(10, 0, voltage(graph, atDc, a));
        assertComplex(0, 0, voltage(graph, atDc, b));
        assertComplex(10.0 / 3, 0, voltage(graph, atDc, output));
        assertComplex(-2.0 / 3, 0, current(graph, atDc, 0, dc));
        assertComplex(1.0 / 3, 0, current(graph, atDc, 0, ac));
        assertComplex(0, 0, voltage(graph, atAc, a));
        assertComplex(20, 0, voltage(graph, atAc, b));
        assertComplex(20.0 / 3, 0, voltage(graph, atAc, output));
        assertComplex(2.0 / 3, 0, current(graph, atAc, 50, dc));
        assertComplex(-4.0 / 3, 0, current(graph, atAc, 50, ac));
    }

    @Test
    void sameFrequencySourcesAreCombinedAsPhasors() {
        CircuitGraph graph = groundedGraph();
        int a = graph.addNode(), b = graph.addNode(), output = graph.addNode(), ground = graph.getGround();
        graph.addEdge(a, ground, source("V1", 10, 50, 0));
        graph.addEdge(b, ground, source("V2", 10, 50, Math.PI / 2));
        graph.addEdge(a, output, new Resistor("R1", 10));
        graph.addEdge(b, output, new Resistor("R2", 10));
        graph.addEdge(output, ground, new Resistor("R3", 10));
        CircuitSolution results = solver.solve(graph);
        assertEquals(Set.of(50.0), results.getResults().keySet());
        assertComplex(10.0 / 3, 10.0 / 3, voltage(graph, results, output));
    }

    @Test
    void floatingNodeMakesSystemSingular() {
        CircuitGraph graph = groundedGraph();
        int supply = graph.addNode();
        graph.addNode(); // No reference voltage for this isolated node.
        graph.addEdge(supply, graph.getGround(), dc(5));
        assertThrows(ArithmeticException.class, () -> solver.solve(graph));
    }

    @Test
    void capacitorOnlyDcConnectionLeavesNodeFloating() {
        CircuitGraph graph = groundedGraph();
        int supply = graph.addNode(), floating = graph.addNode();
        graph.addEdge(supply, graph.getGround(), dc(5));
        graph.addEdge(floating, graph.getGround(), new Capacitor("C", 1e-6));
        assertThrows(ArithmeticException.class, () -> solver.solve(graph));
    }

    @Test
    void contradictoryParallelVoltageSourcesAreRejected() {
        CircuitGraph graph = groundedGraph();
        int node = graph.addNode();
        graph.addEdge(node, graph.getGround(), dc(5));
        graph.addEdge(node, graph.getGround(), dc(10));
        assertThrows(ArithmeticException.class, () -> solver.solve(graph));
    }

    @Test
    void redundantIdealSourcesHaveNoUniqueBranchCurrents() {
        CircuitGraph graph = groundedGraph();
        int node = graph.addNode();
        graph.addEdge(node, graph.getGround(), dc(5));
        graph.addEdge(node, graph.getGround(), dc(5));
        graph.addEdge(node, graph.getGround(), new Resistor("R", 5));
        assertThrows(ArithmeticException.class, () -> solver.solve(graph));
    }

    @Test
    void dcInductorAcrossNonzeroVoltageSourceIsRejected() {
        CircuitGraph graph = groundedGraph();
        int node = graph.addNode();
        graph.addEdge(node, graph.getGround(), dc(5));
        graph.addEdge(node, graph.getGround(), new Inductor("L", 0.01));
        assertThrows(ArithmeticException.class, () -> solver.solve(graph));
    }

    @Test
    void shortedSourceIsRejected() {
        CircuitGraph graph = groundedGraph();
        graph.addEdge(graph.getGround(), graph.getGround(), dc(5));
        assertThrows(ArithmeticException.class, () -> solver.solve(graph));
    }

    @Test
    void repeatedSolvesDoNotMutateGraphOrRetainPreviousResults() {
        CircuitGraph graph = groundedGraph();
        int node = graph.addNode(), ground = graph.getGround();
        VoltageSource source = dc(12);
        graph.addEdge(node, ground, source);
        graph.addEdge(node, ground, new Resistor("R1", 6));
        Set<Integer> nodes = graph.getNodes();
        List<Edge> edges = graph.getEdges();
        CircuitSolution first = solver.solve(graph);
        CircuitSolution second = solver.solve(graph);
        assertNotSame(first, second);
        assertNotSame(first.getResults().get(0.0), second.getResults().get(0.0));
        assertEquals(nodes, graph.getNodes());
        assertEquals(edges, graph.getEdges());
        assertEquals(ground, graph.getGround());
        assertComplex(-2, 0, current(graph, first, 0, source));
        assertComplex(-2, 0, current(graph, second, 0, source));
        graph.addEdge(node, ground, new Resistor("R2", 6));
        CircuitSolution afterEdit = solver.solve(graph);
        assertComplex(-4, 0, current(graph, afterEdit, 0, source));
        assertComplex(-2, 0, current(graph, first, 0, source));
    }

    @Test
    void returnedSolutionFeedsAnalyzerWithFrequencySpecificInductorIndices() {
        CircuitGraph graph = groundedGraph();
        int a = graph.addNode(), b = graph.addNode(), c = graph.addNode();
        int ground = graph.getGround();
        // Put the inductor first so later source indices shift between DC and AC.
        graph.addEdge(c, ground, new Inductor("L", 0.01));
        graph.addEdge(a, ground, dc(10));
        graph.addEdge(b, a, source("AC", 20, 50, 0));
        graph.addEdge(b, c, new Resistor("R", 10));
        Edge inductor = graph.getEdges().getFirst();
        CircuitSolution solution = solver.solve(graph);
        assertEquals(ground, solution.getGround());
        assertEquals(Set.of(a, b, c), solution.getNodeIndex().keySet());
        var atDc = solution.getResults().get(0.0);
        var atAc = solution.getResults().get(50.0);
        assertEquals(6, atDc.getSolution().length);
        assertEquals(5, atAc.getSolution().length);
        assertEquals(3, atDc.getSourceIndex().size());
        assertEquals(2, atAc.getSourceIndex().size());
        assertTrue(atDc.getSourceIndex().containsKey(inductor));
        assertFalse(atAc.getSourceIndex().containsKey(inductor));
        CircuitAnalyzer analyzer = new CircuitAnalyzer(solution);
        assertComplex(1, 0, analyzer.getCurrent(inductor, 0));
        double reactance = Math.PI;
        assertComplex(200 / (100 + reactance * reactance),
                -20 * reactance / (100 + reactance * reactance), analyzer.getCurrent(inductor, 50));
        assertComplex(10, 0, analyzer.getNodeVoltage(a, 0));
        assertComplex(0, 0, analyzer.getNodeVoltage(a, 50));
    }

    @Test
    void solutionRemainsAnImmutableSnapshotAfterCallerAndGraphChanges() {
        CircuitGraph graph = groundedGraph();
        int node = graph.addNode();
        graph.addEdge(node, graph.getGround(), dc(10));
        Edge sourceEdge = graph.getEdges().getFirst();
        CircuitSolution solution = solver.solve(graph);
        var result = solution.getResults().get(0.0);
        int voltageIndex = solution.getNodeIndex().get(node);
        Complex[] exposed = result.getSolution();
        exposed[voltageIndex] = new Complex(999);
        assertComplex(10, 0, result.getSolution()[voltageIndex]);
        assertThrows(UnsupportedOperationException.class, () -> solution.getNodeIndex().clear());
        assertThrows(UnsupportedOperationException.class, () -> solution.getResults().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.getSourceIndex().clear());
        graph.removeNode(node);
        assertTrue(solution.getNodeIndex().containsKey(node));
        assertTrue(result.getSourceIndex().containsKey(sourceEdge));
        assertComplex(10, 0, new CircuitAnalyzer(solution).getNodeVoltage(node, 0));
    }
}
