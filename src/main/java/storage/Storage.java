package main.java.storage;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;

public class Storage {
    private final ConcurrentHashMap<String, StoredValue> db = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sets = new ConcurrentHashMap<>();

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

    public boolean sadd(String key, String member) {
        sets.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(member);
        return true;
    }

    public Set<String> smembers(String key) {
        return sets.getOrDefault(key, Collections.emptySet());
    }

    public boolean srem(String key, String member) {
        Set<String> set = sets.get(key);
        if (set != null) {
            return set.remove(member);
        }
        return false;
    }


    public Set<String> keySet(){
        return db.keySet();
    }
}
