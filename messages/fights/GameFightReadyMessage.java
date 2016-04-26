package messages.fights;

import messages.Message;

public class GameFightReadyMessage extends Message {
	public boolean isReady = false;
	
	@Override
	public void serialize() {
		this.content.writeBoolean(this.isReady);
	}
	
	@Override
	public void deserialize() {
		// not implemented yet
	}
}