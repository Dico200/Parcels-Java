package com.redstoner.parcels.api.list;

import java.io.Serializable;

public class SerialPlayerMap<T extends Serializable> extends PlayerMap<T> implements Serializable {

    private static final long serialVersionUID = -767166569071949301L;

    public SerialPlayerMap(T standard) {
        super(standard);
    }

}
