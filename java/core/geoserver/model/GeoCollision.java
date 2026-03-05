package core.geoserver.model;

public interface GeoCollision
{
	public GeoShape getShape();
	public byte[][] getGeoAround();
	public void setGeoAround(byte[][] geo);
	public boolean isConcrete();
}
