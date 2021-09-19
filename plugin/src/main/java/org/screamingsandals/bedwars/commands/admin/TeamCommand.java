package org.screamingsandals.bedwars.commands.admin;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.screamingsandals.bedwars.BedWarsPlugin;
import org.screamingsandals.bedwars.commands.AdminCommand;
import org.screamingsandals.bedwars.game.TeamImpl;
import org.screamingsandals.bedwars.game.TeamColorImpl;
import org.screamingsandals.bedwars.lang.LangKeys;
import org.screamingsandals.bedwars.region.FlatteningBedUtils;
import org.screamingsandals.bedwars.region.LegacyBedUtils;
import org.screamingsandals.bedwars.utils.ArenaUtils;
import org.screamingsandals.lib.entity.EntityHuman;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.PlayerWrapper;
import org.screamingsandals.lib.sender.CommandSenderWrapper;
import org.screamingsandals.lib.utils.AdventureHelper;
import org.screamingsandals.lib.utils.annotations.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TeamCommand extends BaseAdminSubCommand {
    public TeamCommand() {
        super("team");
    }

    @Override
    public void construct(CommandManager<CommandSenderWrapper> manager, Command.Builder<CommandSenderWrapper> commandSenderWrapperBuilder) {
        manager.command(
                commandSenderWrapperBuilder
                        .literal("add")
                        .argument(StringArgument.of("teamName"))
                        .argument(EnumArgument.of(TeamColorImpl.class, "color"))
                        .argument(IntegerArgument
                                .<CommandSenderWrapper>newBuilder("maximumPlayers")
                                .withMin(1)
                        )
                        .handler(commandContext -> editMode(commandContext, (sender, game) -> {
                            String name = commandContext.get("teamName");
                            TeamColorImpl color = commandContext.get("color");
                            int maxPlayers = commandContext.get("maximumPlayers");

                            if (game.getTeamFromName(name) != null) {
                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_TEAM_ALREADY_EXISTS).defaultPrefix());
                                return;
                            }

                            if (maxPlayers < 1) {
                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_MAX_PLAYERS).defaultPrefix());
                                return;
                            }

                            var team = new TeamImpl();
                            team.setName(name);
                            team.setColor(color);
                            team.setMaxPlayers(maxPlayers);
                            team.setGame(game);
                            game.getTeams().add(team);

                            sender.sendMessage(
                                    Message
                                            .of(LangKeys.ADMIN_ARENA_EDIT_SUCCESS_TEAM_CREATED)
                                            .defaultPrefix()
                                            .placeholder("team", team.getName())
                                            .placeholder("teamcolor", AdventureHelper.toComponent(team.getColor().chatColor + team.getColor().name()))
                                            .placeholder("maxplayers", team.getMaxPlayers())
                            );
                        }))
        );

        var teamNameArgument = StringArgument
                .<CommandSenderWrapper>newBuilder("teamName")
                .withSuggestionsProvider((c, s) -> {
                    if (AdminCommand.gc.containsKey(c.<String>get("game"))) {
                        return AdminCommand.gc.get(c.<String>get("game"))
                                .getTeams()
                                .stream()
                                .map(TeamImpl::getName)
                                .collect(Collectors.toList());
                    }
                    return List.of();
                });


        manager.command(
                commandSenderWrapperBuilder
                        .literal("remove")
                        .argument(teamNameArgument)
                        .handler(commandContext -> editMode(commandContext, (sender, game) -> {
                            String name = commandContext.get("teamName");

                            var forRemove = game.getTeamFromName(name);
                            if (forRemove != null) {
                                game.getTeams().remove(forRemove);

                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_SUCCESS_TEAM_REMOVED).defaultPrefix().placeholder("team", forRemove.getName()));
                                return;
                            }
                            sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_TEAM_DOES_NOT_EXIST).defaultPrefix());
                        }))
        );

        manager.command(
                commandSenderWrapperBuilder
                        .literal("color")
                        .argument(teamNameArgument)
                        .argument(EnumArgument.of(TeamColorImpl.class, "color"))
                        .handler(commandContext -> editMode(commandContext, (sender, game) -> {
                            String name = commandContext.get("teamName");
                            TeamColorImpl color = commandContext.get("color");

                            var team = game.getTeamFromName(name);
                            if (team == null) {
                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_TEAM_DOES_NOT_EXIST).defaultPrefix());
                                return;
                            }

                            team.setColor(color);

                            sender.sendMessage(
                                    Message
                                            .of(LangKeys.ADMIN_ARENA_EDIT_SUCCESS_TEAM_COLOR_SET)
                                            .defaultPrefix()
                                            .placeholder("team", team.getName())
                                            .placeholder("teamcolor", AdventureHelper.toComponent(team.getColor().chatColor + team.getColor().name()))
                            );
                        }))
        );

        manager.command(
                commandSenderWrapperBuilder
                        .literal("maxplayers")
                        .argument(teamNameArgument)
                        .argument(IntegerArgument
                                .<CommandSenderWrapper>newBuilder("maximumPlayers")
                                .withMin(1)
                        )
                        .handler(commandContext -> editMode(commandContext, (sender, game) -> {
                            String name = commandContext.get("teamName");
                            int maxPlayers = commandContext.get("maximumPlayers");

                            var team = game.getTeamFromName(name);
                            if (team == null) {
                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_TEAM_DOES_NOT_EXIST).defaultPrefix());
                                return;
                            }

                            if (maxPlayers < 1) {
                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_MAX_PLAYERS).defaultPrefix());
                                return;
                            }

                            team.setMaxPlayers(maxPlayers);

                            sender.sendMessage(
                                    Message
                                            .of(LangKeys.ADMIN_ARENA_EDIT_SUCCESS_TEAM_MAXPLAYERS_SET)
                                            .defaultPrefix()
                                            .placeholder("team", team.getName())
                                            .placeholder("maxplayers", team.getMaxPlayers()));
                        }))
        );

        manager.command(
                commandSenderWrapperBuilder
                        .literal("spawn")
                        .argument(teamNameArgument)
                        .handler(commandContext -> editMode(commandContext, (sender, game) -> {
                            String name = commandContext.get("teamName");

                            var loc = sender.as(PlayerWrapper.class).getLocation();

                            var team = game.getTeamFromName(name);
                            if (team == null) {
                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_TEAM_DOES_NOT_EXIST).defaultPrefix());
                                return;
                            }

                            if (game.getPos1() == null || game.getPos2() == null) {
                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_SET_BOUNDS_FIRST).defaultPrefix());
                                return;
                            }
                            if (!game.getWorld().equals(loc.getWorld())) {
                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_MUST_BE_IN_SAME_WORLD).defaultPrefix());
                                return;
                            }
                            if (!ArenaUtils.isInArea(loc, game.getPos1(), game.getPos2())) {
                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_MUST_BE_IN_BOUNDS).defaultPrefix());
                                return;
                            }
                            team.setTeamSpawn(loc);
                            sender.sendMessage(
                                    Message
                                            .of(LangKeys.ADMIN_ARENA_EDIT_SUCCESS_TEAM_SPAWN_SET)
                                            .defaultPrefix()
                                            .placeholder("team", team.getName())
                                            .placeholder("x", team.getTeamSpawn().getX(), 2)
                                            .placeholder("y", team.getTeamSpawn().getY(), 2)
                                            .placeholder("z", team.getTeamSpawn().getZ(), 2)
                                            .placeholder("yaw", team.getTeamSpawn().getYaw(), 5)
                                            .placeholder("pitch", team.getTeamSpawn().getPitch(), 5)
                            );
                        }))
        );

        manager.command(
                commandSenderWrapperBuilder
                        .literal("bed", "block")
                        .argument(teamNameArgument)
                        .handler(commandContext -> editMode(commandContext, (sender, game) -> {
                            String name = commandContext.get("teamName");

                            var block = sender.as(EntityHuman.class).getTargetBlock(null, 5);
                            var loc = block.getLocation();

                            var team = game.getTeamFromName(name);
                            if (team == null) {
                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_TEAM_DOES_NOT_EXIST).defaultPrefix());
                                return;
                            }

                            if (game.getPos1() == null || game.getPos2() == null) {
                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_SET_BOUNDS_FIRST).defaultPrefix());
                                return;
                            }
                            if (!game.getWorld().equals(loc.getWorld())) {
                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_MUST_BE_IN_SAME_WORLD).defaultPrefix());
                                return;
                            }
                            if (!ArenaUtils.isInArea(loc, game.getPos1(), game.getPos2())) {
                                sender.sendMessage(Message.of(LangKeys.ADMIN_ARENA_EDIT_ERRORS_MUST_BE_IN_BOUNDS).defaultPrefix());
                                return;
                            }

                            if (BedWarsPlugin.isLegacy()) {
                                // Legacy
                                if (block.as(Block.class).getState().getData() instanceof org.bukkit.material.Bed) {
                                    org.bukkit.material.Bed bed = (org.bukkit.material.Bed) block.as(Block.class).getState().getData();
                                    if (!bed.isHeadOfBed()) {
                                        team.setTargetBlock(LegacyBedUtils.getBedNeighbor(block).getLocation());
                                    } else {
                                        team.setTargetBlock(loc);
                                    }
                                } else {
                                    team.setTargetBlock(loc);
                                }

                            } else {
                                // 1.13+
                                if (block.as(Block.class).getBlockData() instanceof Bed) {
                                    Bed bed = (Bed) block.as(Block.class).getBlockData();
                                    if (bed.getPart() != Bed.Part.HEAD) {
                                        team.setTargetBlock(Objects.requireNonNull(FlatteningBedUtils.getBedNeighbor(block)).getLocation());
                                    } else {
                                        team.setTargetBlock(loc);
                                    }
                                } else {
                                    team.setTargetBlock(loc);
                                }
                            }
                            sender.sendMessage(
                                    Message
                                            .of(LangKeys.ADMIN_ARENA_EDIT_SUCCESS_TARGET_BLOCK_SET)
                                            .defaultPrefix()
                                            .placeholder("team", team.getName())
                                            .placeholder("x", team.getTargetBlock().getBlockX())
                                            .placeholder("y", team.getTargetBlock().getBlockY())
                                            .placeholder("z", team.getTargetBlock().getBlockZ())
                                            .placeholder("material", team.getTargetBlock().getBlock().getType().platformName())
                            );
                        }))
        );
    }
}
