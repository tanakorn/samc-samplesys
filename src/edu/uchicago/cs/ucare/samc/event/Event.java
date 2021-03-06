package edu.uchicago.cs.ucare.samc.event;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Event implements Serializable {
    
    public static final String EVENT_ID_KEY = "eventId";
    
    protected Map<String, Serializable> keyValuePairs;
    protected String callbackId;
    protected boolean obsolete;
    protected int obsoleteBy;
    
    public Event() {
        keyValuePairs = new HashMap<String, Serializable>();
        obsolete = false;
        obsoleteBy = -1;
    }
    
    public Event(int eventId) {
        keyValuePairs = new HashMap<String, Serializable>();
        addKeyValue(EVENT_ID_KEY, eventId);
        obsolete = false;
        obsoleteBy = -1;
    }
    
    public Event(int eventId, String callbackId) {
        keyValuePairs = new HashMap<String, Serializable>();
        addKeyValue(EVENT_ID_KEY, eventId);
        this.callbackId = callbackId;
        obsolete = false;
        obsoleteBy = -1;
    }
    
    public Event(String callbackId) {
        keyValuePairs = new HashMap<String, Serializable>();
        this.callbackId = callbackId;
        obsolete = false;
        obsoleteBy = -1;
    }
    
    public void addKeyValue(String key, Serializable value) {
        keyValuePairs.put(key, value);
    }
    
    public Object getValue(String key) {
        return keyValuePairs.get(key);
    }
    
    public String getCallbackId() {
        return callbackId;
    }

    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
    }

    public boolean isObsolete() {
        return obsolete;
    }

    public void setObsolete(boolean obsolete) {
        this.obsolete = obsolete;
    }

    public int getObsoleteBy() {
        return obsoleteBy;
    }

    public void setObsoleteBy(int obsoleteBy) {
        if (this.obsoleteBy == -1) {
            this.obsoleteBy = obsoleteBy;
        }
    }

    @Override
    public String toString() {
        return "Event=" + keyValuePairs + "";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((callbackId == null) ? 0 : callbackId.hashCode());
        result = prime * result
                + ((keyValuePairs == null) ? 0 : keyValuePairs.hashCode());
        result = prime * result + (obsolete ? 1231 : 1237);
        result = prime * result + obsoleteBy;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Event other = (Event) obj;
        if (callbackId == null) {
            if (other.callbackId != null)
                return false;
        } else if (!callbackId.equals(other.callbackId))
            return false;
        if (keyValuePairs == null) {
            if (other.keyValuePairs != null)
                return false;
        } else if (!keyValuePairs.equals(other.keyValuePairs))
            return false;
        if (obsolete != other.obsolete)
            return false;
        if (obsoleteBy != other.obsoleteBy)
            return false;
        return true;
    }

}
