package eventplanner.contrivers.com.eventplanner.model;

import java.util.List;

public class Plan {
    public String location;
    public String locationName;
    public String name;
    public List<Person> persons;

    public Plan() {
    }

    public Plan(String location, String locationName, String name, List<Person> persons) {
        this.name = location;
        this.locationName = locationName;
        this.name = name;
        this.persons = persons;
    }

    @Override
    public String toString() {
        return location;
    }

    public void updateOrCreate(Person newPerson) {
        for (Person person : persons) {
            if (person.name.equalsIgnoreCase(newPerson.name)) {
                person.location = newPerson.location;
                return;
            }
        }
        persons.add(newPerson);
    }
}
