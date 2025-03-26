package org.cockroachlabs.simulator.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class Relationship implements Serializable {
    @Column(nullable = false, updatable = false)
    public String parentKey;
    @Column(nullable = false, updatable = false)
    public Integer childId;

    // Default constructor
    public Relationship() {
    }

    public Relationship(String parentKey, Integer childId) {
        this.parentKey = parentKey;
        this.childId = childId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Relationship that = (Relationship) o;
        return Objects.equals(parentKey, that.parentKey) &&
                Objects.equals(childId, that.childId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parentKey, childId);
    }
}

