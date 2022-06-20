package application;

import java.io.Serializable;

public class Request implements Serializable
{
    public String command;
    public Object payload;

    public Request(String c, Object p)
    {
        command=c;
        payload=p;
    }
}
