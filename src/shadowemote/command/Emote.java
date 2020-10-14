package shadowemote.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import shadowemote.main.Main;

public class Emote implements CommandExecutor, Listener{

	private Main plugin;
	private String prefix, requireonline, specifyemote, bannedphrase, noperm, playeroffline, missingargs, invalidemote, listformat, listtitle, cooldownmsg, reloaded;
	private int cooldowntime;
	public Emote(Main pl) {
		plugin = pl;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		requireonline = plugin.getConfig().getString("options.require-online");
		prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));
		reloaded = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.reloaded"));
		specifyemote = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.specify-emote"));
		bannedphrase = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.banned-phrase"));
		noperm = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-permission"));
		playeroffline = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.player-offline"));
		missingargs = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.missing-arguments"));
		invalidemote = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.invalid-emote"));
		listformat = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.emote-list-format"));
		listtitle = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.emote-list-title"));
		cooldownmsg = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.cooldown-message"));
		cooldowntime = Integer.parseInt(plugin.getConfig().getString("options.cooldown-time"));
	}
	
	
	//CREATE COOLDOWN HASHMAP
		private Map<UUID, Long> COOLDOWN = new HashMap<>();
		
	//REQUIRE AT LEAST ONE ARGUMENT
	public boolean argCheck(String[] args, Player p) {
		if(args.length < 1) {
			p.sendMessage(prefix + specifyemote);
			return false;
		}
		return true;
	}
	
	//CHECK FILTER
	public boolean filterCheck(Player p, String s) {
		List<String>blacklist = plugin.getConfig().getStringList("options.banned-phrases");
		for(String str : blacklist) {
			if(s.contains(str)) {
				p.sendMessage(prefix+bannedphrase);			
				return false;
			}
		}
		return true;
	}
	
	//JOIN ARGS	
		public String argJoin(String[] args) {
			StringBuilder buffer = new StringBuilder();
			for(int i = 1; i < args.length; i++)
			{
			    buffer.append(' ').append(args[i]);
			}		
			return buffer.toString();
		}
	
	//REPLACE PLACEHOLDERS
		public String strReplace(Player p, String t, String str, String buffer) {
			str = str.replace("PLAYER", 	p.getName())
					 .replace("USER", 	p.getName())
				 	 .replace("USERNAME", p.getName())
					 .replace("TARGET",   t)
					 .replace("ARGS", buffer);
			return str.trim();
		}
		
	@SuppressWarnings("deprecation")
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String str, String[] args) {
		
		if (!(sender instanceof Player)) {
			sender.sendMessage("Only players may execute this command!");
			return true;
		}
		
		Player p = (Player) sender;
		
		//EMOTE RELOAD (reloads plugin config)
		if(p.hasPermission("emote.reload")) {
			if(args[0].equalsIgnoreCase("reload")) {
				plugin.reloadConfig();
				plugin.saveConfig();
				p.sendMessage(prefix + reloaded);
				return true;
			}
		}
		
		try {
		if(!p.hasPermission("emote.use")) {
			p.sendMessage(prefix + noperm.replace("PERMISSION", "emote.use"));
			return false;
		}
		
		
		
		//ensure more than 0 args
		if(!argCheck(args, p)) {
			return false;
		}
		
		//EMOTE LIST
		//If first argument is list or help, list all emotes
		if(args[0].equalsIgnoreCase("list") | args[0].equalsIgnoreCase("help")) {
			if(!p.hasPermission("emote.list")) {
				p.sendMessage(prefix + noperm.replace("PERMISSION", "emote.list"));
				return false;
			}
			String emotelist = "";
			for (String emote : plugin.getConfig().getConfigurationSection("emotes").getKeys(false)) {
				emotelist = emotelist + listformat.replace("EMOTE", emote).replace("TEMPLATE", ChatColor.translateAlternateColorCodes('&',plugin.getConfig().getString("emotes."+emote+".template")))+"\n";

			}
			p.sendMessage(prefix + listtitle + "\n" + emotelist);
			return true;
		}
		
		
		
		//If emote requires permission, check if player has it
		if(plugin.getConfig().getString("emotes."+args[0]+".permission-required").equalsIgnoreCase("true")) {
			String perm = plugin.getConfig().getString("emotes."+args[0]+".permission");
			if(!(p.hasPermission(perm))){
				p.sendMessage(prefix + noperm.replace("PERMISSION", perm));
				return false;
			}
		}
		
		
		//grab template from config
		String template = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("emotes."+args[0]+".template"));
		String buffer = argJoin(args).trim();
		
		//set target
		String target = "";
		//added player variable here so it doesnt need to be redeclared later
		Player t = null;
		if(template.contains("TARGET")) {
			if(requireonline.equalsIgnoreCase("true")) {
				try {
				t = p.getServer().getPlayer(args[1]);
				target = t.getName();
				} catch (NullPointerException e) {
					p.sendMessage(prefix + playeroffline);
					return false;
				}
			}
			else {
				target = args[0];
			}
		}
		
		//PUSH PLAYER (push-target)
		Boolean push = Boolean.parseBoolean(plugin.getConfig().getString("emotes."+args[0]+".push-target"));
		String pushdistance = plugin.getConfig().getString("emotes."+args[0]+".push-distance");
		//If push-target enabled
			if(push) {
				//if push-distance set 
				if(pushdistance != null) {
					//parse push distance to integer 
					int distance = Integer.parseInt(pushdistance);
					//push player backwards using the distance in config (honestly my math here is super janky and likely inaccurate for higher numbers)
					t.setVelocity(t.getLocation().getDirection().multiply(-distance/2.5));
				} else {
					//if no distance is set, push back 2 blocks 
					t.setVelocity(t.getLocation().getDirection().multiply(-0.4));
				}
			}
		
		
		
		//PARTICLE EFFECTS (effect, target-effect, target-effect-range)
		String effect = plugin.getConfig().getString("emotes."+args[0]+".effect");
		String targeteffect = plugin.getConfig().getString("emotes."+args[0]+".target-effect");
		String targeteffectrange = plugin.getConfig().getString("emotes."+args[0]+".target-effect-range");
		Location loc = p.getLocation();
		//if emote effect is set, spawn effect on player
		if(effect != null) {
			World w = p.getWorld();
			loc.add(0,2,0);
			w.spawnParticle(Particle.valueOf(effect) , loc, 3, 0.2, 0.2, 0.2);
		}
		//If a target effect is set, spawn effect on target player
		if(targeteffect != null) {
			//if a range is specified in config
			if(targeteffectrange != null) {
				//convert range from string to int
				int range = Integer.parseInt(targeteffectrange);
				//compile all entities within the range of the player into a list
				List<Entity> nearbyEntities = (List<Entity>) loc.getWorld().getNearbyEntities(loc, range, range, range);
				//run through each entity in list
				for(Entity e : nearbyEntities) {
					//if entity is a player
					if(e instanceof Player) {
						//if the player's name matches the one specified in the command
						if(e.getName().equalsIgnoreCase(args[1])) {
							World w = t.getWorld();
							loc = t.getLocation().add(0,2,0);
							w.spawnParticle(Particle.valueOf(targeteffect) , loc, 3, 0.2, 0.2, 0.2);
						}
					}
				}
			} else {
				//if a range is not specified, just run the particles 
				World w = t.getWorld();
				loc = t.getLocation().add(0,2,0);
				w.spawnParticle(Particle.valueOf(targeteffect) , loc, 3, 0.2, 0.2, 0.2);
			}
				
		}
		
		//RUN COMMAND (run-command)
		String command = plugin.getConfig().getString("emotes."+args[0]+".run-command");
		//if run-command is set in config
		if(command != null) {
			command.replace("PLAYER", p.getName());
			//force player to run command
			p.performCommand(command);
		}
		
		//RUN CONSOLE COMMAND (run-console-command)
		command = plugin.getConfig().getString("emotes."+args[0]+".run-console-command");
		if(command != null) {
			command.replace("PLAYER", p.getName());
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
		}
		
		//replace placeholders with words
		template = strReplace(p, target, template, buffer);
		
		//filter for blocked words
		if(!filterCheck(p, template)) {
			return false;
		}
		
		//COOLDOWN
		if(!(p.hasPermission("emote.cooldown"))) {
		   Long cooldown = COOLDOWN.get(p.getUniqueId());
	        long now = System.currentTimeMillis();
	        
	        //If player is still in cooldown, send message with time
	        if (cooldown != null && cooldown > now) {
	            p.sendMessage(prefix + cooldownmsg.replace("COOLDOWNTIME", ""+TimeUnit.MILLISECONDS.toSeconds(cooldown-now)));
	            return true;
	        }
	        //If player is not in cooldown, add them
	        COOLDOWN.put(p.getUniqueId(), now + TimeUnit.SECONDS.toMillis(cooldowntime));//cooldownTime is set in config.yml
		}
		
		//final broadcast
		p.getServer().broadcastMessage(prefix + template);
		
		} catch (ArrayIndexOutOfBoundsException e) {
			sender.sendMessage(prefix+missingargs);
			return false;
		}
		  catch (NullPointerException n) {
			sender.sendMessage(prefix+invalidemote);
			return false;
		}
		
		return false;
	}
	
}
