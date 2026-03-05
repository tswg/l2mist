package core.gameserver.captcha;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author bloodshed (L2NextGen)
 * @date 25.04.2011
 * @time 22:31:10
 */
public class ByteInputStream extends ByteArrayInputStream
{
	private static final byte[] EMPTY_ARRAY = new byte[0];

	public ByteInputStream()
	{
		this(EMPTY_ARRAY, 0);
	}

	public ByteInputStream(byte buf[], int length)
	{
		super(buf, 0, length);
	}

	public ByteInputStream(byte buf[], int offset, int length)
	{
		super(buf, offset, length);
	}

	public byte[] getBytes()
	{
		return buf;
	}

	public int getCount()
	{
		return count;
	}

	@Override
	public void close() throws IOException
	{
		reset();
	}

	public void setBuf(byte[] buf)
	{
		this.buf = buf;
		pos = 0;
		count = buf.length;
	}
}
