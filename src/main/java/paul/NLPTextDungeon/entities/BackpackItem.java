package paul.NLPTextDungeon.entities;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by Paul Dennis on 8/8/2017.
 */



@JsonTypeInfo(defaultImpl=BackpackItem.class,
        use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Note.class, name = "note")
})
@Entity
@Table(name = "items")
public class BackpackItem extends DungeonRoomEntity {

    @Id
    @GeneratedValue
    private Integer id;
    private String name;
    private Integer value;

    private transient String onPickup;
    private transient boolean darklight; //Item can only be seen in the dark

    public BackpackItem () {

    }

    public BackpackItem (String name) {
        this.name = name;
    }

    public BackpackItem(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getOnPickup() {
        return onPickup;
    }

    public void setOnPickup(String onPickup) {
        this.onPickup = onPickup;
    }

    public boolean hasPickupAction () {
        return onPickup != null;
    }

    @Override
    public String toString () {
        return name;
    }

    public static final double DEFAULT_VISIBILITY_THRESHHOLD = 0.6;
    public boolean isVisible (double lighting) {
        if (darklight) {
            return lighting == 0.0;
        }
        return lighting >= DEFAULT_VISIBILITY_THRESHHOLD;
    }

    public boolean isDarklight() {
        return darklight;
    }

    public void setDarklight(boolean darklight) {
        this.darklight = darklight;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
