package core.gameserver.model.phantom;

public class PhantomSet
{
	public final int body;
	public final int gaiters;
	public final int gloves;
	public final int boots;
	public final int weapon;
	public final int custom;
	public final int grade;

	public PhantomSet(int body, int gaiters, int gloves, int boots, int weapon, int grade, int custom)
	{
		this.body = body;
		this.gaiters = gaiters;
		this.gloves = gloves;
		this.boots = boots;
		this.weapon = weapon;
		this.grade = grade;
		this.custom = custom;
	}
}
