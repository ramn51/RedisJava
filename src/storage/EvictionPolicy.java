package storage;

public class EvictionPolicy implements Runnable{
    private final Storage storage;
    private final int SAMPLE_SIZE=20;

    public EvictionPolicy(Storage storage){
        this.storage = storage;
    }

    @Override
    public void run() {
        while(true){
            try{
                Thread.sleep(100);
                storage.removeExpiredKeys(SAMPLE_SIZE);
            } catch (InterruptedException e){
                System.out.println("Eviction worked stopped");
                break;
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
