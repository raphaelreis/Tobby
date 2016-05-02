package controller.characters;

import frames.Processor;
import gamedata.d2p.ankama.Map;
import gamedata.enums.BreedEnum;
import gui.Controller;

import java.util.Hashtable;

import controller.CharacterBehaviour;
import controller.CharacterState;
import controller.api.InteractionAPI;
import controller.api.MovementAPI;
import controller.api.SocialAPI;
import controller.informations.CharacterInformations;
import controller.informations.PartyManager;
import controller.informations.RoleplayContext;
import main.FatalError;
import main.Log;
import main.Main;
import main.NetworkInterface;
import messages.Message;
import messages.character.BasicWhoIsRequestMessage;

public abstract class Character extends Thread {
	private Hashtable<CharacterState, Boolean> states;
	public int id; // identifiant du personnage
	public Log log; // gestion des logs (fichier + historique graphique)
	public Thread[] threads; // tableau contenant les 4 threads du personnage
	public NetworkInterface net; // gestion de la connexion r�seau
	public Processor processor; // entit� charg�e du traitement des messages
	public CharacterInformations infos;
	public RoleplayContext roleplayContext;
	public PartyManager partyManager;
	public MovementAPI mvt;
	public InteractionAPI interaction;
	public SocialAPI social;
	
	public static Character create(int id, int behaviour, String login, String password, int serverId, int areaId, Log log) {
		switch(behaviour) {
			case CharacterBehaviour.WAITING_MULE : return new Mule(id, login, password, serverId, BreedEnum.Sadida, log);
			case CharacterBehaviour.TRAINING_MULE : throw new FatalError("Not implemented yet !");
			case CharacterBehaviour.SELLER : throw new FatalError("Not implemented yet !");
			case CharacterBehaviour.LONE_WOLF : return new LoneFighter(id, login, password, serverId, BreedEnum.Cra, areaId, log);
			case CharacterBehaviour.CAPTAIN : return new Captain(id, login, password, serverId, BreedEnum.Cra, areaId, log);
			case CharacterBehaviour.SOLDIER : return new Soldier(id, login, password, serverId, BreedEnum.Cra, log);
			default : throw new FatalError("Unknown behaviour.");
		}
	}

	protected Character(int id, String login, String password, int serverId, int breed, Log log) {
		super(login + "/controller");
		this.id = id;
		
		// initialisation des modules principaux
		this.log = log;
		this.net = new NetworkInterface(this, login);
		this.processor = new Processor(this, login);
		
		// initialisation des threads
		this.threads = new Thread[4];
		this.threads[0] = this.net;
		this.threads[1] = this.net.sender;
		this.threads[2] = this.processor;
		this.threads[3] = this;
		
		// initialisation des modules du contr�leur
		this.infos = new CharacterInformations(login, password, serverId, breed);
		this.roleplayContext = new RoleplayContext(this);
		this.partyManager = new PartyManager(this);
		this.mvt = new MovementAPI(this);
		this.interaction = new InteractionAPI(this);
		this.social = new SocialAPI(this);
		
		// initialisation de la table des �tats
		this.states = new Hashtable<CharacterState, Boolean>();
		for(CharacterState state : CharacterState.values())
			this.states.put(state, false);
		
		// lancement de la thread de traitement (qui va lancer les autres threads le moment venu)
		this.processor.start();
		
		Log.info("Character with id = " + this.id + " started.");
	}
	
	@Override
	public abstract void run(); // impl�ment�e par les diff�rents personnages
	
	public static void log(String msg) {
		Log log = Controller.getInstance().getLog();
		if(log != null)
			log.p(msg);
		else
			Log.info(msg);
	}
	
	public static void log(String direction, Message msg) {
		Log log = Controller.getInstance().getLog();
		if(log != null)
			log.p(direction, msg);
		else
			Log.info(msg.toString());
	}
	
	// destruction des threads du personnage depuis la GUI (forc�e ou non)
	public void deconnectionOrder(boolean forced) {
		if(forced) {
			this.net.sender.interrupt(); // on interrompt d'abord le sender pour �viter une exception
			this.net.closeReceiver();
			this.processor.interrupt();
			interrupt();
		}
		else
			updateState(CharacterState.SHOULD_DECONNECT, true);
	}
	
	public boolean isActive() {
		for(Thread thread : this.threads)
			if(thread.isAlive())
				return true;
		return false;
	}

	public void updatePosition(Map map, int cellId) {
		this.mvt.updatePosition(map, cellId);
	}

	// determine si l'inventaire est plein ou pas selon le pourcentage donn�
	protected boolean inventoryIsSoHeavy(float percentage) { // percentage < 1
		if(this.infos.weight > this.infos.weightMax * percentage)
			return true;
		return false;
	}

	// envoie une requ�te WHOIS pour savoir si le mod�rateur du serveur est en ligne
	protected void checkIfModeratorIsOnline(String moderatorName) {
		BasicWhoIsRequestMessage BWIRM = new BasicWhoIsRequestMessage();
		BWIRM.verbose = true;
		BWIRM.search = moderatorName;
		this.net.send(BWIRM);
		this.log.p("Checking if moderator is online.");
		waitState(CharacterState.WHOIS_RESPONSE);
	}

	// seul le thread de traitement entre ici
	public synchronized void updateState(CharacterState state, boolean newState) {
		this.log.p("State updated : " + state + " = " + newState + ".");
		this.states.put(state, newState);
		notify();
	}

	public boolean inState(CharacterState state) {
		return this.states.get(state);
	}

	// seul le contr�leur entre ici
	public boolean waitState(CharacterState state, int timeout) {
		Condition condition = null;
		boolean isEvent = false;
		boolean forbiddenTimeout = false;
		switch(state) {
			case IS_FREE : // �tat compos�
				this.log.p("Waiting for character to be free.");
				condition = new Condition(CharacterState.IS_LOADED, 60000);
				condition.addSecondState(CharacterState.IN_EXCHANGE, false);
				forbiddenTimeout = true;
				break;
			case IN_GAME_TURN : // �tat avec contrainte
				this.log.p("Waiting for my game turn.");
				condition = new Condition(state, 0); // mort du perso dans le combat
				condition.addConstraint(CharacterState.IN_FIGHT, false);
				break;
			case IS_LOADED : // �tat simple
				this.log.p("Waiting for character to be loaded.");
				condition = new Condition(state, 60000);
				forbiddenTimeout = true;
				break;
			case PENDING_DEMAND : // �tat simple
				this.log.p("Waiting for exchange demand.");
				condition = new Condition(state, 0);
				condition.addConstraint(CharacterState.SHOULD_DECONNECT, true); // tant qu'on ne re�oit pas d'ordre de d�connexion
				break;
			case MULE_ONLINE : // �tat simple
				this.log.p("Waiting for mule connection.");
				condition = new Condition(state, 0);
				condition.addConstraint(CharacterState.SHOULD_DECONNECT, true); // tant qu'on ne re�oit pas d'ordre de d�connexion
				break;
			case MULE_AVAILABLE : // �tat simple
				this.log.p("Waiting for mule to be available.");
				condition = new Condition(state, 0);
				condition.addConstraint(CharacterState.SHOULD_DECONNECT, true); // tant qu'on ne re�oit pas d'ordre de d�connexion
				break;
			case CAN_MOVE : // �tat simple
				this.log.p("Waiting for movement acceptation by server.");
				condition = new Condition(state, 30000);
				forbiddenTimeout = true;
				break;
			case IN_PARTY : // �tat simple
				this.log.p("Waiting for joining party.");
				condition = new Condition(state, 30000);
				forbiddenTimeout = true;
				break;
			case NOT_IN_PARTY : // �tat abstrait inverse (abstrait = qui n'exste pas)
				this.log.p("Waiting for leaving party.");
				condition = new Condition(CharacterState.IN_PARTY, false, 30000);
				forbiddenTimeout = true;
				break;
			case IN_FIGHT : // attente avec timeout autoris�
				this.log.p("Waiting for fight beginning.");
				condition = new Condition(state, 5000);
				break;
			case IN_EXCHANGE : // attente avec timeout autoris�
				this.log.p("Waiting for exchange acceptance.");
				condition = new Condition(state, 5000);
				break;
			case DIALOG_DISPLAYED : // attente
				this.log.p("Waiting for dialog to be displayed.");
				condition = new Condition(state, 30000);
				forbiddenTimeout = true;
				break;
			case IN_REGENERATION : // attente inverse avec timeout donn� (on attend juste la fin du timeout)
				this.log.p("Waiting for regeneration to be completed.");
				this.states.put(state, true);
				condition = new Condition(state, false, timeout);
				condition.addConstraint(CharacterState.SHOULD_DECONNECT, true); // tant qu'on ne re�oit pas d'ordre de d�connexion
				isEvent = true; // pas un event en fait
				break;
			case NEW_ACTOR_ON_MAP : // event
				this.log.p("Waiting for new actor on the map.");
				condition = new Condition(state, 0);
				isEvent = true;
				break;
			case EXCHANGE_VALIDATED : // event (inutilis� actuellement)
				this.log.p("Waiting for exchange validation.");
				condition = new Condition(state, 60000);
				condition.addConstraint(CharacterState.SHOULD_DECONNECT, true); // tant qu'on ne re�oit pas d'ordre de d�connexion
				isEvent = true;
				break;
			case CAPTAIN_ACT : // event
				this.log.p("Waiting for captain act.");
				condition = new Condition(state, 0);
				condition.addConstraint(CharacterState.SHOULD_DECONNECT, true); // tant qu'on ne re�oit pas d'ordre de d�connexion
				isEvent = true;
				break;
			case SOLDIER_ACT : // event
				this.log.p("Waiting for soldier act.");
				condition = new Condition(state, 0);
				condition.addConstraint(CharacterState.SHOULD_DECONNECT, true); // tant qu'on ne re�oit pas d'ordre de d�connexion
				isEvent = true;
				break;
			case NEW_PARTY_MEMBER : // event
				this.log.p("Waiting for party invitation acceptation.");
				condition = new Condition(state, 30000);
				isEvent = true;
				forbiddenTimeout = true;
				break;
			case FIGHT_LAUNCHED : // event
				this.log.p("Waiting for fight be launched by captain.");
				condition = new Condition(state, 30000);
				isEvent = true;
				forbiddenTimeout = true;
				break;
			case WHOIS_RESPONSE : // event
				this.log.p("Waiting for WHOIS response.");
				condition = new Condition(state, 30000);
				isEvent = true;
				forbiddenTimeout = true;
				break;
			case BANK_TRANSFER : // event
				this.log.p("Waiting for bank transfer to be done.");
				condition = new Condition(state, 30000);
				isEvent = true;
				forbiddenTimeout = true;
				break;
			case EXCHANGE_DEMAND_OUTCOME : // event
				this.log.p("Waiting for exchange demand outcome.");
				condition = new Condition(state, 5000);
				isEvent = true;
				break;
			case SPELL_CASTED : // event avec contrainte
				this.log.p("Waiting for result of spell cast.");
				condition = new Condition(state, 10000);
				condition.addConstraint(CharacterState.IN_GAME_TURN, false); // tant que le tour de jeu n'est pas termin�
				isEvent = true;
				break;
			case NEW_ACTOR_IN_FIGHT : // event avec contrainte
				this.log.p("Waiting for soldier join fight.");
				condition = new Condition(state, 0);
				condition.addConstraint(CharacterState.IN_GAME_TURN, true);
				isEvent = true;
				break;
			case LEVEL_UP : // �tat ponctuel
			case NEED_TO_EMPTY_INVENTORY : // �tat ponctuel
			case SHOULD_DECONNECT : // �tat ponctuel
				throw new FatalError("Impossible to wait the one-time state : " + state + ".");
		}
		boolean result = waitFor(condition);
		if(isEvent)
			this.states.put(condition.expectedState, false);
		if(isInterrupted())
			return false;
		if(forbiddenTimeout && !result) // si le timeout est interdit pour cet �tat
			throw new FatalError("Timeout reached and forbidden for the state : " + state + ".");
		return result;
	}

	public boolean waitState(CharacterState state) {
		return waitState(state, 0);
	}

	// retourne false lors d'un timeout, d'une contrainte vraie ou d'une interruption
	private synchronized boolean waitFor(Condition condition) {
		boolean infiniteWaiting = condition.timeout == 0;
		if(infiniteWaiting)
			condition.timeout = 120000; // 2 minutes
		long startTime = System.currentTimeMillis();
		long currentTime;
		while(!isInterrupted()) { // n'entre m�me pas dans la boucle si le thread est en cours d'interruption
			while((currentTime = System.currentTimeMillis() - startTime) < condition.timeout) {
				if(!condition.type) {
					// condition � un �tat
					if(this.states.get(condition.expectedState) == condition.expectedValueForExpectedState)
						return true;
					// condition � un �tat avec une contrainte
					if(condition.otherState != null && this.states.get(condition.otherState) == condition.expectedValueForOtherState)
						return false;
				}
				else // condition � 2 �tats
					if(this.states.get(condition.expectedState) == condition.expectedValueForExpectedState && this.states.get(condition.otherState) == condition.expectedValueForOtherState)
						return true;
				try {
					wait(condition.timeout - currentTime);
				} catch(Exception e) {
					interrupt();
					return false;
				}
			}
			if(infiniteWaiting) {
				//sendPingRequest();
				if(this.infos.isConnected)
					checkIfModeratorIsOnline(Main.MODERATOR_NAME); // requ�te effectu�e toutes les 2 minutes
				startTime = System.currentTimeMillis();
			}
			else {
				this.log.p("TIMEOUT");
				return false; // si on ne l'a pas re�u � temps
			}
		}
		return false;
	}

	private static class Condition {
		private CharacterState expectedState;
		private boolean expectedValueForExpectedState;
		private CharacterState otherState;
		private boolean expectedValueForOtherState;
		private int timeout;
		private boolean type; // true = second state, false = simple state or constraint state

		private Condition(CharacterState state, boolean expectedValue, int timeout) {
			this.expectedState = state;
			this.expectedValueForExpectedState = expectedValue;
			this.timeout = timeout;
			this.otherState = null;
			this.type = false;
		}

		private Condition(CharacterState state, int timeout) {
			this(state, true, timeout);
		}

		private void addSecondState(CharacterState state, boolean expectedValue) {
			this.otherState = state;
			this.expectedValueForOtherState = expectedValue;
			this.type = true;
		}

		private void addConstraint(CharacterState state, boolean expectedValue) {
			this.otherState = state;
			this.expectedValueForOtherState = expectedValue;
			this.type = false;
		}
	}
}