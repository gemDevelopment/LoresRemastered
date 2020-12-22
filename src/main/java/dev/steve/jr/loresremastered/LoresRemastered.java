package dev.steve.jr.loresremastered;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LoresRemastered extends JavaPlugin implements CommandExecutor {
    private static enum Action { NAME, OWNER, ADD, DELETE, SET, INSERT, CLEAR, UNDO }
    private static final HashMap<String, LinkedList<ItemStack>> undo = new HashMap<String, LinkedList<ItemStack>>();
    char[] colorCodes = {
            '0', '1', '2', '3', '4',
            '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e',
            'f', 'l', 'n', 'o', 'k', 'm', 'r'
    };

    @Override
    public void onEnable () {
        //Register the lore command
        getCommand("lore").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        //This Command is only useful to Players
        if (!(sender instanceof Player)) {
            sendHelp(sender);
            return true;
        }
        Player player = (Player) sender;

        ItemStack item = player.getEquipment().getItemInMainHand();

        //Retrieve the meta and make one if it doesn't exist
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
            if (meta == null) {
                player.sendMessage("§4The Item you are holding does not support Lore");
                return true;
            }
        }

        //There must be at least one argument
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        //Retrieve the lore and make one if it doesn't exist
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new LinkedList<String>();
        }

        //Discover the action to be executed
        Action action;
        try {
            action = Action.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException notEnum) {
            sendHelp(sender);
            return true;
        }

        //Generate the String id for the Player/Item
        String id = player.getName() + "'" + item.getType().name();

        if (action != Action.UNDO) {
            //Create an undo List for the Player if there isn't one
            if (!undo.containsKey(id)) {
                undo.put(id, new LinkedList<ItemStack>());
            }

            //Add the meta state to the undo List
            LinkedList<ItemStack> list = undo.get(id);
            list.addFirst(item.clone());

            //Don't allow the undo List to grow too large
            while (list.size() > 5) {
                list.removeLast();
            }
        }

        switch (action) {
            case NAME: //Change the Name of the Item
                if (!sender.hasPermission("lores.name") || args.length < 2) {
                    sendHelp(sender);
                    return true;
                }

                String name = concatArgs(sender, args, 1);
                if (name.contains("|")) {
                    int max = name.replaceAll("§[0-9a-klmnor]", "").length();
                    Iterator<String> itr = lore.iterator();
                    while (itr.hasNext()) {
                        max = Math.max(max, itr.next().replaceAll("§[0-9a-klmnor]", "").length());
                    }
                    int spaces = max - name.replaceAll("§[0-9a-klmnor]", "").length() - 1;
                    String space = " ";
                    for (int i = 1; i < spaces * 1.5; i++) {
                        space += ' ';
                    }
                    name = name.replace("|", space);
                }

                meta.setDisplayName(name);
                break;

            case OWNER: //Change the Owner of the Skull
                if (!sender.hasPermission("lores.owner") || args.length < 2) {
                    sendHelp(sender);
                    return true;
                }

                if (!(meta instanceof SkullMeta)) {
                    player.sendMessage("§4You may only set the Owner of a §6Skull");
                    return true;
                }

                ((SkullMeta) meta).setOwner(args[1]);
                break;

            case ADD: //Add a line to the end of the lore
                if (!sender.hasPermission("lores.lore") || args.length < 2) {
                    sendHelp(sender);
                    return true;
                }

                lore.add(concatArgs(sender, args, 1));
                break;

            case DELETE: //Delete a line of the lore
                if (!sender.hasPermission("lores.lore")) {
                    sendHelp(sender);
                    return true;
                }

                switch (args.length) {
                    case 1: //Delete the last line
                        if (lore.size() < 1) {
                            player.sendMessage("§4There is nothing to delete!");
                            return true;
                        }

                        lore.remove(lore.size() - 1);
                        break;

                    case 2: //Delete specified line
                        //Ensure that the argument is an Integer
                        int index;
                        try {
                            index = Integer.parseInt(args[1]) - 1;
                        } catch (Exception e) {
                            return false;
                        }

                        //Ensure that the lore is large enough
                        if (lore.size() <= index || index < 0) {
                            player.sendMessage("§4Invalid line number!");
                            return true;
                        }

                        lore.remove(index);
                        break;

                    default: return false;
                }
                break;

            case SET: //Change a line of the lore
                if (!sender.hasPermission("lores.lore") || args.length < 3) {
                    sendHelp(sender);
                    return true;
                }

                //Ensure that the argument is an Integer
                int index;
                try {
                    index = Integer.parseInt(args[1]) - 1;
                } catch (Exception e) {
                    return false;
                }

                //Ensure that the lore is large enough
                if (lore.size() <= index || index < 0) {
                    player.sendMessage("§4Invalid line number!");
                    return true;
                }

                lore.set(index, concatArgs(sender, args, 2));
                break;

            case INSERT: //Insert a line into the lore
                if (!sender.hasPermission("lores.lore") || args.length < 3) {
                    sendHelp(sender);
                    return true;
                }

                //Ensure that the argument is an Integer
                int i;
                try {
                    i = Integer.parseInt(args[1]) - 1;
                } catch (Exception e) {
                    return false;
                }

                //Ensure that the lore is large enough
                if (lore.size() <= i || i < 0) {
                    player.sendMessage("§4Invalid line number!");
                    return true;
                }

                lore.add(i, concatArgs(sender, args, 2));
                break;

            case CLEAR:
                if (!sender.hasPermission("lores.lore") || args.length != 1) {
                    sendHelp(sender);
                    return true;
                }

                lore.clear();
                break;

            case UNDO:
                //Ensure that we are given the correct number of arguments
                if (args.length != 1) {
                    return false;
                }

                //Retrieve the old ItemStack from the undo List
                LinkedList<ItemStack> list = undo.get(id);
                if (list == null) {
                    player.sendMessage("§4This item has not been modified yet!");
                    return true;
                }
                if (list.size() < 1) {
                    player.sendMessage("§4You cannot continue to undo for this Item!");
                    return true;
                }

                ItemStack undoneItem = list.removeFirst();
                if (!item.isSimilar(undoneItem) && item.getType() != Material.PLAYER_HEAD) {
                    player.sendMessage("§4You have not yet modified this Item!");
                    return true;
                }

                //Dupe fix
                int stackSize = item.getAmount();
                if (undoneItem.getAmount() != stackSize) {
                    undoneItem.setAmount(stackSize);
                }

                //Place the old Item in the Player's hand
                player.getEquipment().setItemInMainHand(undoneItem);
                player.sendMessage("§5The last modification you made on this item has been undone!");
                return true;
        }

        //Set the new lore/meta
        meta.setLore(lore);
        item.setItemMeta(meta);
        player.sendMessage("§5Lore successfully modified!");
        return true;
    }

    /**
     * Concats arguments together to create a sentence from words
     * This also replaces & with § to add color codes to
     * the String if the sender has the needed permissions
     *
     * @param sender the Player concating
     * @param args the arguments to concat
     * @param first Which argument should the sentence start with
     * @return The new String that was created
     */
    private static String concatArgs(CommandSender sender, String[] args, int first) {
        StringBuilder sb = new StringBuilder();
        if (first > args.length) {
            return "";
        }
        for (int i = first; i <= args.length - 1; i++) {
            sb.append(" ");
            //sb.append(ChatColor.translateAlternateColorCodes('&', args[i]));
            sb.append(format(args[i]));
        }
        String string = sb.substring(1);

        char[] charArray = string.toCharArray();
        boolean modified = false;
        for (int i = 0; i < charArray.length; i++) {
            if (charArray[i] == ChatColor.COLOR_CHAR) {
                if (!sender.hasPermission("lores.color." + charArray[i + 1])) {
                    charArray[i] = '?';
                    modified = true;
                }
            }
        }

        return modified ? String.copyValueOf(charArray) : string;
    }

    /**
     * Displays the Lores Help Page to the given Player
     *
     */
    private static void sendHelp(CommandSender sender) {
        sender.sendMessage("§e     Lores Help Page:");
        sender.sendMessage("§4Each command will modify the Item in your hand");
        if (sender.hasPermission("lores.color") || sender.hasPermission("lores.format")) {
            sender.sendMessage("§4Use §c& §4to add color with any command");
        }
        if (sender.hasPermission("lores.name")) {
            sender.sendMessage(format("&4/lore name <custom name> &7Set the new Name of the Item"));
        }
        if (sender.hasPermission("lores.owner")) {
            sender.sendMessage(format("&4/lore owner <player> &7Change the Owner of a Skull"));
        }
        if (sender.hasPermission("lores.lore")) {
            sender.sendMessage(format("&4/lore add <line of text> &7Add a line to the lore"));
            sender.sendMessage(format("&4/lore set <line #> <line of text> §7Change a line of the lore"));
            sender.sendMessage(format("&4/lore insert <line #> <line of text> §7Insert a line into the lore"));
            sender.sendMessage(format("&4/lore delete [line #] §7Delete a line of the lore (last line by default)"));
            sender.sendMessage(format("&4/lore clear §7Clear all lines of the lore"));
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&4/lore undo §7Undoes your last modification (up to 5 times) §cX"));
    }
    private static final Pattern pattern = Pattern.compile("&#[a-fA-F0-9]{6}");

    private static String format(String msg) {
        if (Bukkit.getVersion().contains("1.16")) {
            Matcher match = pattern.matcher(msg);
            while (match.find()) {
                String color = msg.substring(match.start(), match.end());
                msg = msg.replace(color, ChatColor.of(color.replace("&#","#")) + "");
                match = pattern.matcher(msg);
            }
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}