package core.gameserver.network.l2.c2s;

import core.gameserver.model.Player;
//import core.gameserver.network.l2.Pinger;

public class NetPing extends L2GameClientPacket 
{
    int kID;
    int ping;
    int mtu;

    @Override
    protected void readImpl() 
    {
        kID = readD();
        ping = readD();
        mtu = readD();
    }

    @Override
    protected void runImpl() 
    {
    	Player activeChar = getClient().getActiveChar();
        if(activeChar == null)
        	return;
        
        //Pinger.getInstance().answerPing(activeChar.getObjectId());
        //System.out.println("PING:"+ping+":MTU:"+mtu);
    }

    @Override
    public String getType() 
    {
        return "[C] B1 NetPing";
    }
}