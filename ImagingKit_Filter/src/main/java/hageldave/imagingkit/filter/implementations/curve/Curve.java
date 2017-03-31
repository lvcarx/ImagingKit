package hageldave.imagingkit.filter.implementations.curve;

import java.util.function.DoubleFunction;
import java.util.function.Function;

public interface Curve extends DoubleFunction<Double>, Function<Double, Double> {
	
	public double interpolate(double v);
	
	@Override
	default Double apply(double value) {
		return interpolate(value);
	}
	
	@Override
	default Double apply(Double value) {
		return interpolate(value);
	}
	
	public default Curve affineTransformedBy(double a, double b){
		return v -> a*interpolate(v)+b;
	}
	
	public default Curve shiftedBy(double x){
		return v -> interpolate(v+x);
	}
	
	public static Curve identity(){return v->v;}
	
	public static Curve constant(double c){return v->c;}
}