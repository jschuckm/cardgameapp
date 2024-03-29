package coms362.cards;


import static org.junit.Assert.*;

import org.junit.Test;

import coms362.cards.abstractcomp.Table;
import coms362.cards.app.GameController;
import coms362.cards.fiftytwo.TestEndpoint;
import coms362.cards.fiftytwo.TestGFF;
import coms362.cards.fiftytwo.TestGameFactory;
import coms362.cards.socket.SocketEvent;
import coms362.cards.streams.InBoundQueue;
import coms362.cards.streams.RemoteTableGateway;
import events.inbound.ConnectEvent;
import events.inbound.DealEvent;
import events.inbound.EndPlay;
import model.Quorum;


public class TestConnectEvents {


	
	public String fullHostInfo = "{"
				+ "\"event\": \"Connect\","
				+ "\"host\": \"\","
				+ "\"min\": \"1\","
				+ "\"max\": \"4\","
				+ "\"player\": \"1\","
				+ "\"game\": \"PU52MP\"}";			

	@Test
	public void testGameSelection() {
		//set up input, output, and abstract factory 
		InBoundQueue inQ = new InBoundQueue();
		//pre-load the input stream with the input for this test
		inQ.add(new ConnectEvent(new SocketEvent(fullHostInfo, 10001)));
		inQ.add(new DealEvent());
		inQ.add(new EndPlay());
		RemoteTableGateway gw = RemoteTableGateway.getInstance();
		TestEndpoint epLog = new TestEndpoint();				
		gw.setSocket(epLog);
		TestGFF gff = new TestGFF();
		GameController sut = new GameController(inQ, gw, gff);
		sut.run();
		assertTrue(gff.activeFactory instanceof TestGameFactory);
		assertNotNull(gff.activeFactory.activeTable);
		Table table = gff.activeFactory.activeTable;
		Quorum quorum = null; 
		assertNotNull(quorum = table.getQuorum());
		assertEquals(1, table.getPlayers().size());
		System.out.println(epLog.getLog());
		
	}

}
