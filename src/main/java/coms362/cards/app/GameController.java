package coms362.cards.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import coms362.cards.abstractcomp.GameFactory;
import coms362.cards.abstractcomp.Move;
import coms362.cards.abstractcomp.Rules;
import coms362.cards.abstractcomp.Table;
import coms362.cards.fiftytwo.PartyRole;
import coms362.cards.fiftytwo.PickupRules;
import coms362.cards.streams.InBoundQueue;
import coms362.cards.streams.RemoteTableGateway;
import coms362.events.InvalidGameSelection;
import events.inbound.ConnectEvent;
import events.inbound.EndPlay;
import events.inbound.Event;
import events.inbound.EventFactory;
import events.inbound.EventUnmarshallers;
import events.inbound.NewPartyEvent;
import events.inbound.SelectGame;
import events.inbound.SetQuorumEvent;
import events.inbound.SysEvent;
import model.Game;
import model.Party;
import model.Quorum;
import model.TableBase;


/**
 * This class is responsible for processing events involved in
 * determining what game is to be played. It runs in a context with
 * a single default view.
 *
 * It defers events that can't be processed without access to concrete
 * game rules: for example
 * how many players are required/allowed, and what position and role
 * each party will hold.
 *
 * Currently events carrying this and related game/match information
 * can be submitted by the host on  * the query string, for example:
 *
 * ?host&player=1&min=1&max=2&game=PU52MP
 *
 * The "host" parameter qualifies the connection to select game and specify
 * game "quorum" limits.
 * In this case, the host would also be registered as player 1.
 * The "host" is <em>not</em> required to be a
 * player in all games.
 *
 *
 * @author Robert Ward
 *
 */
public class GameController
{

	private InBoundQueue inQ;
	private RemoteTableGateway remote;
	private GameFactory factory;
	private GameFactoryFactory abstractFactory;
    Stack<Event> deferred = new Stack<Event>();
    Game game = new Game();

	public GameController (InBoundQueue inQ, RemoteTableGateway gateway, GameFactoryFactory gFFactory){
		this.inQ = inQ;
		this.remote = gateway;
		this.abstractFactory = gFFactory;
	}

	public void run (){

        System.out.println("Application Started");

    	Event e = null;
        while (! game.isSelected()  )
        {
         	try {
        		e = inQ.take();
        		((SysEvent) e).accept(this, game);

        	}
         	catch (ExitTestException ex){
         		break;
         	}
         	catch (Exception ex){
        		System.out.println("GameController Exception "+ex.getMessage());
        		System.out.println("Deferring unhandled event "+ e.toString());
        		ex.printStackTrace(System.err);
        		// we can't do anything game specific until after we have seen SelectGame.
        		deferred.add(e);
        	};
        }

        GameFactory factory = abstractFactory.getGameFactory(game.getSelection());
		Rules rules = factory.createRules();
		Table table = factory.createTable();
		inQ.pushBack(deferred);

		MatchController match = new MatchController(
			inQ, table, rules, remote, factory
			);
		match.start();
	}

	// TODO: make specific handlers for each match pre-req event.
	public void apply(ConnectEvent e, Game game) {		// TODO Auto-generated method stub
		System.out.println("GameRules.apply "+e.getClass().getSimpleName() );
		System.out.println(e.toString());


		String selection = "";
		if (e.getParam("host") != null){
			if ((selection = e.getParam("game")) != null){
				inQ.pushBack(new SelectGame(selection, e));
			}
		}
		String pnum = null;
		if ((pnum = e.getParam("player"))!=null){
			pnum = (pnum.isEmpty() ? "1" : pnum );
			deferred.push(
				new NewPartyEvent( PartyRole.player, pnum, e.getSocketId())
			);
		}
	}

	public void apply (SelectGame e, Game game){
		String selected = "";
		if ( abstractFactory.isValidSelection(selected = e.getSelection() ) ){
			game.setSelected(selected);
			// without other information force to a maximum of 4
			// To give the rules a chance to supply game specific limits.
			Quorum pushQ = (e.hasQuorum()) ? e.getQuorum() : new Quorum(4,4);
			deferred.insertElementAt(new SetQuorumEvent(e.getQuorum()), 0);
		} else {
			// we need to inform the alleged host now
			System.out.format("GameController. SelectGame : %s is not a supported game.", selected );
			inQ.pushBack(new InvalidGameSelection(selected));
		}

	}

	public void apply (InvalidGameSelection e, Game game)
	{
		System.out.println("InvalidGameSelection Event");
		try {
			remote.send(new SystemStatus( e.getMsg() ), "default");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void apply(NewPartyEvent e, Game game) {
		deferred.add(e);
	}

	// should not normally be processed from game controller.
	public void apply(EndPlay endPlay, Game game2) {
		throw new ExitTestException("Exit on EndPlay Event");
	}



}
