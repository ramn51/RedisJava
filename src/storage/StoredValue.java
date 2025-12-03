package storage;

public class StoredValue {
    private final String value;
    private final long expiryTime;

    public StoredValue(String value, long ttlMs){
        this.value = value;

        if(ttlMs > 0){
            this.expiryTime = System.currentTimeMillis() + ttlMs;
            System.out.println("DEBUG MATH: Now=" + System.currentTimeMillis() +
                    " + TTL=" + ttlMs +
                    " = Expiry=" + this.expiryTime);
        } else{
            this.expiryTime = -1;
            System.out.println("DEBUG MATH: No Expiry set (-1)");
        }
    }

    public String getValue(){
        return value;
    }

    public boolean isExpired(){
        if(expiryTime == -1) return false;
        return System.currentTimeMillis() > expiryTime;
    }

}
