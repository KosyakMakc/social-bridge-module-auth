package io.github.kosyakmakc.socialBridge.AuthSocial.Commands.MinecraftCommands;

import io.github.kosyakmakc.socialBridge.AuthSocial.AuthModule;
import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.AuthMessageKey;
import io.github.kosyakmakc.socialBridge.AuthSocial.Utils.AuthPermissions;
import io.github.kosyakmakc.socialBridge.Commands.MinecraftCommands.MinecraftCommandBase;
import io.github.kosyakmakc.socialBridge.MinecraftPlatform.MinecraftUser;
import io.github.kosyakmakc.socialBridge.Utils.MessageKey;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class StatusCommand extends MinecraftCommandBase {
    private final AuthModule module;

    public StatusCommand(AuthModule module) {
        super("status", AuthMessageKey.STATUS_COMMAND_DESCRIPTION, AuthPermissions.CAN_STATUS);
        this.module = module;
    }

    @Override
    public void execute(MinecraftUser minecraftUser, List<Object> list) {
        var locale = minecraftUser.getLocale();

        var getHeaderTemplateTask = getBridge().getLocalizationService().getMessage(module, locale, AuthMessageKey.STATUS_COMMAND_HEADER);
        var getRecordTemplateTask = getBridge().getLocalizationService().getMessage(module, locale, AuthMessageKey.STATUS_COMMAND_RECORD);
        var getEmptyTemplateTask = getBridge().getLocalizationService().getMessage(module, locale, AuthMessageKey.STATUS_COMMAND_EMPTY);

        var handlersCopy = module.getSocialHandlers().stream().toList();
        @SuppressWarnings({ "rawtypes", "unchecked" })
        LinkedList<CompletableFuture> tasks = new LinkedList(handlersCopy.stream().map(x -> (CompletableFuture) x.isAuthorized(minecraftUser)).toList());
        tasks.add(getHeaderTemplateTask);
        tasks.add(getRecordTemplateTask);
        tasks.add(getEmptyTemplateTask);

        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new))
        .thenRun(() -> {
            try {
                minecraftUser.sendMessage(getHeaderTemplateTask.get(), new HashMap<>());
                var isAny = false;
                var index = 0;
                for (var handler : handlersCopy) {
                    if ((boolean) tasks.get(index).get()) {
                        isAny = true;
                        var placeholders = new HashMap<String, String>();
                        placeholders.put("social-platform-name", handler.getPlatform().getPlatformName());
                        minecraftUser.sendMessage(getRecordTemplateTask.get(), placeholders);
                    }
                    index++;
                }
                
                if (!isAny) {
                    minecraftUser.sendMessage(getEmptyTemplateTask.get(), new HashMap<>());
                }
            }
            catch (InterruptedException | ExecutionException err) {
                getBridge().getLocalizationService().getMessage(module, locale, MessageKey.INTERNAL_SERVER_ERROR)
                .thenAccept(msgTemplate -> {
                    minecraftUser.sendMessage(msgTemplate, new HashMap<>());
                });
            }
        });
    }
}
