package br.uneb.montecarlo.simulation;

import br.uneb.montecarlo.model.OptionParameters;

/** Analytical reference price for a European call without dividends. */
public final class BlackScholes {
    private BlackScholes() {
    }

    public static double callPrice(OptionParameters p) {
        double s = p.initialPrice();
        double k = p.strikePrice();
        double r = p.annualRiskFreeRate();
        double sigma = p.annualVolatility();
        double t = p.maturityYears();
        double discount = StrictMath.exp(-r * t);
        if (sigma == 0.0) {
            return discount * Math.max(s * StrictMath.exp(r * t) - k, 0.0);
        }
        double sigmaSqrtT = sigma * StrictMath.sqrt(t);
        double d1 = (StrictMath.log(s / k) + (r + 0.5 * sigma * sigma) * t) / sigmaSqrtT;
        double d2 = d1 - sigmaSqrtT;
        return Math.max(0.0, s * standardNormalCdf(d1) - k * discount * standardNormalCdf(d2));
    }

    private static double standardNormalCdf(double x) {
        double a = Math.abs(x);
        double z = 1.0 / (1.0 + 0.2316419 * a);
        double polynomial = z * (0.319381530 + z * (-0.356563782
                + z * (1.781477937 + z * (-1.821255978 + z * 1.330274429))));
        double tail = StrictMath.exp(-0.5 * a * a) / StrictMath.sqrt(2.0 * StrictMath.PI)
                * polynomial;
        return x >= 0 ? 1.0 - tail : tail;
    }
}
