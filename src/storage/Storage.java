package storage;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import storage.StoredValue;

public class Storage {
    private final ConcurrentHashMap<String, StoredValue> db = new ConcurrentHashMap<>();

    public void put(String key, String value, long ttl){
        System.out.println("DEBUG STORAGE: Putting " + key + " with ttl " + ttl);
        db.put(key, new StoredValue(value, ttl));
    }

    public void put(String key, String value){
        put(key, value, -1);
    }

    public String get(String key){
        StoredValue entry = db.get(key);

        if(entry == null) return null;

        if(entry.isExpired()){
            db.remove(key);
            return null;
        }
        return entry.getValue();
    }

    public int size(){
        return db.size();
    }

    public void removeExpiredKeys(int sampleSize) {
        int checked = 0;

        for(String key: db.keySet()){
            StoredValue entry = db.get(key);
            if(entry!=null && entry.isExpired()){
                db.remove(key);
            }
            checked++;
            if(checked >= sampleSize)
                break;
        }
    }

    public Set<String> keySet(){
        return db.keySet();
    }
}
