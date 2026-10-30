package circuitsim.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ComplexTest {

    private static final double EPS = 1e-9;

    private void assertComplexEquals(
            double real, double imag, Complex actual) {
        assertEquals(real, actual.real(), EPS);
        assertEquals(imag, actual.imag(), EPS);
    }

    @Test
    void testConstructors() {
        assertComplexEquals(3, 4, new Complex(3, 4));
        assertComplexEquals(5, 0, new Complex(5));
    }

    @Test
    void testAddition() {
        Complex a = new Complex(3, 4);
        Complex b = new Complex(1, -2);

        assertComplexEquals(4, 2, a.add(b));
        assertComplexEquals(8, 4, a.add(5));
        assertComplexEquals(4, 2, Complex.add(a, b));
        assertComplexEquals(8, 4, Complex.add(5, a));
    }

    @Test
    void testSubtraction() {
        Complex a = new Complex(3, 4);
        Complex b = new Complex(1, -2);

        assertComplexEquals(2, 6, a.subtract(b));
        assertComplexEquals(-2, 4, a.subtract(5));
        assertComplexEquals(2, 6, Complex.subtract(a, b));
        assertComplexEquals(2, -4, Complex.subtract(5, a));
    }

    @Test
    void testMultiplication() {
        Complex a = new Complex(3, 4);
        Complex b = new Complex(1, -2);

        assertComplexEquals(11, -2, a.multiply(b));
        assertComplexEquals(6, 8, a.multiply(2));
        assertComplexEquals(11, -2, Complex.multiply(a, b));
        assertComplexEquals(6, 8, Complex.multiply(2, a));
    }

    @Test
    void testDivision() {
        Complex a = new Complex(3, 4);
        Complex b = new Complex(1, -2);

        assertComplexEquals(-1, 2, a.divide(b));
        assertComplexEquals(1.5, 2, a.divide(2));
        assertComplexEquals(-1, 2, Complex.divide(a, b));
        assertComplexEquals(0.4, 0.8, Complex.divide(2, b));
    }

    @Test
    void testDivisionByZero() {
        Complex a = new Complex(3, 4);

        assertThrows(ArithmeticException.class,
            () -> a.divide(Complex.ZERO));

        assertThrows(ArithmeticException.class,
            () -> a.divide(0));
    }

    @Test
    void testPolar() {
        Complex z = Complex.fromPolar(5, Math.PI / 2);

        assertComplexEquals(0, 5, z);
        assertEquals(5, z.magnitude(), EPS);
        assertEquals(Math.PI / 2, z.phase(), EPS);
    }

    @Test
    void testUnaryOperations() {
        Complex a = new Complex(3, 4);

        assertComplexEquals(-3, -4, a.negate());
        assertComplexEquals(3, -4, a.conjugate());
        assertComplexEquals(0.12, -0.16, a.reciprocal());
    }

    @Test
    void testMethodChaining() {
        Complex a = new Complex(3, 4);
        Complex b = new Complex(1, -2);
        Complex c = new Complex(2, 1);

        // (a * b + c) / 2
        Complex result = a.multiply(b).add(c).divide(2);

        assertComplexEquals(6.5, -0.5, result);
    }

    @Test
    void testImmutability() {
        Complex a = new Complex(3, 4);

        Complex b = a.add(5);

        assertComplexEquals(3, 4, a);
        assertComplexEquals(8, 4, b);
        assertNotSame(a, b);
    }

    @Test
    void testEquality() {
        Complex a = new Complex(3, 4);
        Complex b = new Complex(3, 4);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertTrue(a.approximatelyEquals(
            new Complex(3 + 1e-10, 4), EPS
        ));
    }
}
