package plugins.fmp.fmpTools;



public interface NHDistance<T> 
{
	double computeDistance(T s1, T s2) throws NHFeatureException;
}