package de.presti.ree6.backend.controller;


import de.presti.ree6.backend.bot.BotWorker;
import de.presti.ree6.backend.service.GuildService;
import de.presti.ree6.backend.service.SessionService;
import de.presti.ree6.backend.utils.data.container.*;
import de.presti.ree6.backend.utils.data.container.api.*;
import de.presti.ree6.backend.utils.data.container.guild.GuildContainer;
import de.presti.ree6.backend.utils.data.container.guild.GuildStatsContainer;
import de.presti.ree6.backend.utils.data.container.role.RoleContainer;
import de.presti.ree6.backend.utils.data.container.role.RoleLevelContainer;
import de.presti.ree6.backend.utils.data.container.user.UserContainer;
import de.presti.ree6.backend.utils.data.container.user.UserLevelContainer;
import de.presti.ree6.sql.SQLSession;
import de.presti.ree6.sql.entities.Tickets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@CrossOrigin
@RestController
@RequestMapping("/guilds")
public class GuildController {
    private final SessionService sessionService;

    private final GuildService guildService;

    @Autowired
    public GuildController(SessionService sessionService, GuildService guildService) {
        this.sessionService = sessionService;
        this.guildService = guildService;
    }

    //region Guild Retrieve

    @CrossOrigin
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<List<GuildContainer>> retrieveGuilds(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier) {
        try {
            return new GenericObjectResponse<>(true, sessionService.retrieveGuilds(sessionIdentifier), "Guilds retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @GetMapping(value = "/{guildId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<GuildContainer> retrieveGuild(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                               @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, sessionService.retrieveGuild(sessionIdentifier, guildId, true, true), "Guild retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    //endregion

    //region Guild Channel and Role

    @CrossOrigin
    @GetMapping(value = "/{guildId}/channels", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<List<ChannelContainer>> retrieveGuildChannels(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                               @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, sessionService.retrieveGuild(sessionIdentifier, guildId, true).getChannels(), "Channels retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @GetMapping(value = "/{guildId}/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<List<RoleContainer>> retrieveGuildRoles(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                         @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, sessionService.retrieveGuild(sessionIdentifier, guildId, false, true).getRoles(), "Roles retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    //endregion

    //region Guild Blacklist

    @CrossOrigin
    @GetMapping(value = "/{guildId}/blacklist", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<List<String>> retrieveGuildBlacklist(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                      @PathVariable(name = "guildId") String guildId) {
        try {
            GuildContainer guildContainer = sessionService.retrieveGuild(sessionIdentifier, guildId);
            List<String> blacklist = SQLSession.getSqlConnector().getSqlWorker().getChatProtectorWords(guildId);
            return new GenericObjectResponse<>(true, blacklist, "Blacklist retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, Collections.emptyList(), e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/blacklist/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeGuildBlacklist(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                @PathVariable(name = "guildId") String guildId,
                                                @RequestBody GenericValueRequest request) {
        try {
            GuildContainer guildContainer = sessionService.retrieveGuild(sessionIdentifier, guildId);

            SQLSession.getSqlConnector().getSqlWorker().removeChatProtectorWord(guildId, request.value());
            return new GenericResponse(true, "Blacklist removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/blacklist/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addGuildBlacklist(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                             @PathVariable(name = "guildId") String guildId,
                                             @RequestBody GenericValueRequest request) {
        try {
            GuildContainer guildContainer = sessionService.retrieveGuild(sessionIdentifier, guildId);

            if (!SQLSession.getSqlConnector().getSqlWorker().isChatProtectorSetup(guildId, request.value())) {
                SQLSession.getSqlConnector().getSqlWorker().addChatProtectorWord(guildId, request.value());
                return new GenericResponse(true, "Blacklist added!");
            } else {
                return new GenericResponse(false, "Word already blacklisted!");
            }
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion

    //region Guild AutoRole

    @CrossOrigin
    @GetMapping(value = "/{guildId}/autorole", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<List<RoleContainer>> retrieveGuildAutoRole(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                            @PathVariable(name = "guildId") String guildId) {
        try {
            GuildContainer guildContainer = sessionService.retrieveGuild(sessionIdentifier, guildId, false, true);
            List<RoleContainer> autoRoles = SQLSession.getSqlConnector().getSqlWorker().getAutoRoles(guildId).stream().map(c -> guildContainer.getRoleById(c.getRoleId())).filter(Objects::nonNull).toList();
            return new GenericObjectResponse<>(true, autoRoles, "AutoRole retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, Collections.emptyList(), e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/autorole/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeGuildAutoRole(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                               @PathVariable(name = "guildId") String guildId,
                                               @RequestBody GenericValueRequest request) {
        try {
            GuildContainer guildContainer = sessionService.retrieveGuild(sessionIdentifier, guildId);
            SQLSession.getSqlConnector().getSqlWorker().removeAutoRole(guildId, request.value());
            return new GenericResponse(true, "AutoRole removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/autorole/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addGuildAutoRole(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                            @PathVariable(name = "guildId") String guildId,
                                            @RequestBody GenericValueRequest request) {
        try {
            GuildContainer guildContainer = sessionService.retrieveGuild(sessionIdentifier, guildId, false, true);

            if (guildContainer.getRoleById(request.value()) == null)
                throw new IllegalAccessException("Role not found!");

            if (!SQLSession.getSqlConnector().getSqlWorker().isAutoRoleSetup(guildId, request.value())) {
                SQLSession.getSqlConnector().getSqlWorker().addAutoRole(guildId, request.value());
                return new GenericResponse(true, "AutoRole added!");
            } else {
                return new GenericResponse(false, "Role is already in AutoRole!");
            }
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion

    //region Guild Leaderboard

    @CrossOrigin
    @GetMapping(value = "/{guildId}/leaderboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<LeaderboardContainer> retrieveSetting(@PathVariable(name = "guildId") String guildId) {
        try {
            // Call this to check if guild exists. If not exception is thrown.
            GuildContainer guildContainer = sessionService.retrieveGuild(guildId);

            LeaderboardContainer leaderboardContainer = new LeaderboardContainer();

            leaderboardContainer.setChatLeaderboard(SQLSession.getSqlConnector().getSqlWorker().getTopChat(guildId, 5).stream()
                    .map(c -> new UserLevelContainer(c, new UserContainer(BotWorker.getShardManager().retrieveUserById(c.getUserId()).complete()))).toList());

            leaderboardContainer.setVoiceLeaderboard(SQLSession.getSqlConnector().getSqlWorker().getTopVoice(guildId, 5).stream()
                    .map(c -> new UserLevelContainer(c, new UserContainer(BotWorker.getShardManager().retrieveUserById(c.getUserId()).complete()))).toList());

            leaderboardContainer.setGuildId(guildContainer.getId());

            return new GenericObjectResponse<>(true, leaderboardContainer, "Leaderboard retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    //endregion

    // region Guild Stats

    @CrossOrigin
    @GetMapping(value = "/{guildId}/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<GuildStatsContainer> retrieveStats(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                    @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getStats(sessionIdentifier, guildId), "Stats retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    //endregion

    // region Guild Chat Autorole

    @CrossOrigin
    @GetMapping(value = "/{guildId}/chatrole", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<List<RoleLevelContainer>> retrieveChatRoles(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                             @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getChatAutoRoles(sessionIdentifier, guildId), "Chat Autorole retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/chatrole/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeChatAutoRole(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                              @PathVariable(name = "guildId") String guildId,
                                              @RequestBody GenericValueRequest valueRequest) {
        try {
            long level = Long.parseLong(valueRequest.value());
            guildService.removeChatAutoRole(sessionIdentifier, guildId, level);
            return new GenericResponse(true, "Chat Auto-role removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/chatrole/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addChatAutoRole(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                           @PathVariable(name = "guildId") String guildId,
                                           @RequestBody LevelAutoRoleRequest levelAutoRoleRequest) {
        try {
            guildService.addChatAutoRole(sessionIdentifier, guildId, levelAutoRoleRequest.roleId(), levelAutoRoleRequest.level());
            return new GenericResponse(true, "Chat Auto-role added!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion

    // region Guild Voice Autorole

    @CrossOrigin
    @GetMapping(value = "/{guildId}/voicerole", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<List<RoleLevelContainer>> retrieveVoiceRoles(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                              @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getVoiceAutoRoles(sessionIdentifier, guildId), "Voice Autorole retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/voicerole/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeVoiceAutoRole(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                               @PathVariable(name = "guildId") String guildId,
                                               @RequestBody GenericValueRequest valueRequest) {
        try {
            long level = Long.parseLong(valueRequest.value());
            guildService.removeVoiceAutoRole(sessionIdentifier, guildId, level);
            return new GenericResponse(true, "Voice Auto-role removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/voicerole/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addVoiceAutoRole(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                            @PathVariable(name = "guildId") String guildId,
                                            @RequestBody LevelAutoRoleRequest levelAutoRoleRequest) {
        try {
            guildService.addVoiceAutoRole(sessionIdentifier, guildId, levelAutoRoleRequest.roleId(), levelAutoRoleRequest.level());
            return new GenericResponse(true, "Voice Auto-role added!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion

    //region Recording

    @CrossOrigin
    @GetMapping(value = "/recording", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<RecordContainer> retrieveRecording(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                    @RequestParam(name = "recordId") String recordId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getRecording(sessionIdentifier, recordId), "Recording retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    //endregion

    //region Guild Welcome Channel

    @CrossOrigin
    @GetMapping(value = "/{guildId}/welcome", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<ChannelContainer> retrieveWelcomeChannel(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                          @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getWelcomeChannel(sessionIdentifier, guildId), "Welcome channel retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/welcome/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeWelcomeChannel(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                @PathVariable(name = "guildId") String guildId) {
        try {
            guildService.removeWelcomeChannel(sessionIdentifier, guildId);
            return new GenericResponse(true, "Welcome channel removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/welcome/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addWelcomeChannel(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                             @PathVariable(name = "guildId") String guildId,
                                             @RequestBody GenericValueRequest request) {
        try {
            guildService.updateWelcomeChannel(sessionIdentifier, guildId, request.value());
            return new GenericResponse(true, "Welcome channel added!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion

    //region Guild Log Channel

    @CrossOrigin
    @GetMapping(value = "/{guildId}/log", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<ChannelContainer> retrieveLogChannel(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                      @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getLogChannel(sessionIdentifier, guildId), "Log channel retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/log/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeLogChannel(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                            @PathVariable(name = "guildId") String guildId) {
        try {
            guildService.removeLogChannel(sessionIdentifier, guildId);
            return new GenericResponse(true, "Log channel removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/log/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addLogChannel(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                         @PathVariable(name = "guildId") String guildId,
                                         @RequestBody GenericValueRequest request) {
        try {
            guildService.updateLogChannel(sessionIdentifier, guildId, request.value());
            return new GenericResponse(true, "Log channel added!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion

    //region Notifier

    //region Reddit Notifier

    @CrossOrigin
    @GetMapping(value = "/{guildId}/reddit", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<List<NotifierContainer>> retrieveRedditNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                          @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getRedditNotifier(sessionIdentifier, guildId), "Reddit Notifiers retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/reddit/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeRedditNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                @PathVariable(name = "guildId") String guildId,
                                                @RequestBody GenericValueRequest request) {
        try {
            guildService.removeRedditNotifier(sessionIdentifier, guildId, request.value());
            return new GenericResponse(true, "Reddit Notifier removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/reddit/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addRedditNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                             @PathVariable(name = "guildId") String guildId,
                                             @RequestBody GenericNotifierRequest notifierRequestObject) {
        try {
            guildService.addRedditNotifier(sessionIdentifier, guildId, notifierRequestObject);
            return new GenericResponse(true, "Reddit Notifier added!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion

    //region Twitch Notifier

    @CrossOrigin
    @GetMapping(value = "/{guildId}/twitch", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<List<NotifierContainer>> retrieveTwitchNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                                 @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getTwitchNotifier(sessionIdentifier, guildId), "Twitch Notifiers retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/twitch/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeTwitchNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                @PathVariable(name = "guildId") String guildId,
                                                @RequestBody GenericValueRequest request) {
        try {
            guildService.removeTwitchNotifier(sessionIdentifier, guildId, request.value());
            return new GenericResponse(true, "Twitch Notifier removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/twitch/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addTwitchNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                             @PathVariable(name = "guildId") String guildId,
                                             @RequestBody GenericNotifierRequest notifierRequestObject) {
        try {
            guildService.addTwitchNotifier(sessionIdentifier, guildId, notifierRequestObject);
            return new GenericResponse(true, "Twitch Notifier added!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }


    //endregion

    //region Twitter Notifier

    @CrossOrigin
    @GetMapping(value = "/{guildId}/twitter", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<List<NotifierContainer>> retrieveTwitterNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                                  @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getTwitterNotifier(sessionIdentifier, guildId), "Twitter Notifiers retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/twitter/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeTwitterNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                 @PathVariable(name = "guildId") String guildId,
                                                 @RequestBody GenericValueRequest request) {
        try {
            guildService.removeTwitterNotifier(sessionIdentifier, guildId, request.value());
            return new GenericResponse(true, "Twitter Notifier removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/twitter/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addTwitterNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                              @PathVariable(name = "guildId") String guildId,
                                              @RequestBody GenericNotifierRequest notifierRequestObject) {
        try {
            guildService.addTwitterNotifier(sessionIdentifier, guildId, notifierRequestObject);
            return new GenericResponse(true, "Twitter Notifier added!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion

    //region YouTube Notifier

    @CrossOrigin
    @GetMapping(value = "/{guildId}/youtube", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<List<NotifierContainer>> retrieveYoutubeNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                                  @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getYouTubeNotifier(sessionIdentifier, guildId), "Youtube Notifiers retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/youtube/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeYoutubeNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                 @PathVariable(name = "guildId") String guildId,
                                                 @RequestBody GenericValueRequest request) {
        try {
            guildService.removeYouTubeNotifier(sessionIdentifier, guildId, request.value());
            return new GenericResponse(true, "Youtube Notifier removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/youtube/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addYoutubeNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                              @PathVariable(name = "guildId") String guildId,
                                              @RequestBody GenericNotifierRequest notifierRequestObject) {
        try {
            guildService.addYouTubeNotifier(sessionIdentifier, guildId, notifierRequestObject);
            return new GenericResponse(true, "Youtube Notifier added!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion

    //region Instagram Notifier

    @CrossOrigin
    @GetMapping(value = "/{guildId}/instagram", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<List<NotifierContainer>> retrieveInstagramNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                                     @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getInstagramNotifier(sessionIdentifier, guildId), "Instagram Notifiers retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/instagram/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeInstagramNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                   @PathVariable(name = "guildId") String guildId,
                                                   @RequestBody GenericValueRequest request) {
        try {
            guildService.removeInstagramNotifier(sessionIdentifier, guildId, request.value());
            return new GenericResponse(true, "Instagram Notifier removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/instagram/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addInstagramNotifier(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                @PathVariable(name = "guildId") String guildId,
                                                @RequestBody GenericNotifierRequest notifierRequestObject) {
        try {
            guildService.addInstagramNotifier(sessionIdentifier, guildId, notifierRequestObject);
            return new GenericResponse(true, "Instagram Notifier added!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion

    //endregion

    //region Temporal Voice Channels

    @CrossOrigin
    @GetMapping(value = "/{guildId}/temporalvoice", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<ChannelContainer> retrieveTemporalVoice(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                      @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getTemporalVoice(sessionIdentifier, guildId), "TemporalVoice channel retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/temporalvoice/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeTemporalVoiceChannel(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                            @PathVariable(name = "guildId") String guildId) {
        try {
            guildService.removeTemporalVoice(sessionIdentifier, guildId);
            return new GenericResponse(true, "TemporalVoice channel removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/temporalvoice/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addTemporalVoiceChannel(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                         @PathVariable(name = "guildId") String guildId,
                                         @RequestBody GenericValueRequest request) {
        try {
            guildService.updateTemporalVoice(sessionIdentifier, guildId, request.value());
            return new GenericResponse(true, "TemporalVoice channel added!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion

    //region Opt-Out

    @CrossOrigin
    @GetMapping(value = "/{guildId}/opt-out/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse checkOptOut(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                       @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericResponse(true, guildService.checkOptOut(sessionIdentifier, guildId));
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @GetMapping(value = "/{guildId}/opt-out", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse optOut(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                       @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericResponse(true, guildService.optOut(sessionIdentifier, guildId));
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion

    //region Tickets

    @CrossOrigin
    @GetMapping(value = "/{guildId}/tickets", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<TicketContainer> retrieveTicket(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                 @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getTicket(sessionIdentifier, guildId), "Tickets retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/tickets/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeTicket(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                      @PathVariable(name = "guildId") String guildId) {
        try {
            guildService.removeTicket(sessionIdentifier, guildId);
            return new GenericResponse(true, "Tickets removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/tickets/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addTicket(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                   @PathVariable(name = "guildId") String guildId,
                                                   @RequestBody TicketsRequest request) {
        try {
            guildService.updateTicket(sessionIdentifier, guildId, request.channelId(), request.logChannelId(), request.ticketMessageOpen(), request.ticketMessageMenu());
            return new GenericResponse(true, "Tickets added!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion

    //region Suggestion

    @CrossOrigin
    @GetMapping(value = "/{guildId}/suggestions", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericObjectResponse<SuggestionContainer> retrieveSuggestion(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                                                 @PathVariable(name = "guildId") String guildId) {
        try {
            return new GenericObjectResponse<>(true, guildService.getSuggestion(sessionIdentifier, guildId), "Suggestions retrieved!");
        } catch (Exception e) {
            return new GenericObjectResponse<>(false, null, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/suggestions/remove", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse removeSuggestion(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                        @PathVariable(name = "guildId") String guildId) {
        try {
            guildService.removeSuggestion(sessionIdentifier, guildId);
            return new GenericResponse(true, "Suggestions removed!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    @CrossOrigin
    @PostMapping(value = "/{guildId}/suggestions/add", produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse addSuggestion(@RequestHeader(name = "X-Session-Authenticator") String sessionIdentifier,
                                     @PathVariable(name = "guildId") String guildId,
                                     @RequestBody SuggestionRequest request) {
        try {
            guildService.updateSuggestion(sessionIdentifier, guildId, request.channelId(), request.suggestionMessageMenu());
            return new GenericResponse(true, "Suggestions added!");
        } catch (Exception e) {
            return new GenericResponse(false, e.getMessage());
        }
    }

    //endregion
}