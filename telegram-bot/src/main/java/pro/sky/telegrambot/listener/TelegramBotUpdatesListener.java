package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {




    private static final Pattern PATTERN = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private NotificationTaskRepository notificationTaskRepository;

    @Autowired
    private TelegramBot telegramBot;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> list) {
        list.stream().filter(update -> update.message() != null)
                .forEach(this::processUpdate);
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void processUpdate(Update update) {
        String userMessage = update.message().text();
        Long chatId = update.message().chat().id();
        if (userMessage.equals("/start")) {
            this.telegramBot.execute(new SendMessage(chatId, "Hello!"));
        } else {
            if (processNotificationMessage(chatId, userMessage)) {
                this.telegramBot.execute(new SendMessage(chatId, "Напоминание создано"));
            } else {
                this.telegramBot.execute(new SendMessage(chatId, "Нипалучилось! " +
                        "Нужно в формате вида '01.01.2022 20:00 Сделать домашнюю работу'"));
            }
        }
    }

    private boolean processNotificationMessage(Long chatId, String message) {
        Matcher matcher = PATTERN.matcher(message);
        if (!matcher.matches()) {
            return false;
        }
        String date = matcher.group(1);
        String text = matcher.group(3);
        try {
            LocalDateTime notificationDate = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            NotificationTask notificationTask = new NotificationTask();
            notificationTask.setNotificationText(text);
            notificationTask.setNotificationDateTime(notificationDate);
            notificationTask.setNotificationChatId(chatId);
            notificationTaskRepository.save(notificationTask);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }

    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void sendNotifications() {
        LocalDateTime dateTime = LocalDateTime.now();
        List<NotificationTask> tasks = this.notificationTaskRepository
                .findByNotificationDateTimeEquals(dateTime.truncatedTo(ChronoUnit.MINUTES));
        tasks.forEach(task -> {
            this.telegramBot.execute(
                    new SendMessage(task.getNotificationChatId(), task.getNotificationText()));

        });
        this.notificationTaskRepository.deleteAll(tasks);
    }


}
