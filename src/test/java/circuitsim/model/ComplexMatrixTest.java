
package circuitsim.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ComplexMatrixTest {

    private static final double EPS = 1e-9;

    private void assertComplexEquals(
            Complex expected, Complex actual) {
        assertNotNull(actual);
        assertEquals(expected.real(), actual.real(), EPS);
        assertEquals(expected.imag(), actual.imag(), EPS);
    }

    private void assertMatrixEquals(
            ComplexMatrix expected, ComplexMatrix actual) {

        assertEquals(expected.rows(), actual.rows());
        assertEquals(expected.cols(), actual.cols());

        for (int i = 0; i < expected.rows(); i++) {
            for (int j = 0; j < expected.cols(); j++) {
                assertComplexEquals(
                    expected.get(i, j),
                    actual.get(i, j)
                );
            }
        }
    }

    @Test
    void testZeroMatrix() {
        ComplexMatrix A = new ComplexMatrix(3, 2);

        assertEquals(3, A.rows());
        assertEquals(2, A.cols());

        for (int i = 0; i < A.rows(); i++) {
            for (int j = 0; j < A.cols(); j++) {
                assertComplexEquals(Complex.ZERO, A.get(i, j));
            }
        }
    }

    @Test
    void testConstructorCopiesData() {
        Complex[][] values = {
            {new Complex(1), new Complex(2)},
            {new Complex(3), new Complex(4)}
        };

        ComplexMatrix A = new ComplexMatrix(values);

        values[0][0] = new Complex(100);

        assertComplexEquals(new Complex(1), A.get(0, 0));
    }

    @Test
    void testInvalidConstructors() {
        assertThrows(IllegalArgumentException.class,
            () -> new ComplexMatrix(0, 2));

        assertThrows(IllegalArgumentException.class,
            () -> new ComplexMatrix(2, -1));

        assertThrows(IllegalArgumentException.class,
            () -> new ComplexMatrix(new Complex[][] {}));

        assertThrows(IllegalArgumentException.class,
            () -> new ComplexMatrix(new Complex[][] {
                {Complex.ONE, Complex.ZERO},
                {Complex.ONE}
            }));

        assertThrows(IllegalArgumentException.class,
            () -> new ComplexMatrix(new Complex[][] {
                {Complex.ONE, null}
            }));
    }

    @Test
    void testSetAndGet() {
        ComplexMatrix A = new ComplexMatrix(2, 2);

        A.set(0, 1, new Complex(3, -4));

        assertComplexEquals(
            new Complex(3, -4), A.get(0, 1)
        );

        assertThrows(IllegalArgumentException.class,
            () -> A.set(0, 0, null));
    }

    @Test
    void testAddTo() {
        ComplexMatrix A = new ComplexMatrix(2, 2);

        A.set(0, 0, new Complex(2, 3));
        A.addTo(0, 0, new Complex(1, -5));

        assertComplexEquals(
            new Complex(3, -2), A.get(0, 0)
        );
    }

    @Test
    void testMatrixAddition() {
        ComplexMatrix A = new ComplexMatrix(new Complex[][] {
            {new Complex(1, 2), new Complex(3)},
            {new Complex(4), new Complex(5, -1)}
        });

        ComplexMatrix B = new ComplexMatrix(new Complex[][] {
            {new Complex(2, -1), new Complex(1, 3)},
            {new Complex(-4), new Complex(2, 1)}
        });

        ComplexMatrix expected = new ComplexMatrix(new Complex[][] {
            {new Complex(3, 1), new Complex(4, 3)},
            {Complex.ZERO, new Complex(7)}
        });

        assertMatrixEquals(expected, A.add(B));

        // Hai ma trận ban đầu không bị thay đổi
        assertComplexEquals(new Complex(1, 2), A.get(0, 0));
        assertComplexEquals(new Complex(2, -1), B.get(0, 0));
    }

    @Test
    void testAdditionDimensionMismatch() {
        ComplexMatrix A = new ComplexMatrix(2, 3);
        ComplexMatrix B = new ComplexMatrix(3, 2);

        assertThrows(IllegalArgumentException.class,
            () -> A.add(B));

        assertThrows(IllegalArgumentException.class,
            () -> A.add(null));
    }

    @Test
    void testMatrixMultiplication() {
        ComplexMatrix A = new ComplexMatrix(new Complex[][] {
            {new Complex(1), new Complex(2), new Complex(3)},
            {new Complex(4), new Complex(5), new Complex(6)}
        });

        ComplexMatrix B = new ComplexMatrix(new Complex[][] {
            {new Complex(7), new Complex(8)},
            {new Complex(9), new Complex(10)},
            {new Complex(11), new Complex(12)}
        });

        ComplexMatrix expected = new ComplexMatrix(new Complex[][] {
            {new Complex(58), new Complex(64)},
            {new Complex(139), new Complex(154)}
        });

        assertMatrixEquals(expected, A.multiply(B));
    }

    @Test
    void testComplexMultiplication() {
        ComplexMatrix A = new ComplexMatrix(new Complex[][] {
            {new Complex(1, 1), new Complex(2)}
        });

        ComplexMatrix B = new ComplexMatrix(new Complex[][] {
            {new Complex(1, -1)},
            {new Complex(3, 2)}
        });

        // (1+j)(1-j) + 2(3+2j) = 8+4j
        ComplexMatrix C = A.multiply(B);

        assertEquals(1, C.rows());
        assertEquals(1, C.cols());
        assertComplexEquals(new Complex(8, 4), C.get(0, 0));
    }

    @Test
    void testMultiplicationDimensionMismatch() {
        ComplexMatrix A = new ComplexMatrix(2, 3);
        ComplexMatrix B = new ComplexMatrix(2, 2);

        assertThrows(IllegalArgumentException.class,
            () -> A.multiply(B));

        assertThrows(IllegalArgumentException.class,
            () -> A.multiply(null));
    }

    @Test
    void testSolveRealSystem() {
        // 2x + y = 5
        // 4x + 3y = 11
        ComplexMatrix A = new ComplexMatrix(new Complex[][] {
            {new Complex(2), new Complex(1)},
            {new Complex(4), new Complex(3)}
        });

        Complex[] b = {
            new Complex(5),
            new Complex(11)
        };

        Complex[] x = A.solve(b);

        assertComplexEquals(new Complex(2), x[0]);
        assertComplexEquals(new Complex(1), x[1]);
    }

    @Test
    void testSolveComplexSystem() {
        ComplexMatrix A = new ComplexMatrix(new Complex[][] {
            {new Complex(1, 1), new Complex(2)},
            {new Complex(3), new Complex(1, -1)}
        });

        // Nghiệm mong đợi: x = 1+j, y = 2-j
        Complex[] expected = {
            new Complex(1, 1),
            new Complex(2, -1)
        };

        Complex[] b = {
            new Complex(4, 0),
            new Complex(4, 0)
        };

        // Tạo b = A * expected để tránh sai số khi tự tính
        for (int i = 0; i < 2; i++) {
            b[i] = Complex.ZERO;
            for (int j = 0; j < 2; j++) {
                b[i] = b[i].add(
                    A.get(i, j).multiply(expected[j])
                );
            }
        }

        Complex[] actual = A.solve(b);

        for (int i = 0; i < expected.length; i++) {
            assertComplexEquals(expected[i], actual[i]);
        }
    }

    @Test
    void testSolveRequiresPivoting() {
        ComplexMatrix A = new ComplexMatrix(new Complex[][] {
            {Complex.ZERO, Complex.ONE},
            {Complex.ONE, Complex.ONE}
        });

        Complex[] b = {
            new Complex(2),
            new Complex(5)
        };

        Complex[] x = A.solve(b);

        assertComplexEquals(new Complex(3), x[0]);
        assertComplexEquals(new Complex(2), x[1]);
    }

    @Test
    void testSolveSingularMatrix() {
        ComplexMatrix A = new ComplexMatrix(new Complex[][] {
            {new Complex(1), new Complex(2)},
            {new Complex(2), new Complex(4)}
        });

        Complex[] b = {
            new Complex(3),
            new Complex(6)
        };

        assertThrows(ArithmeticException.class,
            () -> A.solve(b));
    }

    @Test
    void testSolveDoesNotModifyInputs() {
        ComplexMatrix A = new ComplexMatrix(new Complex[][] {
            {new Complex(2), new Complex(1)},
            {new Complex(4), new Complex(3)}
        });

        Complex[] b = {
            new Complex(5),
            new Complex(11)
        };

        A.solve(b);

        assertComplexEquals(new Complex(2), A.get(0, 0));
        assertComplexEquals(new Complex(4), A.get(1, 0));
        assertComplexEquals(new Complex(5), b[0]);
        assertComplexEquals(new Complex(11), b[1]);
    }

    @Test
    void testSolveInvalidInputs() {
        ComplexMatrix rectangular = new ComplexMatrix(2, 3);

        assertThrows(IllegalArgumentException.class,
            () -> rectangular.solve(new Complex[2]));

        ComplexMatrix square = new ComplexMatrix(2, 2);

        assertThrows(IllegalArgumentException.class,
            () -> square.solve(null));

        assertThrows(IllegalArgumentException.class,
            () -> square.solve(new Complex[] {Complex.ONE}));

        assertThrows(IllegalArgumentException.class,
            () -> square.solve(new Complex[] {Complex.ONE, null}));
    }
}
