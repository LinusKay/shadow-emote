package shadowemote.main;

import org.bukkit.plugin.java.JavaPlugin;

import shadowemote.command.Emote;

public class Main extends JavaPlugin{
	 
	@Override
	public void onEnable() {
		registerCommands();
		registerConfig();
	}
	
	public void registerCommands() {
		getCommand("emote").setExecutor(new Emote(this)); 
	}
	
	public void registerConfig() {
		getConfig().options().copyDefaults(true);
		saveConfig();
	}
	

	
}
