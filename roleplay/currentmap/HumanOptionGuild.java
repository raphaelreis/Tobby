package roleplay.currentmap;

import utilities.ByteArray;

public class HumanOptionGuild extends HumanOption {
	public GuildInformations guildInformations;
	
	public HumanOptionGuild(ByteArray buffer) {
		super(buffer);
		this.guildInformations = new GuildInformations(buffer);
	}
}
