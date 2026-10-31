package circuitsim.logic;

import circuitsim.model.*;
import java.util.*;

public class NodalSolver {

    public Map<Double, Complex[]> solve(CircuitGraph graph) {
        if (!graph.getNodes().contains(graph.getGround())) {
            throw new IllegalArgumentException("Ground is not set");
        }

        Map<Integer, Integer> nodeIndex = new HashMap<>();

        int index = 0;
        for (int node : graph.getNodes()) {
            if (node == graph.getGround()) continue;
            nodeIndex.put(node, index++);
        }

        Set<Double> freqs = new HashSet<>();

        for (Edge edge : graph.getEdges()) {
            if (edge.getComponent() instanceof VoltageSource source) {
                freqs.add(source.getFrequency());
            }
        }

        Map<Double, Complex[]> results = new HashMap<>();

        for (double freq : freqs) {
            results.put(freq, solveAtFrequency(graph, freq, nodeIndex));
        }

        return results;
    }

    private Complex[] solveAtFrequency(CircuitGraph graph,double freq,Map<Integer, Integer> nodeIndex) {
        Map<Edge, Integer> sourceIndex = new HashMap<>();

        int k = nodeIndex.size();

        for (Edge edge : graph.getEdges()) {
            Component component = edge.getComponent();
            if (component instanceof VoltageSource || (freq == 0.0 && component instanceof Inductor)) {
                sourceIndex.put(edge, k++);
            }
        }

        int n = k;

        ComplexMatrix eqMatrix = new ComplexMatrix(n, n);
        Complex[] rhs = new Complex[n];

        for (int i = 0; i < n; i++) {
            rhs[i] = new Complex(0);

            for (int j = 0; j < n; j++) {
                eqMatrix.set(i, j, new Complex(0));
            }
        }

        double omega = 2 * Math.PI * freq;

        for (Edge edge : graph.getEdges()) {
            int a = nodeIndex.getOrDefault(edge.getFrom(), -1);
            int b = nodeIndex.getOrDefault(edge.getTo(), -1);
            Component component = edge.getComponent();
            if (component instanceof Resistor resistor) {
                Complex y = new Complex(1.0 / resistor.getResistance());
                stampAdmittance(eqMatrix, a, b, y);
            }

            else if (component instanceof Capacitor capacitor) {
                Complex y = Complex.multiply(omega * capacitor.getCapacitance(),Complex.I);
                stampAdmittance(eqMatrix, a, b, y);
            }

            else if (component instanceof Inductor inductor) {
                if (freq == 0.0) {
                    int currentIndex = sourceIndex.get(edge);

                    stampVoltageSource(
                        eqMatrix, rhs, a, b,
                        currentIndex, new Complex(0)
                    );
                } else {
                    Complex y = Complex.multiply(
                        -1.0 / (omega * inductor.getInductance()),
                        Complex.I
                    );

                    stampAdmittance(eqMatrix, a, b, y);
                }
            }

            else if (component instanceof VoltageSource source) {
                int currentIndex = sourceIndex.get(edge);
                Complex voltage = getSourceVoltage(source, freq);

                stampVoltageSource(
                    eqMatrix, rhs, a, b,
                    currentIndex, voltage
                );
            }
        }

        return eqMatrix.solve(rhs);
    }

    private void stampAdmittance(ComplexMatrix matrix,int a,int b,Complex y) {

        if (a != -1) {
            matrix.set(a, a,Complex.add(matrix.get(a, a), y));
        }
        if (b != -1) {
            matrix.set(b, b,Complex.add(matrix.get(b, b), y));
        }
        if (a != -1 && b != -1) {
            matrix.set(a, b,Complex.subtract(matrix.get(a, b), y));
            matrix.set(b, a,Complex.subtract(matrix.get(b, a), y));
        }
    }

    private void stampVoltageSource(ComplexMatrix matrix,Complex[] rhs,int a,int b,int k,Complex voltage) {

        Complex one = new Complex(1);

        if (a != -1) {
            matrix.set(a, k,
                Complex.add(matrix.get(a, k), one));

            matrix.set(k, a,
                Complex.add(matrix.get(k, a), one));
        }

        if (b != -1) {
            matrix.set(b, k,Complex.subtract(matrix.get(b, k), one));

            matrix.set(k, b,Complex.subtract(matrix.get(k, b), one));
        }

        rhs[k] = Complex.add(rhs[k], voltage);
    }

    private Complex getSourceVoltage(VoltageSource source,double freq) {

        if (source.getFrequency() != freq) {
            return new Complex(0);
        }

        double amplitude = source.getVoltage();
        double phase = source.getPhase();

        return new Complex(
            amplitude * Math.cos(phase),
            amplitude * Math.sin(phase)
        );
    }
}