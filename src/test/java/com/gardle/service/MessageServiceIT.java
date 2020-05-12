package com.gardle.service;

import com.gardle.GardleApp;
import com.gardle.domain.GardenField;
import com.gardle.domain.Leasing;
import com.gardle.domain.User;
import com.gardle.domain.enumeration.LeasingStatus;
import com.gardle.domain.enumeration.MessageType;
import com.gardle.repository.GardenFieldRepository;
import com.gardle.repository.LeasingRepository;
import com.gardle.repository.MessageRepository;
import com.gardle.repository.UserRepository;
import com.gardle.service.dto.MessageDTO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = GardleApp.class)
public class MessageServiceIT {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GardenFieldRepository gardenFieldRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private LeasingRepository leasingRepository;

    private static final String DEFAULT_LOGIN = "johndoe";
    private static final String DEFAULT_EMAIL = "johndoe@localhost";
    private static final String DEFAULT_FIRSTNAME = "john";
    private static final String DEFAULT_LASTNAME = "doe";
    private static final String USER2_LOGIN = "randymarsh";
    private static final String USER3_LOGIN = "towelie";
    private static final String DEFAULT_IBAN = "AT89370400440532013000";
    private static final String GARDEN_NAME = "testGarden";
    private static final Double SIZE_IN_M2 = 10.0;
    private static final Double PRICE_PER_M2 = 3.0;
    private static final Double LATITUDE = 1.0;
    private static final Double LONGITUDE = 2.0;
    private static final String CITY = "testCity";
    private static final Boolean ROOFED = false;
    private static final Boolean GLASS_HOUSE = false;
    private static final Boolean HIGH = true;
    private static final Boolean WATER = true;
    private static final Boolean ELECTRICITY = false;
    private static final Double PH_VALUE = null;
    private static final Instant DEFAULT_FROM = Instant.now().plus(100, ChronoUnit.DAYS);
    private static final Instant DEFAULT_TO = DEFAULT_FROM.plus(180, ChronoUnit.DAYS); // + 6 months

    private User owner;
    private User requester;
    private User user3;
    private GardenField gardenField;
    private Leasing leasing;

    private User saveUser(String login, String fName, String lName, String email) {
        User user = new User();
        user.setLogin(login);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setBankAccountIBAN(DEFAULT_IBAN);
        user.setFirstName(fName);
        user.setLastName(lName);
        user.setEmail(email);

        return userRepository.saveAndFlush(user);
    }

    @BeforeEach
    public void init() {
        owner = saveUser(DEFAULT_LOGIN, DEFAULT_FIRSTNAME, DEFAULT_LASTNAME, DEFAULT_EMAIL);
        requester = saveUser(USER2_LOGIN, "Randy", "Marsh", "rm@sp.com");
        user3 = saveUser(USER3_LOGIN, "Towel", "Wet", "twl@tegr.com");

        gardenField = new GardenField();
        gardenField.setName(GARDEN_NAME);
        gardenField.setSizeInM2(SIZE_IN_M2);
        gardenField.setPricePerM2(PRICE_PER_M2);
        gardenField.setLatitude(LATITUDE);
        gardenField.setLongitude(LONGITUDE);
        gardenField.setCity(CITY);
        gardenField.setRoofed(ROOFED);
        gardenField.setGlassHouse(GLASS_HOUSE);
        gardenField.setHigh(HIGH);
        gardenField.setWater(WATER);
        gardenField.setElectricity(ELECTRICITY);
        gardenField.setPhValue(PH_VALUE);
        gardenField.setOwner(owner);
        gardenFieldRepository.saveAndFlush(gardenField);

        leasing = new Leasing()
            .from(DEFAULT_FROM)
            .to(DEFAULT_TO)
            .gardenField(gardenField)
            .paymentSessionId("test")
            .user(requester);
    }

    private int getMessageCountForUser(User user) {
        return (int) messageRepository.findAll().stream()
            .filter(notif -> notif.getUserTo().getId().equals(user.getId())).count();
    }

    @Test
    @Transactional
    public void testCreateOpenLeasingNotification() {
        int messagesForOwnerBeforeCreation = getMessageCountForUser(owner);
        int messagesForRequesterBeforeCreation = getMessageCountForUser(requester);

        leasing.setStatus(LeasingStatus.OPEN);
        leasingRepository.saveAndFlush(leasing);

        MessageDTO notifDTO = messageService.createLeasingNotification(leasing);

        int messagesForOwnerAfterCreation = getMessageCountForUser(owner);
        int messagesForRequesterAfterCreation = getMessageCountForUser(requester);

        assertThat(messagesForOwnerAfterCreation).isEqualTo(messagesForOwnerBeforeCreation + 1);
        assertThat(messagesForRequesterAfterCreation).isEqualTo(messagesForRequesterBeforeCreation);
        assertThat(notifDTO.getType()).isEqualByComparingTo(MessageType.SYSTEM_LEASING_OPEN);
        assertThat(notifDTO.getOpened()).isFalse();
    }

    @Test
    @Transactional
    public void testCreateReservedLeasingNotification() {
        int messagesForOwnerBeforeCreation = getMessageCountForUser(owner);
        int messagesForRequesterBeforeCreation = getMessageCountForUser(requester);

        leasing.setStatus(LeasingStatus.RESERVED);
        leasingRepository.saveAndFlush(leasing);

        MessageDTO notifDTO = messageService.createLeasingNotification(leasing);

        int messagesForOwnerAfterCreation = getMessageCountForUser(owner);
        int messagesForRequesterAfterCreation = getMessageCountForUser(requester);

        assertThat(messagesForOwnerAfterCreation).isEqualTo(messagesForOwnerBeforeCreation);
        assertThat(messagesForRequesterAfterCreation).isEqualTo(messagesForRequesterBeforeCreation + 1);
        assertThat(notifDTO.getType()).isEqualByComparingTo(MessageType.SYSTEM_LEASING_RESERVED);
        assertThat(notifDTO.getOpened()).isFalse();
    }

    @Test
    @Transactional
    public void testCreateRejectedLeasingNotification() {
        int messagesForOwnerBeforeCreation = getMessageCountForUser(owner);
        int messagesForRequesterBeforeCreation = getMessageCountForUser(requester);

        leasing.setStatus(LeasingStatus.REJECTED);
        leasingRepository.saveAndFlush(leasing);

        MessageDTO notifDTO = messageService.createLeasingNotification(leasing);

        int messagesForOwnerAfterCreation = getMessageCountForUser(owner);
        int messagesForRequesterAfterCreation = getMessageCountForUser(requester);

        assertThat(messagesForOwnerAfterCreation).isEqualTo(messagesForOwnerBeforeCreation);
        assertThat(messagesForRequesterAfterCreation).isEqualTo(messagesForRequesterBeforeCreation + 1);
        assertThat(notifDTO.getType()).isEqualByComparingTo(MessageType.SYSTEM_LEASING_REJECTED);
        assertThat(notifDTO.getOpened()).isFalse();
    }

    @Test
    @Transactional
    public void testCreateCancelledLeasingNotification() {
        int messagesForOwnerBeforeCreation = getMessageCountForUser(owner);
        int messagesForRequesterBeforeCreation = getMessageCountForUser(requester);

        leasing.setStatus(LeasingStatus.CANCELLED);
        leasingRepository.saveAndFlush(leasing);

        MessageDTO notifDTO = messageService.createLeasingNotification(leasing);

        int messagesForOwnerAfterCreation = getMessageCountForUser(owner);
        int messagesForRequesterAfterCreation = getMessageCountForUser(requester);

        assertThat(messagesForOwnerAfterCreation).isEqualTo(messagesForOwnerBeforeCreation + 1);
        assertThat(messagesForRequesterAfterCreation).isEqualTo(messagesForRequesterBeforeCreation);
        assertThat(notifDTO.getType()).isEqualByComparingTo(MessageType.SYSTEM_LEASING_CANCELLED);
        assertThat(notifDTO.getOpened()).isFalse();
    }

    @Test
    @Transactional
    public void testSameThreadInTwoNotifications() {
        int messagesForOwnerBeforeCreation = getMessageCountForUser(owner);
        int messagesForRequesterBeforeCreation = getMessageCountForUser(requester);

        leasing.setStatus(LeasingStatus.CANCELLED);
        leasingRepository.saveAndFlush(leasing);
        MessageDTO notifDTO = messageService.createLeasingNotification(leasing);

        leasing.setStatus(LeasingStatus.RESERVED);
        leasingRepository.saveAndFlush(leasing);
        MessageDTO notif2DTO = messageService.createLeasingNotification(leasing);

        int messagesForOwnerAfterCreation = getMessageCountForUser(owner);
        int messagesForRequesterAfterCreation = getMessageCountForUser(requester);

        assertThat(messagesForOwnerAfterCreation).isEqualTo(messagesForOwnerBeforeCreation + 1);
        assertThat(messagesForRequesterAfterCreation).isEqualTo(messagesForRequesterBeforeCreation +1);
        assertThat(notifDTO.getThread()).isEqualByComparingTo(notif2DTO.getThread());
    }
}
