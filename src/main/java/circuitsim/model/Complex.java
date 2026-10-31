package circuitsim.model;

public final class Complex {

    public static final Complex ZERO = new Complex(0, 0);
    public static final Complex ONE  = new Complex(1, 0);
    public static final Complex I    = new Complex(0, 1);

    private final double real;
    private final double imag;


    public Complex() {
        this(0.0, 0.0);
    }
    public Complex(double real, double imag) {
        this.real = real;
        this.imag = imag;
    }

    public Complex(double real) {
        this(real, 0.0);
    }


    public static Complex fromPolar(double r, double theta) {
        if (!Double.isFinite(r) || r < 0 || !Double.isFinite(theta)) {
            throw new IllegalArgumentException("Invalid magnitude or phase");
        }
        return new Complex(r * Math.cos(theta),r * Math.sin(theta));
    }


    public double real() {
        return real;
    }

    public double imag() {
        return imag;
    }

    public Complex copy() {
        return new Complex(this.real,this.imag);
    }
    public double magnitude() {
        return Math.hypot(real, imag);
    }

    public double magnitudeSquared() {
        return real * real + imag * imag;
    }

    public double phase() {
        return Math.atan2(imag, real);
    }

    public boolean isZero() {
        return real == 0.0 && imag == 0.0;
    }

    public boolean isFinite() {
        return Double.isFinite(real) && Double.isFinite(imag);
    }


    public Complex add(Complex z) {
        return new Complex(real + z.real , imag + z.imag);
    }

    public Complex add(double x) {
        return new Complex(real + x, imag);
    }

    public static Complex add(Complex a, Complex b) {
        return a.add(b);
    }

    public static Complex add(Complex a, double b) {
        return a.add(b);
    }

    public static Complex add(double a, Complex b) {
        return b.add(a);
    }


    public Complex subtract(Complex z) {
        return new Complex(real - z.real , imag - z.imag);
    }

    public Complex subtract(double x) {
        return new Complex(real - x, imag);
    }

    public static Complex subtract(Complex a, Complex b) {
        return a.subtract(b);
    }

    public static Complex subtract(Complex a, double b) {
        return a.subtract(b);
    }

    public static Complex subtract(double a, Complex b) {
        return new Complex(a - b.real , -b.imag);
    }

    public Complex multiply(Complex z) {
        return new Complex(real * z.real - imag * z.imag , real * z.imag + imag * z.real);
    }

    public Complex multiply(double x) {
        return new Complex(real * x , imag * x);
    }

    public static Complex multiply(Complex a, Complex b) {
        return a.multiply(b);
    }

    public static Complex multiply(Complex a, double b) {
        return a.multiply(b);
    }

    public static Complex multiply(double a, Complex b) {
        return b.multiply(a);
    }

    public Complex divide(Complex z) {
        double a = real;
        double b = imag;
        double c = z.real;
        double d = z.imag;

        if (c == 0.0 && d == 0.0) {
            throw new ArithmeticException("Division by zero");
        }

        double r, i;

        if (Math.abs(c) >= Math.abs(d)) {
            double q = d / c;
            double denominator = c + d * q;

            r = (a + b * q) / denominator;
            i = (b - a * q) / denominator;
        } 
        else {
            double q = c / d;
            double denominator = d + c * q;

            r = (a * q + b) / denominator;
            i = (b * q - a) / denominator;
        }

        return new Complex(r, i);
    }

    public Complex divide(double x) {
        if (x == 0.0) {
            throw new ArithmeticException("Division by zero");
        }

        return new Complex(real / x , imag / x);
    }

    public static Complex divide(Complex a, Complex b) {
        return a.divide(b);
    }

    public static Complex divide(Complex a, double b) {
        return a.divide(b);
    }

    public static Complex divide(double a, Complex b) {
        return new Complex(a).divide(b);
    }

    public Complex negate() {
        return new Complex(-real, -imag);
    }

    public Complex conjugate() {
        return new Complex(real, -imag);
    }

    public Complex reciprocal() {
        return ONE.divide(this);
    }

    public boolean approximatelyEquals(Complex other, double tolerance) {
        if (other == null) {
            return false;
        }

        if (!Double.isFinite(tolerance) || tolerance < 0) {
            throw new IllegalArgumentException("Tolerance must be finite and nonnegative");
        }
        return this.subtract(other).magnitude() <= tolerance;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Complex other)) {
            return false;
        }
        return Double.compare(real, other.real) == 0 && Double.compare(imag, other.imag) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(real, imag);
    }


    @Override
    public String toString() {
        if (imag < 0) {
            return real + " - " + (-imag) + "j";
        }
        return real + " + " + imag + "j";
    }
}
