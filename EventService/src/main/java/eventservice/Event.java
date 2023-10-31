package eventservice;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity("events")
public record Event(
        @Id UUID uuid,
        LocalDateTime eventDateTime,
        String title,
        String description,
        String hEmail) {

    private static final int TITLE_MAX_LENGTH = 255;
    private static final int DESCRIPTION_MAX_LENGTH = 600;
    //returns the LocalDateTime if the date and the time are valid
    private static LocalDateTime validateDateTime(String date, String time) throws HandledIllegalValueException {
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")){
            throw new HandledIllegalValueException("Date must be formatted YYYY-MM-DD, was " + date);
        }
        if (!time.matches("\\d{1,2}:\\d{1,2}")){
            throw new HandledIllegalValueException("Time must be in the format \"HH:mm\", was " + time);
        }
        String[] dateSplit = date.split("-");
        String[] timeSplit = time.split(" |:");
        int hour = Integer.parseInt(timeSplit[0]);
        if (hour < 1 || hour > 24){
            throw new HandledIllegalValueException("Hour must be in the range [1,24]");
        }
        int minute = Integer.parseInt(timeSplit[1]);
        if(minute < 0 || minute > 59){
            throw new HandledIllegalValueException("Minutes must be in the range [0, 59]");
        }
        int year = Integer.parseInt(dateSplit[0]);
        Month month = Month.of(Integer.parseInt(dateSplit[1]));
        int day = Integer.parseInt(dateSplit[2]);
        return LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute));
    }
    public static String validateEmail(String email) throws HandledIllegalValueException {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";
        // note -- the actual email regex is much more sophisticated than this,
        // but there's no good way to add all the proper escapes for java, and
        // java has no raw strings
        if(!email.matches(emailRegex)){
            throw new HandledIllegalValueException("Email %s is invalid".formatted(email));
        }
        return email;
    }
    public Event validateEmail() throws HandledIllegalValueException{
        //these should be handled
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new HandledIllegalValueException(String.format("Title may not exceed %d", TITLE_MAX_LENGTH));
        }
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new HandledIllegalValueException(String.format("Description may not exceed %d", DESCRIPTION_MAX_LENGTH));
        }

        return this;
    }
    private static Event create(Optional<String> uuid, String date, String time, String title, String description, String hEmail) throws HandledIllegalValueException{
        UUID uid;
        try {
            uid = uuid.map(UUID::fromString).orElse(UUID.randomUUID());
        } catch (IllegalArgumentException e) {
            throw new HandledIllegalValueException("UUID is invalid, please try again");
        }
        return new Event(uid, validateDateTime(date, time), title, description, validateEmail(hEmail)).validateEmail();
    }

    public static Event create(String uuid, String date, String time, String title, String description, String hEmail) throws HandledIllegalValueException{
        return Event.create(Optional.ofNullable(uuid), date, time, title, description, hEmail).validateEmail();
    }
    public static class HandledIllegalValueException extends Exception{
        public HandledIllegalValueException(String message) {
            super(message);
        }
    }

    @Deprecated
    public static Event create(String date, String time, String title, String description, String hEmail) throws HandledIllegalValueException{
        return Event.create(UUID.randomUUID().toString(), date, time, title, description, validateEmail(hEmail));
    }

    public EventPojo toPojo(){
        return new EventPojo(uuid, eventDateTime, title, description, hEmail);
    }


}
