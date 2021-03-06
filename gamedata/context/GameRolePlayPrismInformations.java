package gamedata.context;

import gamedata.ProtocolTypeManager;
import utilities.ByteArray;

public class GameRolePlayPrismInformations extends GameRolePlayActorInformations {
    public PrismInformation prism;

	public GameRolePlayPrismInformations(ByteArray buffer) {
		super(buffer);
        this.prism = (PrismInformation) ProtocolTypeManager.getInstance(buffer.readShort(), buffer);
	}
}