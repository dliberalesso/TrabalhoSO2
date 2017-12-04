package me.dliberalesso;

import org.jgroups.Address;
import org.jgroups.util.Streamable;
import org.jgroups.util.Util;

import java.io.DataInput;
import java.io.DataOutput;

public class ClusterID implements Streamable {
    private static int next_id = 1;
    private Address creator;
    private int id;

    public ClusterID(Address creator, int id) {
        this.creator = creator;
        this.id = id;
    }

    public static synchronized ClusterID create(Address addr) {
        return new ClusterID(addr, next_id++);
    }

    public int getId() {
        return id;
    }

    public int hashCode() {
        return creator.hashCode() + id;
    }

    public boolean equals(Object obj) {
        ClusterID other = (ClusterID) obj;
        return creator.equals(other.creator) && id == other.id;
    }

    public String toString() {
        return creator + "::" + id;
    }


    public void writeTo(DataOutput out) throws Exception {
        Util.writeAddress(creator, out);
        out.writeInt(id);
    }

    public void readFrom(DataInput in) throws Exception {
        creator = Util.readAddress(in);
        id = in.readInt();
    }
}