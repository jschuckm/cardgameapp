/**
 *
 */
package coms362.cards.fiftytwo;

import java.io.IOException;

import coms362.cards.abstractcomp.Move;
import coms362.cards.abstractcomp.Player;
import coms362.cards.abstractcomp.View;
import coms362.cards.streams.Marshalls;
import coms362.cards.streams.RemoteTableGateway;
import events.remote.FilterOnOwner;

/**
 * @author Robert Ward
 *
 */
public class P52PlayerView implements View {

	private RemoteTableGateway remote;
	private Integer pos;
	private String socketId;

	public P52PlayerView(Integer num, String socketId, RemoteTableGateway remote) {
		this.remote = remote;
		this.socketId = socketId;
		this.pos = num;
	}

	public void send(Marshalls event) throws IOException {
		if (event instanceof FilterOnOwner ){
			if (!((FilterOnOwner)event).isOwnedBy(socketId)){
				return;
			}
		}
		System.out.format("View %s  sending event %s to socket %s%n" ,
				this, event, socketId
				);
		remote.send(event, socketId);
	}

	@Override
	public int getCameraPosition() {
		return pos;
	}

}
