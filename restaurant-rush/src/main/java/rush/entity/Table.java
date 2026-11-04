package rush.entity;

/** A table in the restaurant. It is occupied by at most one customer until that customer pays or leaves. */
public final class Table {

    private final String id;
    private Customer occupant;

    Table(String id) {
        this.id = id;
    }

    /** @return true if no customer is at this table */
    public boolean isFree() {
        return occupant == null;
    }

    void occupy(Customer customer) {
        occupant = customer;
    }

    void release() {
        occupant = null;
    }

    /** @return the display id, for example "T1" */
    public String getId() {
        return id;
    }

    /** @return the customer at the table, or null if free */
    public Customer getOccupant() {
        return occupant;
    }

    @Override
    public String toString() {
        return id;
    }
}
