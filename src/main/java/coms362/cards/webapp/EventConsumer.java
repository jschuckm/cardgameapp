package coms362.cards.webapp;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.ServiceLoader;

import coms362.cards.socket.CardSocket;
import coms362.cards.socket.CardSocketListener;
import coms362.cards.socket.SocketEvent;
import coms362.cards.streams.InBoundQueue;
import coms362.cards.streams.RemoteTableGateway;
import events.inbound.CardEvent;
import events.inbound.DealEvent;
import events.inbound.Event;
import events.inbound.EventFactory;
import events.inbound.EventUnmarshallers;
import events.inbound.GameRestartEvent;

public class EventConsumer implements CardSocketListener {
    private InBoundQueue q; 
    private CardSocket cardSocket;
    private EventUnmarshallers handlers;

    public EventConsumer(InBoundQueue q, EventUnmarshallers handlers) {
    	this.q = q;
    	this.handlers = handlers;
    }

    public void onConnect() {
        System.out.println("onConnect");
    }

    public void onReceive(SocketEvent event) {
    	Event e = createEvent(event);
    	if (e != null){
    		q.add(e);
    	}
    	//otherwise, drop it on the floor.     	
    }
    
    // TODO: move to injected Event factory. 
    private Event createEvent(SocketEvent e){
    	Object eventObj = e.get("event");
        if (eventObj == null) {
        	return null;
        }
        
        String eventName = (String) eventObj;
        System.out.println(eventName);
        
        Class klass = handlers.getHandler(eventName);
        System.out.println("handling event "+e.getName());
        System.out.println("registry instance = "+handlers);
        
        if (klass != null){
        	try {
				Method m = klass.getDeclaredMethod("createEvent", SocketEvent.class);
				return (Event) m.invoke(null, e);
			} catch (NoSuchMethodException | SecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalArgumentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
        else {
        	//drop it on the floor
        	System.err.println("Unable to process socket event " + e.toString());
        }
        return null;          
            
    }

    public void setCardSocket(CardSocket cardSocket) {
        this.cardSocket = cardSocket;
        RemoteTableGateway.getInstance().setSocket(cardSocket.getRemote());
    }
}
