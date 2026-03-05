package core.gameserver.geodata;

import core.commons.geometry.Shape;

public interface GeoCollision
{
	public Shape getShape();

	public byte[][] getGeoAround();

	public void setGeoAround(byte[][] geo);

	public boolean isConcrete();
}
